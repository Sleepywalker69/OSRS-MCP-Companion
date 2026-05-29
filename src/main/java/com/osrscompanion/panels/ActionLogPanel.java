package com.osrscompanion.panels;


import com.osrscompanion.ActionTracker;
import com.osrscompanion.OsrsCompanionPlugin;
import net.runelite.client.ui.ColorScheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.List;

/**
 * Action Log tab — scrollable, filterable view of the ActionTracker ring buffer.
 * Uses JTable for resizable columns and proper text selection.
 */
public class ActionLogPanel extends JPanel
{
	private final OsrsCompanionPlugin plugin;

	private final JComboBox<String> sourceFilter;
	private final JTextField searchField;
	private final JTable table;
	private final DefaultTableModel tableModel;
	private final JLabel footerLabel = new JLabel("—");
	private final JLabel subtitleLabel;

	private static final String[] COLUMNS = {"Tick", "Src", "Action", "Target", "Detail"};

	public ActionLogPanel(OsrsCompanionPlugin plugin)
	{
		this.plugin = plugin;
		setLayout(new BorderLayout());
		setBackground(PanelUtils.PAGE_BG);
		setBorder(new EmptyBorder(16, 20, 16, 20));

		// ── NORTH: header + filter bar ──────────────────────────────
		JPanel north = new JPanel();
		north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
		north.setOpaque(false);

		subtitleLabel = new JLabel("— / — in ring buffer");
		subtitleLabel.setForeground(PanelUtils.SUBTITLE_FG);
		subtitleLabel.setFont(new Font(PanelUtils.FONT_FAMILY, Font.PLAIN, (int) PanelUtils.FONT_SUBTITLE));

		JPanel head = PanelUtils.panelHead("Actions", "");
		head.remove(1);
		head.add(subtitleLabel, BorderLayout.EAST);
		head.setAlignmentX(LEFT_ALIGNMENT);
		north.add(head);
		north.add(PanelUtils.vgap(10));

		JPanel filterBar = new JPanel(new BorderLayout(8, 0));
		filterBar.setOpaque(false);
		filterBar.setAlignmentX(LEFT_ALIGNMENT);
		filterBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

		JPanel sourceRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
		sourceRow.setOpaque(false);

		sourceFilter = new JComboBox<>(new String[]{"All", "menu", "script", "inferred"});
		sourceFilter.setFont(new Font(PanelUtils.FONT_FAMILY, Font.PLAIN, (int) PanelUtils.FONT_SMALL));
		sourceFilter.setPreferredSize(new Dimension(85, 24));
		sourceFilter.addActionListener(e -> refresh());
		sourceRow.add(sourceFilter);

		filterBar.add(sourceRow, BorderLayout.WEST);

		searchField = new JTextField();
		searchField.setFont(new Font(PanelUtils.FONT_FAMILY, Font.PLAIN, (int) PanelUtils.FONT_BODY));
		searchField.setToolTipText("filter…");
		searchField.addActionListener(e -> refresh());
		filterBar.add(searchField, BorderLayout.CENTER);

		JPanel rightBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
		rightBtns.setOpaque(false);
		JButton copyBtn = PanelUtils.btn("Copy");
		copyBtn.addActionListener(e -> copyActions());
		rightBtns.add(copyBtn);
		filterBar.add(rightBtns, BorderLayout.EAST);

		north.add(filterBar);
		north.add(PanelUtils.vgap(10));

		add(north, BorderLayout.NORTH);

		// ── Action table ────────────────────────────────────────────
		tableModel = new DefaultTableModel(COLUMNS, 0)
		{
			@Override
			public boolean isCellEditable(int row, int column)
			{
				return false;
			}
		};

		table = new JTable(tableModel);
		table.setBackground(PanelUtils.CARD_BG);
		table.setForeground(Color.WHITE);
		table.setGridColor(new Color(0x2a, 0x2a, 0x2a));
		table.setFont(PanelUtils.monoFont(PanelUtils.FONT_MONO));
		table.setRowHeight(20);
		table.setShowHorizontalLines(true);
		table.setShowVerticalLines(false);
		table.setIntercellSpacing(new Dimension(4, 1));
		table.setFillsViewportHeight(true);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
		table.getTableHeader().setReorderingAllowed(false);
		table.getTableHeader().setBackground(ColorScheme.DARKER_GRAY_COLOR);
		table.getTableHeader().setForeground(ColorScheme.BRAND_ORANGE);
		table.getTableHeader().setFont(new Font(PanelUtils.FONT_FAMILY, Font.BOLD, (int) PanelUtils.FONT_SMALL));

		// Column widths
		setColumnWidth(table.getColumnModel().getColumn(0), 55, 55);   // Tick
		setColumnWidth(table.getColumnModel().getColumn(1), 65, 65);   // Src
		setColumnWidth(table.getColumnModel().getColumn(2), 140, 80);  // Action
		setColumnWidth(table.getColumnModel().getColumn(3), 140, 80);  // Target
		// Detail gets remaining space

		// Source badge renderer
		table.getColumnModel().getColumn(1).setCellRenderer(new SourceBadgeRenderer());

		// Default renderer for other columns
		DefaultTableCellRenderer defaultRenderer = new DefaultTableCellRenderer();
		defaultRenderer.setBackground(PanelUtils.CARD_BG);
		defaultRenderer.setForeground(Color.WHITE);
		table.getColumnModel().getColumn(0).setCellRenderer(new TickRenderer());
		table.getColumnModel().getColumn(2).setCellRenderer(defaultRenderer);
		table.getColumnModel().getColumn(3).setCellRenderer(defaultRenderer);
		table.getColumnModel().getColumn(4).setCellRenderer(new DetailRenderer());

		JScrollPane scroll = new JScrollPane(table);
		scroll.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
		scroll.setBackground(PanelUtils.CARD_BG);
		scroll.getViewport().setBackground(PanelUtils.CARD_BG);
		scroll.getVerticalScrollBar().setUnitIncrement(16);

		add(scroll, BorderLayout.CENTER);

		// ── SOUTH: footer ───────────────────────────────────────────
		footerLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		footerLabel.setFont(new Font(PanelUtils.FONT_FAMILY, Font.PLAIN, (int) PanelUtils.FONT_SMALL));
		footerLabel.setBorder(new EmptyBorder(4, 0, 0, 0));
		add(footerLabel, BorderLayout.SOUTH);
	}

	private void setColumnWidth(TableColumn col, int preferred, int min)
	{
		col.setPreferredWidth(preferred);
		col.setMinWidth(min);
	}

	public void refresh()
	{
		ActionTracker tracker = plugin.getActionTracker();
		if (tracker == null)
		{
			tableModel.setRowCount(0);
			footerLabel.setText("Action tracker not available");
			subtitleLabel.setText("— / — in ring buffer");
			return;
		}

		String source = (String) sourceFilter.getSelectedItem();
		String sourceArg = "All".equals(source) ? null : source;
		String search = searchField.getText().trim();
		String searchArg = search.isEmpty() ? null : search;

		List<ActionTracker.TrackedAction> actions = tracker.getActions(100, sourceArg, searchArg);
		subtitleLabel.setText(tracker.filled() + " / " + tracker.capacity() + " in ring buffer");

		tableModel.setRowCount(0);
		for (int i = actions.size() - 1; i >= 0; i--)
		{
			ActionTracker.TrackedAction a = actions.get(i);
			String detail = "";
			if (a.details != null && !a.details.isEmpty())
			{
				detail = a.details.toString();
			}
			tableModel.addRow(new Object[]{
				a.tick,
				a.source,
				a.action != null ? a.action : "",
				a.target != null ? a.target : "—",
				detail
			});
		}

		footerLabel.setText(String.format("%d shown | %d / %d in buffer",
			actions.size(), tracker.filled(), tracker.capacity()));
	}

	private void copyActions()
	{
		ActionTracker tracker = plugin.getActionTracker();
		if (tracker == null) return;

		String source = (String) sourceFilter.getSelectedItem();
		String sourceArg = "All".equals(source) ? null : source;
		String search = searchField.getText().trim();
		String searchArg = search.isEmpty() ? null : search;

		List<ActionTracker.TrackedAction> actions = tracker.getActions(100, sourceArg, searchArg);
		StringBuilder sb = new StringBuilder();
		sb.append("=== Action Log (").append(actions.size()).append(") ===\n");
		for (int i = actions.size() - 1; i >= 0; i--)
		{
			ActionTracker.TrackedAction a = actions.get(i);
			sb.append("[+").append(a.tick).append("] [").append(a.source).append("] ")
				.append(a.action).append(" → ").append(a.target);
			if (a.details != null && !a.details.isEmpty())
			{
				sb.append(" | ").append(a.details);
			}
			sb.append("\n");
		}
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
			new StringSelection(sb.toString()), null);
	}

	// ── Cell renderers ──────────────────────────────────────────────

	private static class TickRenderer extends DefaultTableCellRenderer
	{
		@Override
		public Component getTableCellRendererComponent(JTable t, Object value,
			boolean isSelected, boolean hasFocus, int row, int column)
		{
			super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
			setForeground(PanelUtils.MUTED);
			setBackground(isSelected ? t.getSelectionBackground() : PanelUtils.CARD_BG);
			return this;
		}
	}

	private static class SourceBadgeRenderer extends DefaultTableCellRenderer
	{
		@Override
		public Component getTableCellRendererComponent(JTable t, Object value,
			boolean isSelected, boolean hasFocus, int row, int column)
		{
			super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
			String src = value != null ? value.toString() : "";
			setHorizontalAlignment(CENTER);
			if (isSelected)
			{
				setBackground(t.getSelectionBackground());
				setForeground(t.getSelectionForeground());
			}
			else
			{
				switch (src)
				{
					case "menu":
						setForeground(PanelUtils.BADGE_GREEN.fg);
						setBackground(PanelUtils.BADGE_GREEN.bg);
						break;
					case "script":
						setForeground(PanelUtils.BADGE_BLUE.fg);
						setBackground(PanelUtils.BADGE_BLUE.bg);
						break;
					case "inferred":
						setForeground(PanelUtils.BADGE_YELLOW.fg);
						setBackground(PanelUtils.BADGE_YELLOW.bg);
						break;
					default:
						setForeground(PanelUtils.MUTED);
						setBackground(PanelUtils.CARD_BG);
				}
			}
			setFont(getFont().deriveFont(Font.BOLD));
			return this;
		}
	}

	private static class DetailRenderer extends DefaultTableCellRenderer
	{
		@Override
		public Component getTableCellRendererComponent(JTable t, Object value,
			boolean isSelected, boolean hasFocus, int row, int column)
		{
			super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
			setForeground(PanelUtils.MUTED);
			setBackground(isSelected ? t.getSelectionBackground() : PanelUtils.CARD_BG);
			return this;
		}
	}
}
