package com.osrscompanion.panels;


import com.osrscompanion.ClientSnapshot;
import com.osrscompanion.GameStateServer;
import com.osrscompanion.OsrsCompanionPlugin;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import net.runelite.client.ui.ColorScheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.List;
import java.util.Map;

/**
 * Stats tab — 3-column skill grid with per-skill XP progress bars,
 * session summary cards, and loot log.
 * Matches mockup's .stats-grid (3-col, 4px gap, #1e1e1e bg).
 */
public class StatsPanel extends JPanel
{
	private final OsrsCompanionPlugin plugin;

	// 3-column skill grid
	private final JPanel skillGrid;
	private final SkillCell[] skillCells;

	// Summary labels — Session card
	private final JLabel sessionXpLabel   = PanelUtils.val("—", PanelUtils.GREEN);
	private final JLabel sessionRateLabel = PanelUtils.val("—");
	private final JLabel levelsLabel      = PanelUtils.val("—");
	private final JLabel startedLabel     = PanelUtils.val("—", PanelUtils.MUTED);

	// Summary labels — Combat card
	private final JLabel combatLvlLabel  = PanelUtils.val("—");
	private final JLabel dmgDealtLabel   = PanelUtils.val("—");
	private final JLabel dmgTakenLabel   = PanelUtils.val("—");
	private final JLabel deathsLabel     = PanelUtils.val("—", PanelUtils.MUTED);

	// Summary labels — Loot card
	private final JLabel lootCountLabel = PanelUtils.val("—");
	private final JPanel lootListPanel;

	private final JLabel footerLabel = new JLabel("—");

	// Ordered skill list matching OSRS canonical order
	private static final Skill[] SKILL_ORDER = {
		Skill.ATTACK, Skill.HITPOINTS, Skill.MINING,
		Skill.STRENGTH, Skill.AGILITY, Skill.SMITHING,
		Skill.DEFENCE, Skill.HERBLORE, Skill.FISHING,
		Skill.RANGED, Skill.THIEVING, Skill.COOKING,
		Skill.PRAYER, Skill.CRAFTING, Skill.FIREMAKING,
		Skill.MAGIC, Skill.FLETCHING, Skill.WOODCUTTING,
		Skill.RUNECRAFT, Skill.SLAYER, Skill.FARMING,
		Skill.CONSTRUCTION, Skill.HUNTER
	};

	public StatsPanel(OsrsCompanionPlugin plugin)
	{
		this.plugin = plugin;
		setLayout(new BorderLayout());
		setBackground(PanelUtils.PAGE_BG);
		setBorder(new EmptyBorder(16, 20, 16, 20));

		// ── NORTH: header ───────────────────────────────────────────
		JPanel head = PanelUtils.panelHead("Stats", "23 skills · session deltas");
		head.setBorder(new EmptyBorder(0, 0, 10, 0));
		add(head, BorderLayout.NORTH);

		// ── CENTER: scrollable body ─────────────────────────────────
		JPanel body = new JPanel();
		body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
		body.setBackground(PanelUtils.PAGE_BG);

		// Skill grid (3 columns)
		skillGrid = new JPanel(new GridLayout(0, 3, 4, 4));
		skillGrid.setBackground(PanelUtils.CARD_BG);
		skillGrid.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(Color.BLACK, 1),
			new EmptyBorder(6, 6, 6, 6)
		));
		skillGrid.setAlignmentX(LEFT_ALIGNMENT);

		skillCells = new SkillCell[SKILL_ORDER.length];
		for (int i = 0; i < SKILL_ORDER.length; i++)
		{
			skillCells[i] = new SkillCell(SKILL_ORDER[i]);
			skillGrid.add(skillCells[i]);
		}

		body.add(skillGrid);
		body.add(PanelUtils.vgap(10));

		// Summary cards (grid-3: Session + Combat + Loot)
		JPanel sessionCard = buildSessionCard();
		JPanel combatCard = buildCombatCard();

		JPanel lootCard = PanelUtils.card();
		lootCard.setLayout(new BoxLayout(lootCard, BoxLayout.Y_AXIS));
		lootCard.add(PanelUtils.cardHeader("Loot"));
		lootCard.add(PanelUtils.vgap(6));
		lootCard.add(PanelUtils.kvRow("Drops", lootCountLabel));
		lootCard.add(PanelUtils.vgap(4));

		lootListPanel = new JPanel();
		lootListPanel.setLayout(new BoxLayout(lootListPanel, BoxLayout.Y_AXIS));
		lootListPanel.setOpaque(false);
		lootCard.add(lootListPanel);

		JPanel summaryRow = PanelUtils.grid3(sessionCard, combatCard, lootCard);
		summaryRow.setAlignmentX(LEFT_ALIGNMENT);
		summaryRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
		body.add(summaryRow);
		body.add(PanelUtils.vgap(6));

		// Buttons
		JButton copyBtn = PanelUtils.btn("Copy Stats");
		copyBtn.addActionListener(e -> copyStats());
		JPanel btnPanel = PanelUtils.btnRow(copyBtn);
		btnPanel.setAlignmentX(LEFT_ALIGNMENT);
		body.add(btnPanel);

		JPanel bodyWrapper = new JPanel(new BorderLayout());
		bodyWrapper.setBackground(PanelUtils.PAGE_BG);
		bodyWrapper.add(body, BorderLayout.NORTH);

		JScrollPane scroll = new JScrollPane(bodyWrapper,
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
			JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setBorder(null);
		scroll.setBackground(PanelUtils.PAGE_BG);
		scroll.getViewport().setBackground(PanelUtils.PAGE_BG);
		scroll.getVerticalScrollBar().setUnitIncrement(16);

		add(scroll, BorderLayout.CENTER);

		// ── SOUTH: footer ───────────────────────────────────────────
		footerLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		footerLabel.setFont(new Font(PanelUtils.FONT_FAMILY, Font.PLAIN, (int) PanelUtils.FONT_SMALL));
		footerLabel.setBorder(new EmptyBorder(4, 0, 0, 0));
		add(footerLabel, BorderLayout.SOUTH);
	}

	private JPanel buildSessionCard()
	{
		JPanel card = PanelUtils.card();
		card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
		card.add(PanelUtils.cardHeader("Session"));
		card.add(PanelUtils.vgap(6));
		card.add(PanelUtils.kvRow("XP gained", sessionXpLabel));
		card.add(PanelUtils.kvRow("XP/hour", sessionRateLabel));
		card.add(PanelUtils.kvRow("Levels gained", levelsLabel));
		card.add(PanelUtils.kvRow("Started", startedLabel));
		return card;
	}

	private JPanel buildCombatCard()
	{
		JPanel card = PanelUtils.card();
		card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
		card.add(PanelUtils.cardHeader("Combat"));
		card.add(PanelUtils.vgap(6));
		card.add(PanelUtils.kvRow("Combat level", combatLvlLabel));
		card.add(PanelUtils.kvRow("Damage dealt", dmgDealtLabel));
		card.add(PanelUtils.kvRow("Damage taken", dmgTakenLabel));
		card.add(PanelUtils.kvRow("Deaths", deathsLabel));
		return card;
	}

	@SuppressWarnings("unchecked")
	public void refresh(ClientSnapshot snap)
	{
		GameStateServer server = plugin.getApiServer();
		if (server == null || snap == null || !snap.loggedIn)
		{
			sessionXpLabel.setText("—");
			sessionRateLabel.setText("—");
			levelsLabel.setText("—");
			startedLabel.setText("—");
			lootCountLabel.setText("—");
			for (SkillCell cell : skillCells) cell.clear();
			lootListPanel.removeAll();
			lootListPanel.revalidate();
			lootListPanel.repaint();
			footerLabel.setText("Not logged in");
			return;
		}

		Map<String, Integer> baselines = server.getXpBaselinesCopy();
		long sessionMs = server.getSessionStartMs();
		long elapsed = sessionMs > 0 ? System.currentTimeMillis() - sessionMs : 0;

		int totalGained = 0;
		int levelsGained = 0;
		int skillsActive = 0;

		for (int i = 0; i < SKILL_ORDER.length; i++)
		{
			Skill skill = SKILL_ORDER[i];
			String name = skill.name();
			Integer baseline = baselines.get(name);

			int currentXp = snap.getSkillExperience(skill);
			int currentLevel = snap.getRealLevel(skill);
			int gained = baseline != null ? currentXp - baseline : 0;

			// Calculate % to next level
			float pctToNext = 0f;
			if (currentLevel < 99)
			{
				int xpCurrent = currentXp;
				int xpThisLevel = Experience.getXpForLevel(currentLevel);
				int xpNextLevel = Experience.getXpForLevel(currentLevel + 1);
				int range = xpNextLevel - xpThisLevel;
				if (range > 0)
				{
					pctToNext = (float) (xpCurrent - xpThisLevel) / range;
					pctToNext = Math.max(0f, Math.min(1f, pctToNext));
				}
			}
			else
			{
				pctToNext = 1f;
			}

			Color barColor = getSkillColor(skill);
			skillCells[i].update(capitalize(name), currentLevel, gained, pctToNext, barColor);

			if (gained > 0)
			{
				totalGained += gained;
				skillsActive++;
			}
		}

		// Session summary
		sessionXpLabel.setText(String.format("+%,d", totalGained));
		long xpPerHour = elapsed > 0 ? (long) totalGained * 3600000 / elapsed : 0;
		sessionRateLabel.setText(String.format("%,d/hr", xpPerHour));
		levelsLabel.setText(String.valueOf(levelsGained));

		long hours = elapsed / 3600000;
		long mins = (elapsed / 60000) % 60;
		startedLabel.setText(String.format("%dh %dm ago", hours, mins));

		// Combat summary
		combatLvlLabel.setText(String.valueOf(snap.combatLevel));
		// Damage dealt/taken/deaths are not tracked by the plugin; show placeholder
		dmgDealtLabel.setText("—");
		dmgTakenLabel.setText("—");
		deathsLabel.setText("—");

		// Loot
		List<Map<String, Object>> loot = server.getLootLogCopy();
		lootCountLabel.setText(loot.size() + " drops");
		lootListPanel.removeAll();
		int lootStart = Math.max(0, loot.size() - 5);
		for (int i = loot.size() - 1; i >= lootStart; i--)
		{
			Map<String, Object> drop = loot.get(i);
			lootListPanel.add(createLootRow(drop));
		}
		if (lootListPanel.getComponentCount() == 0)
		{
			JLabel noLoot = new JLabel("No loot yet");
			noLoot.setForeground(PanelUtils.MUTED);
			noLoot.setFont(new Font(PanelUtils.FONT_FAMILY, Font.PLAIN, (int) PanelUtils.FONT_SMALL));
			lootListPanel.add(noLoot);
		}
		lootListPanel.revalidate();
		lootListPanel.repaint();

		footerLabel.setText(skillsActive + " skills active this session");
	}

	private Color getSkillColor(Skill skill)
	{
		switch (skill)
		{
			case ATTACK:
			case STRENGTH:
			case DEFENCE:
			case HITPOINTS:
			case RANGED:
			case PRAYER:
			case MAGIC:
				return PanelUtils.SKILL_COMBAT;
			case FISHING:
			case MINING:
			case WOODCUTTING:
			case FARMING:
			case HUNTER:
			case AGILITY:
				return PanelUtils.SKILL_GATHERING;
			case COOKING:
			case SMITHING:
			case CRAFTING:
			case FLETCHING:
			case FIREMAKING:
			case CONSTRUCTION:
			case HERBLORE:
				return PanelUtils.SKILL_ARTISAN;
			case RUNECRAFT:
			case THIEVING:
			case SLAYER:
				return PanelUtils.SKILL_SUPPORT;
			default:
				return PanelUtils.MUTED;
		}
	}

	@SuppressWarnings("unchecked")
	private JPanel createLootRow(Map<String, Object> drop)
	{
		JPanel row = new JPanel(new BorderLayout(4, 0));
		row.setOpaque(false);
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
		row.setAlignmentX(LEFT_ALIGNMENT);

		String npcName = String.valueOf(drop.getOrDefault("npcName", "Unknown"));
		JLabel npcLabel = new JLabel(npcName);
		npcLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		npcLabel.setFont(new Font(PanelUtils.FONT_FAMILY, Font.PLAIN, (int) PanelUtils.FONT_SMALL));
		row.add(npcLabel, BorderLayout.WEST);

		List<Map<String, Object>> items = (List<Map<String, Object>>) drop.get("items");
		if (items != null && !items.isEmpty())
		{
			StringBuilder sb = new StringBuilder();
			for (int j = 0; j < Math.min(items.size(), 2); j++)
			{
				if (j > 0) sb.append(", ");
				Map<String, Object> item = items.get(j);
				sb.append(item.getOrDefault("name", "?"));
				int qty = item.containsKey("quantity") ? ((Number) item.get("quantity")).intValue() : 1;
				if (qty > 1) sb.append(" x").append(qty);
			}
			if (items.size() > 2) sb.append(" +").append(items.size() - 2);

			JLabel il = new JLabel(sb.toString());
			il.setForeground(PanelUtils.GOLD);
			il.setFont(new Font(PanelUtils.FONT_FAMILY, Font.PLAIN, (int) PanelUtils.FONT_SMALL));
			row.add(il, BorderLayout.EAST);
		}
		return row;
	}

	private static String capitalize(String s)
	{
		if (s == null || s.isEmpty()) return s;
		return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
	}

	@SuppressWarnings("unchecked")
	private void copyStats()
	{
		GameStateServer server = plugin.getApiServer();
		if (server == null) return;

		Map<String, Integer> baselines = server.getXpBaselinesCopy();
		long sessionMs = server.getSessionStartMs();
		long elapsed = sessionMs > 0 ? System.currentTimeMillis() - sessionMs : 0;

		StringBuilder sb = new StringBuilder();
		sb.append("=== XP Gains ===\n");

		int totalGained = 0;
		ClientSnapshot snap = plugin.getLatestSnapshot();
		for (Skill skill : SKILL_ORDER)
		{
			Integer baseline = baselines.get(skill.name());
			if (baseline == null) continue;
			int gained = (snap != null ? snap.getSkillExperience(skill) : 0) - baseline;
			if (gained > 0)
			{
				totalGained += gained;
				long xpPerHour = elapsed > 0 ? (long) gained * 3600000 / elapsed : 0;
				sb.append(capitalize(skill.name())).append(": +")
					.append(String.format("%,d", gained))
					.append(" (").append(String.format("%,d", xpPerHour)).append("/hr)\n");
			}
		}
		sb.append("Total: +").append(String.format("%,d", totalGained)).append("\n\n");

		List<Map<String, Object>> loot = server.getLootLogCopy();
		sb.append("=== Loot (").append(loot.size()).append(" drops) ===\n");
		for (Map<String, Object> drop : loot)
		{
			sb.append(drop.getOrDefault("npcName", "Unknown")).append(": ");
			List<Map<String, Object>> items = (List<Map<String, Object>>) drop.get("items");
			if (items != null)
			{
				for (int j = 0; j < items.size(); j++)
				{
					if (j > 0) sb.append(", ");
					Map<String, Object> item = items.get(j);
					sb.append(item.getOrDefault("name", "ID:" + item.get("itemId")));
					int qty = item.containsKey("quantity") ? ((Number) item.get("quantity")).intValue() : 1;
					if (qty > 1) sb.append(" x").append(qty);
				}
			}
			sb.append("\n");
		}

		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
			new StringSelection(sb.toString()), null);
	}

	// ── Skill cell — individual skill display with progress bar ─────
	/**
	 * .skill { background: #1a1a1a; border-radius: 2px; padding: 6px 10px; }
	 * Row 1: name (#ddd) | level (bold white) | xp gained (green or muted)
	 * Row 2: 4px progress bar (track #0a0a0a, fill = skill-type color)
	 */
	private static class SkillCell extends JPanel
	{
		private final JLabel nameLabel;
		private final JLabel levelLabel;
		private final JLabel deltaLabel;
		private Color barColor = PanelUtils.MUTED;
		private float barPct = 0f;

		private static final Color CELL_BG = new Color(0x1a, 0x1a, 0x1a);
		private static final Color BAR_TRACK = new Color(0x0a, 0x0a, 0x0a);
		private static final int BAR_H = 4;

		SkillCell(Skill skill)
		{
			setLayout(new BorderLayout());
			setBackground(CELL_BG);
			setBorder(new EmptyBorder(6, 10, 6 + BAR_H + 3, 10));
			setPreferredSize(new Dimension(0, 40));

			nameLabel = new JLabel(capitalize(skill.name()));
			nameLabel.setForeground(new Color(0xdd, 0xdd, 0xdd));
			nameLabel.setFont(new Font(PanelUtils.FONT_FAMILY, Font.PLAIN, (int) PanelUtils.FONT_SMALL));
			add(nameLabel, BorderLayout.WEST);

			JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
			rightPanel.setOpaque(false);

			levelLabel = new JLabel("—");
			levelLabel.setForeground(Color.WHITE);
			levelLabel.setFont(new Font(PanelUtils.FONT_FAMILY, Font.BOLD, (int) PanelUtils.FONT_SMALL));

			deltaLabel = new JLabel("");
			deltaLabel.setFont(new Font(PanelUtils.FONT_FAMILY, Font.PLAIN, (int) PanelUtils.FONT_BADGE));
			deltaLabel.setForeground(PanelUtils.MUTED);

			rightPanel.add(deltaLabel);
			rightPanel.add(levelLabel);
			add(rightPanel, BorderLayout.EAST);
		}

		void update(String name, int level, int xpGained, float pctToNext, Color skillBarColor)
		{
			nameLabel.setText(name);
			levelLabel.setText(String.valueOf(level));
			barPct = pctToNext;
			barColor = skillBarColor;

			if (xpGained > 0)
			{
				deltaLabel.setText("+" + formatNumber(xpGained));
				deltaLabel.setForeground(PanelUtils.GREEN);
			}
			else
			{
				deltaLabel.setText("+0");
				deltaLabel.setForeground(PanelUtils.MUTED);
			}
			repaint();
		}

		void clear()
		{
			levelLabel.setText("—");
			deltaLabel.setText("");
			barPct = 0f;
			repaint();
		}

		@Override
		protected void paintComponent(Graphics g)
		{
			super.paintComponent(g);
			Graphics2D g2 = (Graphics2D) g.create();

			int w = getWidth();
			int h = getHeight();
			int barY = h - BAR_H - 4;

			// Bar track
			g2.setColor(BAR_TRACK);
			g2.fillRect(6, barY, w - 12, BAR_H);

			// Bar fill
			int fillW = (int) ((w - 12) * barPct);
			if (fillW > 0)
			{
				g2.setColor(barColor);
				g2.fillRect(6, barY, fillW, BAR_H);
			}

			g2.dispose();
		}

		private static String formatNumber(int n)
		{
			if (n >= 1_000_000) return String.format("%.1fM", n / 1_000_000.0);
			if (n >= 1_000) return String.format("%.1fK", n / 1_000.0);
			return String.valueOf(n);
		}

		private static String capitalize(String s)
		{
			if (s == null || s.isEmpty()) return s;
			return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
		}
	}
}
