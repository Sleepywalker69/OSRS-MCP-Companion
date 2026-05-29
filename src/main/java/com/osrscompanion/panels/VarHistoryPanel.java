package com.osrscompanion.panels;


import com.osrscompanion.GameStateServer;
import com.osrscompanion.OsrsCompanionPlugin;
import com.osrscompanion.VarNames;
import net.runelite.client.ui.ColorScheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Var History tab — timeline viewer for varbit/varp changes.
 * Uses JTextPane for native text selection. Semi-transparent type badges.
 */
public class VarHistoryPanel extends JPanel
{
	private final OsrsCompanionPlugin plugin;

	private final JComboBox<String> typeFilter;
	private final JTextField searchField;
	private final JTextField varpExcludeField;
	private final JTextField varbitExcludeField;
	private final JTextPane textPane;
	private final JLabel footerLabel = new JLabel("—");

	private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");

	// Style names
	private static final String S_TIME       = "time";
	private static final String S_CHANGE     = "change";
	private static final String S_TICK       = "tick";
	private static final String S_BADGE_VARBIT = "b_varbit";
	private static final String S_BADGE_VARP   = "b_varp";

	public VarHistoryPanel(OsrsCompanionPlugin plugin)
	{
		this.plugin = plugin;
		setLayout(new BorderLayout());
		setBackground(PanelUtils.PAGE_BG);
		setBorder(new EmptyBorder(16, 20, 16, 20));

		// ── NORTH: header + filter bar ──────────────────────────────
		JPanel north = new JPanel();
		north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
		north.setOpaque(false);

		JPanel head = PanelUtils.panelHead("Vars", "varbit & varp change history");
		head.setAlignmentX(LEFT_ALIGNMENT);
		north.add(head);
		north.add(PanelUtils.vgap(10));

		JPanel filterBar = new JPanel(new BorderLayout(8, 0));
		filterBar.setOpaque(false);
		filterBar.setAlignmentX(LEFT_ALIGNMENT);
		filterBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

		typeFilter = new JComboBox<>(new String[]{"All", "varbit", "varp"});
		typeFilter.setFont(new Font(PanelUtils.FONT_FAMILY, Font.PLAIN, (int) PanelUtils.FONT_SMALL));
		typeFilter.setPreferredSize(new Dimension(75, 24));
		typeFilter.addActionListener(e -> refresh());
		filterBar.add(typeFilter, BorderLayout.WEST);

		searchField = new JTextField();
		searchField.setFont(new Font(PanelUtils.FONT_FAMILY, Font.PLAIN, (int) PanelUtils.FONT_BODY));
		searchField.setToolTipText("Search by ID...");
		searchField.addActionListener(e -> refresh());
		filterBar.add(searchField, BorderLayout.CENTER);

		JPanel rightBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
		rightBtns.setOpaque(false);
		JButton copyBtn = PanelUtils.btn("Copy");
		copyBtn.addActionListener(e -> copyVarHistory());
		rightBtns.add(copyBtn);
		filterBar.add(rightBtns, BorderLayout.EAST);

		north.add(filterBar);
		north.add(PanelUtils.vgap(6));

		// ── Exclude filter row ──────────────────────────────────────
		JPanel excludeRow = new JPanel(new GridLayout(1, 2, 8, 0));
		excludeRow.setOpaque(false);
		excludeRow.setAlignmentX(LEFT_ALIGNMENT);
		excludeRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));

		JPanel varpExcPanel = new JPanel(new BorderLayout(4, 0));
		varpExcPanel.setOpaque(false);
		JLabel varpExcLabel = new JLabel("Varp ×");
		varpExcLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		varpExcLabel.setFont(new Font(PanelUtils.FONT_FAMILY, Font.PLAIN, (int) PanelUtils.FONT_SMALL));
		varpExcPanel.add(varpExcLabel, BorderLayout.WEST);
		varpExcludeField = new JTextField();
		varpExcludeField.setFont(new Font(PanelUtils.FONT_FAMILY, Font.PLAIN, (int) PanelUtils.FONT_SMALL));
		varpExcludeField.setToolTipText("Comma-separated varp IDs to hide (e.g. 3079, 83)");
		varpExcludeField.addActionListener(e -> refresh());
		varpExcPanel.add(varpExcludeField, BorderLayout.CENTER);
		excludeRow.add(varpExcPanel);

		JPanel varbitExcPanel = new JPanel(new BorderLayout(4, 0));
		varbitExcPanel.setOpaque(false);
		JLabel varbitExcLabel = new JLabel("Varbit ×");
		varbitExcLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		varbitExcLabel.setFont(new Font(PanelUtils.FONT_FAMILY, Font.PLAIN, (int) PanelUtils.FONT_SMALL));
		varbitExcPanel.add(varbitExcLabel, BorderLayout.WEST);
		varbitExcludeField = new JTextField();
		varbitExcludeField.setFont(new Font(PanelUtils.FONT_FAMILY, Font.PLAIN, (int) PanelUtils.FONT_SMALL));
		varbitExcludeField.setToolTipText("Comma-separated varbit IDs to hide (e.g. 12392, 12391)");
		varbitExcludeField.addActionListener(e -> refresh());
		varbitExcPanel.add(varbitExcludeField, BorderLayout.CENTER);
		excludeRow.add(varbitExcPanel);

		north.add(excludeRow);
		north.add(PanelUtils.vgap(10));

		add(north, BorderLayout.NORTH);

		// ── CENTER: var changes card ────────────────────────────────
		JPanel card = PanelUtils.card();
		card.setLayout(new BorderLayout());

		textPane = new JTextPane();
		textPane.setEditable(false);
		textPane.setBackground(PanelUtils.CARD_BG);
		textPane.setFont(PanelUtils.monoFont(PanelUtils.FONT_MONO));
		textPane.setForeground(Color.WHITE);
		initStyles(textPane.getStyledDocument());
		PanelUtils.installTextPopup(textPane);

		JScrollPane scroll = new JScrollPane(textPane);
		scroll.setBorder(null);
		scroll.setBackground(PanelUtils.CARD_BG);
		scroll.getVerticalScrollBar().setUnitIncrement(16);
		card.add(scroll, BorderLayout.CENTER);

		add(card, BorderLayout.CENTER);

		// ── SOUTH: footer ───────────────────────────────────────────
		footerLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		footerLabel.setFont(new Font(PanelUtils.FONT_FAMILY, Font.PLAIN, (int) PanelUtils.FONT_SMALL));
		footerLabel.setBorder(new EmptyBorder(4, 0, 0, 0));
		add(footerLabel, BorderLayout.SOUTH);
	}

	private void initStyles(StyledDocument doc)
	{
		Style def = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);
		Font mono = PanelUtils.monoFont(PanelUtils.FONT_MONO);

		Style time = doc.addStyle(S_TIME, def);
		StyleConstants.setForeground(time, new Color(0x55, 0x55, 0x55));
		StyleConstants.setFontFamily(time, mono.getFamily());
		StyleConstants.setFontSize(time, (int) PanelUtils.FONT_SMALL);

		Style change = doc.addStyle(S_CHANGE, def);
		StyleConstants.setForeground(change, Color.WHITE);
		StyleConstants.setFontFamily(change, mono.getFamily());
		StyleConstants.setFontSize(change, (int) PanelUtils.FONT_MONO);

		Style tick = doc.addStyle(S_TICK, def);
		StyleConstants.setForeground(tick, ColorScheme.LIGHT_GRAY_COLOR);
		StyleConstants.setFontFamily(tick, mono.getFamily());
		StyleConstants.setFontSize(tick, (int) PanelUtils.FONT_BADGE);

		// Badge styles
		addBadge(doc, S_BADGE_VARBIT, PanelUtils.BADGE_ORANGE);
		addBadge(doc, S_BADGE_VARP,   PanelUtils.BADGE_BLUE);
	}

	private void addBadge(StyledDocument doc, String name, PanelUtils.BadgeColor bc)
	{
		Style def = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);
		Style s = doc.addStyle(name, def);
		StyleConstants.setForeground(s, bc.fg);
		StyleConstants.setBackground(s, bc.bg);
		StyleConstants.setBold(s, true);
		StyleConstants.setFontSize(s, (int) PanelUtils.FONT_BADGE);
	}

	public void refresh()
	{
		GameStateServer server = plugin.getApiServer();
		if (server == null)
		{
			textPane.setText("");
			footerLabel.setText("API server not available");
			return;
		}

		List<Map<String, Object>> changes = server.getVarHistoryCopy();
		String typeFilterVal = (String) typeFilter.getSelectedItem();
		String searchVal = searchField.getText().trim();
		Set<Integer> excludedVarps = parseIdList(varpExcludeField.getText());
		Set<Integer> excludedVarbits = parseIdList(varbitExcludeField.getText());

		StyledDocument doc = textPane.getStyledDocument();
		textPane.setText("");

		int shown = 0;
		int excluded = 0;
		try
		{
			for (int i = changes.size() - 1; i >= 0; i--)
			{
				Map<String, Object> entry = changes.get(i);
				String type = String.valueOf(entry.getOrDefault("type", ""));
				String idStr = String.valueOf(entry.getOrDefault("id", ""));

				if (!"All".equals(typeFilterVal) && !type.equals(typeFilterVal))
					continue;

				// Exclude filter
				int id = 0;
				try { id = Integer.parseInt(idStr); } catch (NumberFormatException ignored2) {}
				if ("varp".equals(type) && !excludedVarps.isEmpty() && excludedVarps.contains(id))
				{
					excluded++;
					continue;
				}
				if ("varbit".equals(type) && !excludedVarbits.isEmpty() && excludedVarbits.contains(id))
				{
					excluded++;
					continue;
				}

				if (!searchVal.isEmpty())
				{
					String varName = VarNames.lookup(type, id);
					boolean matches = idStr.contains(searchVal)
						|| (varName != null && varName.toLowerCase().contains(searchVal.toLowerCase()));
					if (!matches) continue;
				}

				insertVarRow(doc, entry);
				shown++;
				if (shown >= 200) break;
			}
		}
		catch (BadLocationException ignored) {}

		String footerText = shown + " / " + changes.size() + " var changes";
		if (excluded > 0) footerText += " (" + excluded + " excluded)";
		footerLabel.setText(footerText);
		textPane.setCaretPosition(0);
	}

	private Set<Integer> parseIdList(String text)
	{
		Set<Integer> ids = new HashSet<>();
		if (text == null || text.trim().isEmpty()) return ids;
		for (String part : text.split("[,\\s]+"))
		{
			try { ids.add(Integer.parseInt(part.trim())); }
			catch (NumberFormatException ignored) {}
		}
		return ids;
	}

	private void insertVarRow(StyledDocument doc, Map<String, Object> entry) throws BadLocationException
	{
		String type = String.valueOf(entry.getOrDefault("type", "?"));
		int tick = entry.containsKey("tick") ? ((Number) entry.get("tick")).intValue() : 0;
		int id = entry.containsKey("id") ? ((Number) entry.get("id")).intValue() : 0;
		int oldVal = entry.containsKey("oldValue") ? ((Number) entry.get("oldValue")).intValue() : 0;
		int newVal = entry.containsKey("newValue") ? ((Number) entry.get("newValue")).intValue() : 0;
		long timestamp = entry.containsKey("timestamp") ? ((Number) entry.get("timestamp")).longValue() : 0;

		// Time
		String timeStr = timestamp > 0 ? TIME_FORMAT.format(new Date(timestamp)) : "??:??:??";
		doc.insertString(doc.getLength(), String.format("%-10s", timeStr), doc.getStyle(S_TIME));

		// Type badge
		boolean isVarbit = "varbit".equals(type);
		String badgeStyle = isVarbit ? S_BADGE_VARBIT : S_BADGE_VARP;
		String badgeText = " " + type + " ";
		doc.insertString(doc.getLength(), String.format("%-8s", badgeText), doc.getStyle(badgeStyle));
		doc.insertString(doc.getLength(), "  ", doc.getStyle(S_TIME));

		// Change text
		String varName = VarNames.lookup(type, id);
		String changeText = varName != null
			? String.format("%s %d (%s): %d → %d", type, id, varName, oldVal, newVal)
			: String.format("%s %d: %d → %d", type, id, oldVal, newVal);
		doc.insertString(doc.getLength(), changeText, doc.getStyle(S_CHANGE));

		// Tick
		doc.insertString(doc.getLength(), "  T" + tick, doc.getStyle(S_TICK));
		doc.insertString(doc.getLength(), "\n", doc.getStyle(S_TIME));
	}

	private void copyVarHistory()
	{
		String selected = textPane.getSelectedText();
		if (selected != null && !selected.isEmpty())
		{
			textPane.copy();
			return;
		}

		GameStateServer server = plugin.getApiServer();
		if (server == null) return;

		List<Map<String, Object>> changes = server.getVarHistoryCopy();
		String typeFilterVal = (String) typeFilter.getSelectedItem();
		String searchVal = searchField.getText().trim();

		StringBuilder sb = new StringBuilder();
		sb.append("=== Var History (").append(changes.size()).append(") ===\n");
		for (Map<String, Object> entry : changes)
		{
			String type = String.valueOf(entry.getOrDefault("type", ""));
			String idStr = String.valueOf(entry.getOrDefault("id", ""));
			if (!"All".equals(typeFilterVal) && !type.equals(typeFilterVal)) continue;
			if (!searchVal.isEmpty() && !idStr.contains(searchVal)) continue;
			int tick = entry.containsKey("tick") ? ((Number) entry.get("tick")).intValue() : 0;
			int id = entry.containsKey("id") ? ((Number) entry.get("id")).intValue() : 0;
			int oldVal = entry.containsKey("oldValue") ? ((Number) entry.get("oldValue")).intValue() : 0;
			int newVal = entry.containsKey("newValue") ? ((Number) entry.get("newValue")).intValue() : 0;
			sb.append("[").append(tick).append("] ").append(type).append(" ")
				.append(id).append(": ").append(oldVal).append(" -> ").append(newVal).append("\n");
		}
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
			new StringSelection(sb.toString()), null);
	}
}
