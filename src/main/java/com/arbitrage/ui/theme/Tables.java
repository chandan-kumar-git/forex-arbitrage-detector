package com.arbitrage.ui.theme;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.List;

/**
 * Helper methods that give every JTable the same look:
 * tall rows, a quiet header, no grid lines and a soft selection colour.
 */
public final class Tables {

    private Tables() {
    }

    public static void style(JTable table) {
        table.setRowHeight(Theme.ROW_HEIGHT);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFillsViewportHeight(true);
        table.setBackground(Theme.SURFACE);
        table.setSelectionBackground(Theme.PRIMARY_TINT);
        table.setSelectionForeground(Theme.INK);
        table.setDefaultRenderer(Object.class, new PaddedRenderer(Theme.BODY));
        table.setDefaultRenderer(Number.class, new PaddedRenderer(Theme.BODY));
        table.getTableHeader().setDefaultRenderer(new HeaderRenderer());
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setPreferredSize(new Dimension(0, Theme.ROW_HEIGHT));
    }

    /** Wraps the table in a scroll pane that has no border and a white background. */
    public static JScrollPane scroll(JTable table) {
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Theme.SURFACE);
        return scrollPane;
    }

    /** A table model the user cannot type into. Columns sort by the type of their values. */
    public static DefaultTableModel readOnlyModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int column) {
                return getRowCount() == 0 ? Object.class : getValueAt(0, column).getClass();
            }
        };
    }

    /** Green, bold, right-aligned profit such as 0.347%. */
    public static TableCellRenderer profitRenderer() {
        PaddedRenderer renderer = new PaddedRenderer(Theme.BODY_BOLD) {
            @Override
            protected void setValue(Object value) {
                setText(value instanceof Number ? Formats.profit(((Number) value).doubleValue()) : "");
            }
        };
        renderer.setHorizontalAlignment(SwingConstants.RIGHT);
        renderer.setForeground(Theme.GOOD);
        return renderer;
    }

    /** Fixed-width text for codes, rates and prices. */
    public static TableCellRenderer monoRenderer() {
        return new PaddedRenderer(Theme.MONO);
    }

    /** A normal cell in the given font, with 8 px of space on the left and right. */
    public static class PaddedRenderer extends DefaultTableCellRenderer {
        private final Font cellFont;

        public PaddedRenderer(Font cellFont) {
            this.cellFont = cellFont;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                       boolean focused, int row, int column) {
            super.getTableCellRendererComponent(table, value, selected, false, row, column);
            setFont(cellFont);                    // the line above resets the font, so set ours again
            setBorder(BorderFactory.createEmptyBorder(0, Theme.S2, 0, Theme.S2));
            return this;
        }
    }

    /** The column header: small bold grey text, a line below, and an arrow on the sorted column. */
    private static class HeaderRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                       boolean focused, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, false, false, row, column);
            label.setText(value + sortArrow(table, column));
            label.setFont(Theme.SMALL_BOLD);
            label.setForeground(Theme.INK_2);
            label.setBackground(Theme.SURFACE);
            label.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                    BorderFactory.createEmptyBorder(0, Theme.S2, 0, Theme.S2)));
            return label;
        }

        private String sortArrow(JTable table, int column) {
            if (table.getRowSorter() == null) {
                return "";
            }
            List<? extends RowSorter.SortKey> keys = table.getRowSorter().getSortKeys();
            if (keys.isEmpty() || keys.get(0).getColumn() != table.convertColumnIndexToModel(column)) {
                return "";
            }
            return keys.get(0).getSortOrder() == SortOrder.ASCENDING ? "  ▲" : "  ▼";
        }
    }
}
