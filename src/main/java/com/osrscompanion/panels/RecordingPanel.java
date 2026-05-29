package com.osrscompanion.panels;


import com.osrscompanion.GameStateServer;
import com.osrscompanion.OsrsCompanionPlugin;
import net.runelite.client.ui.ColorScheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.*;
import java.util.List;

/**
 * Recording tab — start/stop recording, choose duration and event types, view status.
 * Layout matches mockup: grid-2 top (Controls + Preset), full-width 4-col event grid below.
 */
public class RecordingPanel extends JPanel
{
	private final OsrsCompanionPlugin plugin;

	// Controls card KV
	private final RecordingDot recordingDot = new RecordingDot();
	private final JLabel statusVal  = PanelUtils.val("Idle", PanelUtils.MUTED);
	private final JLabel durationVal = PanelUtils.val("180 sec");
	private final JLabel elapsedVal = PanelUtils.val("—");
	private final JLabel eventsVal  = PanelUtils.val("0");
	private final JLabel fileSizeVal = PanelUtils.val("—");

	// Progress bar
	private final PanelUtils.StatusBar progressBar = new PanelUtils.StatusBar(PanelUtils.REC_RED);

	// Buttons
	private final JButton toggleBtn;
	private final JButton pauseBtn   = PanelUtils.btn("Pause");
	private final JButton revealBtn  = PanelUtils.btn("Reveal file");

	// Preset card
	private JSpinner durationSpinner;

	// Event types
	private static final String[] ALL_EVENT_TYPES = {
		"game_tick", "hitsplat", "animation_changed", "npc_spawned", "npc_despawned",
		"actor_death", "var_changed", "menu_clicked", "stat_changed", "item_changed",
		"interacting_changed", "object_spawned", "object_despawned", "projectile_spawned",
		"gfx_created", "chat_message", "sound_effect", "loot_received", "game_state_changed"
	};

	private static final Map<String, String[]> PRESETS = new LinkedHashMap<>();
	static
	{
		PRESETS.put("All", null);
		PRESETS.put("Boss", new String[]{
			"game_tick", "hitsplat", "animation_changed", "npc_spawned", "npc_despawned",
			"actor_death", "menu_clicked", "object_spawned", "object_despawned",
			"projectile_spawned", "gfx_created", "sound_effect", "chat_message",
			"loot_received", "game_state_changed"
		});
		PRESETS.put("Combat", new String[]{
			"game_tick", "hitsplat", "npc_spawned", "npc_despawned", "actor_death",
			"menu_clicked", "object_spawned", "object_despawned", "projectile_spawned",
			"gfx_created", "sound_effect"
		});
		PRESETS.put("Lite", new String[]{
			"game_tick", "hitsplat", "actor_death", "menu_clicked", "projectile_spawned"
		});
		PRESETS.put("Vars", new String[]{"var_changed", "game_tick"});
		PRESETS.put("Clicks", new String[]{"menu_clicked", "game_tick"});
	}

	private final Map<String, JCheckBox> eventCheckboxes = new LinkedHashMap<>();
	private String activePreset = "All";
	private final Map<String, JButton> presetButtons = new LinkedHashMap<>();

	// Recorded Events viewer
	private final JTextPane eventsPane;
	private final JLabel eventsFooterLabel = new JLabel("—");

	public RecordingPanel(OsrsCompanionPlugin plugin)
	{
		this.plugin = plugin;
		setLayout(new BorderLayout());
		setBackground(PanelUtils.PAGE_BG);
		setBorder(new EmptyBorder(16, 20, 16, 20));

		// ── NORTH: header, controls, presets, event types ───────────
		JPanel north = new JPanel();
		north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
		north.setOpaque(false);

		JPanel head = PanelUtils.panelHead("Record", "capture a slice of game events to disk");
		head.setAlignmentX(LEFT_ALIGNMENT);
		north.add(head);
		north.add(PanelUtils.vgap(14));

		// Top row: grid-2 (Controls + Preset)
		JPanel controlsCard = buildControlsCard();
		JPanel presetCard   = buildPresetCard();
		JPanel topRow = PanelUtils.grid2(controlsCard, presetCard);
		topRow.setAlignmentX(LEFT_ALIGNMENT);
		topRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));
		north.add(topRow);
		north.add(PanelUtils.vgap(14));

		// Event Types card (full width, 4-col grid)
		JPanel eventCard = buildEventTypesCard();
		eventCard.setAlignmentX(LEFT_ALIGNMENT);
		north.add(eventCard);
		north.add(PanelUtils.vgap(14));

		// Viewer header + controls (stays fixed above the scroll)
		JPanel viewerHeader = new JPanel(new BorderLayout(4, 0));
		viewerHeader.setOpaque(false);
		viewerHeader.setAlignmentX(LEFT_ALIGNMENT);
		viewerHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

		JButton loadBtn = PanelUtils.btn("Load Events");
		loadBtn.addActionListener(e -> loadRecordedEvents());
		viewerHeader.add(loadBtn, BorderLayout.WEST);

		JLabel viewerTitle = PanelUtils.cardHeader("Recorded Events");
		viewerTitle.setHorizontalAlignment(SwingConstants.CENTER);
		viewerHeader.add(viewerTitle, BorderLayout.CENTER);

		JButton copyEventsBtn = PanelUtils.btn("Copy All");
		copyEventsBtn.addActionListener(e -> copyRecordedEvents());
		viewerHeader.add(copyEventsBtn, BorderLayout.EAST);

		north.add(viewerHeader);
		north.add(PanelUtils.vgap(4));

		add(north, BorderLayout.NORTH);

		// ── CENTER: events pane with scroll ─────────────────────────
		eventsPane = new JTextPane();
		eventsPane.setEditable(false);
		eventsPane.setBackground(PanelUtils.FEED_BG);
		eventsPane.setFont(PanelUtils.monoFont(PanelUtils.FONT_MONO));
		eventsPane.setForeground(Color.WHITE);
		PanelUtils.installTextPopup(eventsPane);

		JScrollPane eventsScroll = new JScrollPane(eventsPane);
		eventsScroll.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
		eventsScroll.getVerticalScrollBar().setUnitIncrement(16);

		add(eventsScroll, BorderLayout.CENTER);

		// ── SOUTH: footer ───────────────────────────────────────────
		eventsFooterLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		eventsFooterLabel.setFont(new Font(PanelUtils.FONT_FAMILY, Font.PLAIN, (int) PanelUtils.FONT_SMALL));
		eventsFooterLabel.setBorder(new EmptyBorder(4, 0, 0, 0));
		add(eventsFooterLabel, BorderLayout.SOUTH);

		// initialise toggle button reference
		toggleBtn = null; // assigned in buildControlsCard
	}

	// ── Controls Card ───────────────────────────────────────────────
	private JButton internalToggleBtn;

	private JPanel buildControlsCard()
	{
		JPanel card = PanelUtils.card();
		card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

		card.add(PanelUtils.cardHeader("Controls"));
		card.add(PanelUtils.vgap(10));

		// Status row with recording dot
		JPanel statusRow = new JPanel(new BorderLayout(12, 0));
		statusRow.setOpaque(false);
		statusRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
		statusRow.setAlignmentX(LEFT_ALIGNMENT);
		JLabel statusKey = new JLabel("Status");
		statusKey.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		statusKey.setFont(new Font(PanelUtils.FONT_FAMILY, Font.PLAIN, (int) PanelUtils.FONT_BODY));
		statusKey.setPreferredSize(new Dimension(110, 18));
		statusRow.add(statusKey, BorderLayout.WEST);
		JPanel statusRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
		statusRight.setOpaque(false);
		statusRight.add(recordingDot);
		statusRight.add(statusVal);
		statusRow.add(statusRight, BorderLayout.CENTER);
		card.add(statusRow);
		card.add(PanelUtils.kvRow("Duration",  durationVal));
		card.add(PanelUtils.kvRow("Elapsed",   elapsedVal));
		card.add(PanelUtils.kvRow("Events",    eventsVal));
		card.add(PanelUtils.kvRow("File size", fileSizeVal));
		card.add(PanelUtils.vgap(10));
		card.add(progressBar);
		card.add(PanelUtils.vgap(12));

		internalToggleBtn = PanelUtils.btnDanger("■ Stop");
		internalToggleBtn.setText("Start Recording");
		internalToggleBtn.setBackground(PanelUtils.GREEN);
		internalToggleBtn.addActionListener(e -> toggleRecording());
		revealBtn.addActionListener(e -> revealRecordingFile());

		JPanel btns = PanelUtils.btnRow(internalToggleBtn, pauseBtn, revealBtn);
		btns.setAlignmentX(LEFT_ALIGNMENT);
		card.add(btns);

		return card;
	}

	// ── Preset Card ─────────────────────────────────────────────────
	private JPanel buildPresetCard()
	{
		JPanel card = PanelUtils.card();
		card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

		card.add(PanelUtils.cardHeader("Preset"));
		card.add(PanelUtils.vgap(10));

		// Preset buttons row
		JPanel presetRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
		presetRow.setOpaque(false);
		presetRow.setAlignmentX(LEFT_ALIGNMENT);
		for (String preset : PRESETS.keySet())
		{
			JButton btn = PanelUtils.btn(preset);
			btn.addActionListener(e -> applyPreset(preset));
			presetButtons.put(preset, btn);
			presetRow.add(btn);
		}
		applyPreset("All");
		card.add(presetRow);
		card.add(PanelUtils.vgap(10));

		// Duration spinner
		JPanel durRow = new JPanel(new BorderLayout(12, 0));
		durRow.setOpaque(false);
		durRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
		durRow.setAlignmentX(LEFT_ALIGNMENT);
		JLabel durLabel = new JLabel("Duration (sec):");
		durLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		durLabel.setFont(new Font(PanelUtils.FONT_FAMILY, Font.PLAIN, (int) PanelUtils.FONT_BODY));
		durationSpinner = new JSpinner(new SpinnerNumberModel(180, 30, 600, 30));
		durationSpinner.setPreferredSize(new Dimension(70, 22));
		durRow.add(durLabel, BorderLayout.WEST);
		durRow.add(durationSpinner, BorderLayout.EAST);
		card.add(durRow);
		card.add(PanelUtils.vgap(6));

		// KV info
		JLabel autoSave = PanelUtils.val("Every 30s");
		card.add(PanelUtils.kvRow("Auto-save", autoSave));
		JLabel compress = PanelUtils.val("gzip", PanelUtils.GREEN);
		card.add(PanelUtils.kvRow("Compress", compress));

		return card;
	}

	// ── Event Types Card (4-col grid) ───────────────────────────────
	private JPanel buildEventTypesCard()
	{
		JPanel card = PanelUtils.card();
		card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

		card.add(PanelUtils.cardHeader("Event types"));
		card.add(PanelUtils.vgap(10));

		JPanel grid = new JPanel(new GridLayout(0, 4, 14, 6));
		grid.setOpaque(false);
		grid.setAlignmentX(LEFT_ALIGNMENT);

		for (String type : ALL_EVENT_TYPES)
		{
			JCheckBox cb = new JCheckBox(type, true);
			cb.setFont(new Font(PanelUtils.FONT_FAMILY, Font.PLAIN, (int) PanelUtils.FONT_BODY));
			cb.setForeground(new Color(0xc8, 0xc8, 0xc8));
			cb.setBackground(PanelUtils.CARD_BG);
			cb.setFocusPainted(false);
			eventCheckboxes.put(type, cb);
			grid.add(cb);
		}

		card.add(grid);
		return card;
	}

	// ── Refresh ─────────────────────────────────────────────────────
	public void refresh()
	{
		GameStateServer server = plugin.getApiServer();
		if (server == null)
		{
			statusVal.setText("API server not running");
			statusVal.setForeground(PanelUtils.RED);
			eventsVal.setText("—");
			elapsedVal.setText("—");
			progressBar.update(0, "—");
			return;
		}

		boolean recording = server.isRecording();
		recordingDot.setActive(recording);
		if (recording)
		{
			statusVal.setText("Recording");
			statusVal.setForeground(PanelUtils.REC_RED);
			internalToggleBtn.setText("■ Stop");
			internalToggleBtn.setBackground(new Color(0x6e, 0x24, 0x24));

			int eventCount = server.getRecordingEventCount();
			eventsVal.setText(String.format("%,d", eventCount));

			int[] tickInfo = server.getRecordingTickInfo();
			if (tickInfo != null)
			{
				int elapsed = tickInfo[0];
				int max = tickInfo[1];
				int pct = max > 0 ? (elapsed * 100 / max) : 0;
				int secsLeft = (int) ((max - elapsed) * 0.6);
				elapsedVal.setText(String.format("%02d:%02d / %02d:%02d",
					(int) (elapsed * 0.6) / 60, (int) (elapsed * 0.6) % 60,
					(int) (max * 0.6) / 60, (int) (max * 0.6) % 60));
				progressBar.update(pct, pct + "%");
			}
		}
		else
		{
			statusVal.setText("Idle");
			statusVal.setForeground(PanelUtils.MUTED);
			internalToggleBtn.setText("Start Recording");
			internalToggleBtn.setBackground(PanelUtils.GREEN);

			int eventCount = server.getRecordingEventCount();
			eventsVal.setText(eventCount > 0 ? String.format("%,d recorded", eventCount) : "0");
			elapsedVal.setText("—");
			progressBar.update(0, "—");
		}

		durationVal.setText((int) durationSpinner.getValue() + " sec");

		java.io.File lastFile = server.getLastRecordingFile();
		if (lastFile != null && lastFile.exists())
		{
			long bytes = lastFile.length();
			fileSizeVal.setText(bytes < 1024 ? bytes + " B" : (bytes / 1024) + " KB");
		}
		else
		{
			fileSizeVal.setText("—");
		}
	}

	// ── Recording control ───────────────────────────────────────────
	private void toggleRecording()
	{
		GameStateServer server = plugin.getApiServer();
		if (server == null)
		{
			JOptionPane.showMessageDialog(this, "API server not running", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		if (server.isRecording())
		{
			server.stopRecordingFromPanel();
		}
		else
		{
			int duration = (int) durationSpinner.getValue();
			Set<String> filter = getSelectedEventTypes();
			server.startRecordingFromPanel(duration, filter);
		}
		refresh();
	}

	private void applyPreset(String preset)
	{
		activePreset = preset;
		String[] types = PRESETS.get(preset);

		if (types == null)
		{
			for (JCheckBox cb : eventCheckboxes.values()) cb.setSelected(true);
		}
		else
		{
			Set<String> typeSet = new HashSet<>(Arrays.asList(types));
			for (Map.Entry<String, JCheckBox> entry : eventCheckboxes.entrySet())
			{
				entry.getValue().setSelected(typeSet.contains(entry.getKey()));
			}
		}

		// Update button styling
		for (Map.Entry<String, JButton> entry : presetButtons.entrySet())
		{
			if (entry.getKey().equals(preset))
			{
				PanelUtils.styleAsActive(entry.getValue());
			}
			else
			{
				PanelUtils.styleAsInactive(entry.getValue());
			}
		}
	}

	private Set<String> getSelectedEventTypes()
	{
		Set<String> selected = new LinkedHashSet<>();
		boolean allSelected = true;
		for (Map.Entry<String, JCheckBox> entry : eventCheckboxes.entrySet())
		{
			if (entry.getValue().isSelected())
			{
				selected.add(entry.getKey());
			}
			else
			{
				allSelected = false;
			}
		}
		return allSelected ? null : selected;
	}

	// ── Events viewer ───────────────────────────────────────────────
	@SuppressWarnings("unchecked")
	private void loadRecordedEvents()
	{
		GameStateServer server = plugin.getApiServer();
		if (server == null)
		{
			eventsFooterLabel.setText("API server not running");
			return;
		}

		List<Map<String, Object>> events = server.getRecordingBufferCopy();
		StringBuilder sb = new StringBuilder();
		int max = Math.min(events.size(), 500);
		for (int i = 0; i < max; i++)
		{
			Map<String, Object> evt = events.get(i);
			String type = String.valueOf(evt.getOrDefault("eventType", "?"));
			int tick = evt.containsKey("tick") ? ((Number) evt.get("tick")).intValue() : 0;
			sb.append("[").append(tick).append("] ").append(type).append(": ");
			sb.append(buildEventSummary(type, evt)).append("\n");
		}

		eventsPane.setText(sb.toString());
		eventsPane.setCaretPosition(0);
		eventsFooterLabel.setText(max + " / " + events.size() + " events loaded");
	}

	private void copyRecordedEvents()
	{
		GameStateServer server = plugin.getApiServer();
		if (server == null) return;

		List<Map<String, Object>> events = server.getRecordingBufferCopy();
		StringBuilder sb = new StringBuilder();
		sb.append("=== Recorded Events (").append(events.size()).append(") ===\n");
		for (Map<String, Object> evt : events)
		{
			String type = String.valueOf(evt.getOrDefault("eventType", "?"));
			int tick = evt.containsKey("tick") ? ((Number) evt.get("tick")).intValue() : 0;
			sb.append("[").append(tick).append("] ").append(type).append(": ");
			sb.append(buildEventSummary(type, evt)).append("\n");
		}
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
			new StringSelection(sb.toString()), null);
	}

	private void revealRecordingFile()
	{
		GameStateServer server = plugin.getApiServer();
		java.io.File file = server != null ? server.getLastRecordingFile() : null;
		java.io.File target;
		if (file != null && file.exists())
		{
			target = file.getParentFile();
		}
		else
		{
			target = new java.io.File(System.getProperty("user.home"), ".runelite/osrs-companion/recordings");
			if (!target.exists()) target.mkdirs();
		}
		try
		{
			java.awt.Desktop.getDesktop().open(target);
		}
		catch (Exception ex)
		{
			JOptionPane.showMessageDialog(this, "Could not open folder: " + ex.getMessage(),
				"Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	@SuppressWarnings("unchecked")
	private String buildEventSummary(String type, Map<String, Object> evt)
	{
		switch (type)
		{
			case "hitsplat":
			{
				Object target = evt.get("target");
				String name = "?";
				if (target instanceof Map) { Object n = ((Map<?, ?>) target).get("name"); if (n != null) name = String.valueOf(n); }
				int amount = evt.containsKey("amount") ? ((Number) evt.get("amount")).intValue() : 0;
				return "Hit " + amount + " on " + name;
			}
			case "animation_changed":
			{
				Object actor = evt.get("actor");
				String name = "?";
				if (actor instanceof Map) { Object n = ((Map<?, ?>) actor).get("name"); if (n != null) name = String.valueOf(n); }
				int anim = evt.containsKey("animation") ? ((Number) evt.get("animation")).intValue() : -1;
				return name + " anim=" + anim;
			}
			case "menu_clicked":
			{
				Object option = evt.get("option");
				Object target = evt.get("target");
				return (option != null ? option : "") + " " + (target != null ? target : "");
			}
			case "npc_spawned": case "npc_despawned":
			{
				Object npc = evt.get("npc");
				if (npc instanceof Map) { Object n = ((Map<?, ?>) npc).get("name"); return n != null ? String.valueOf(n) : "?"; }
				return "?";
			}
			case "chat_message":
				return String.valueOf(evt.getOrDefault("message", ""));
			case "game_state_changed":
				return "state=" + evt.getOrDefault("state", "?");
			case "stat_changed":
			{
				Object skill = evt.get("skill");
				Object xp = evt.get("xp");
				Object level = evt.get("level");
				return (skill != null ? skill : "?") + " xp=" + (xp != null ? xp : "?") + " lvl=" + (level != null ? level : "?");
			}
			case "var_changed":
			{
				Object varpIndex = evt.get("varpIndex");
				Object oldVal = evt.get("oldValue");
				Object newVal = evt.get("newValue");
				return "varp " + (varpIndex != null ? varpIndex : "?") + ": " + (oldVal != null ? oldVal : "?") + " → " + (newVal != null ? newVal : "?");
			}
			case "game_tick":
				return "tick " + evt.getOrDefault("tick", "?");
			case "item_changed":
			{
				Object container = evt.get("container");
				Object containerId = evt.get("containerId");
				return (container != null ? container : "container") + " (" + (containerId != null ? containerId : "?") + ")";
			}
			case "gfx_created":
			{
				Object gfxId = evt.get("graphicsId");
				Object pos = evt.get("position");
				String posStr = "";
				if (pos instanceof Map) {
					Map<?, ?> p = (Map<?, ?>) pos;
					Object wx = p.get("worldX");
					Object wy = p.get("worldY");
					posStr = " at (" + (wx != null ? wx : "?") + ", " + (wy != null ? wy : "?") + ")";
				}
				return "gfx " + (gfxId != null ? gfxId : "?") + posStr;
			}
			case "projectile_spawned":
			{
				Object projId = evt.get("projectileId");
				Object targetActor = evt.get("targetActor");
				String targetStr = "";
				if (targetActor instanceof Map) {
					Object n = ((Map<?, ?>) targetActor).get("name");
					if (n != null) targetStr = " → " + n;
				}
				return "projectile " + (projId != null ? projId : "?") + targetStr;
			}
			default:
			{
				StringBuilder info = new StringBuilder();
				for (Map.Entry<String, Object> e : evt.entrySet())
				{
					String k = e.getKey();
					if ("eventType".equals(k) || "tick".equals(k) || "timestamp".equals(k)) continue;
					if (info.length() > 0) info.append(", ");
					Object v = e.getValue();
					String vs = v != null ? String.valueOf(v) : "null";
					if (vs.length() > 40) vs = vs.substring(0, 37) + "...";
					info.append(k).append("=").append(vs);
					if (info.length() > 120) { info.append("..."); break; }
				}
				return info.length() > 0 ? info.toString() : type;
			}
		}
	}

	// ── Recording dot indicator ─────────────────────────────────────
	/**
	 * Small 8×8 red circle indicator, visible only when recording.
	 * Mimics mockup's recording dot with glow effect.
	 */
	private static class RecordingDot extends JPanel
	{
		private boolean active = false;

		RecordingDot()
		{
			setOpaque(false);
			setPreferredSize(new Dimension(12, 12));
			setMaximumSize(new Dimension(12, 12));
		}

		void setActive(boolean active)
		{
			this.active = active;
			repaint();
		}

		@Override
		protected void paintComponent(Graphics g)
		{
			super.paintComponent(g);
			if (!active) return;

			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			int w = getWidth(), h = getHeight();
			int cx = w / 2, cy = h / 2;
			int r = 4;

			// Glow
			g2.setColor(new Color(0xdc, 0x32, 0x32, 60));
			g2.fillOval(cx - r - 2, cy - r - 2, (r + 2) * 2, (r + 2) * 2);

			// Dot
			g2.setColor(PanelUtils.REC_RED);
			g2.fillOval(cx - r, cy - r, r * 2, r * 2);

			g2.dispose();
		}
	}
}
