package com.arbitrage.ui.screens;

import com.arbitrage.model.Opportunity;
import com.arbitrage.ui.theme.Formats;
import com.arbitrage.ui.theme.Tables;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.util.List;

/**
 * The table of saved opportunities on the History screen.
 * Clicking a column header sorts the rows (TableRowSorter).
 */
public class HistoryTable extends JTable {

    private static final int[] COLUMN_WIDTHS = {50, 150, 230, 40, 80, 60, 70};

    private final DefaultTableModel rows =
            Tables.readOnlyModel("#", "Detected at", "Path", "Len", "Profit %", "Fee %", "Source");
    private List<Opportunity> shown = List.of();  // same order as the model rows

    public HistoryTable() {
        setModel(rows);
        Tables.style(this);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setRowSorter(new TableRowSorter<>(rows));
        for (int i = 0; i < COLUMN_WIDTHS.length; i++) {
            getColumnModel().getColumn(i).setPreferredWidth(COLUMN_WIDTHS[i]);
        }
        getColumnModel().getColumn(1).setCellRenderer(Tables.monoRenderer());
        getColumnModel().getColumn(4).setCellRenderer(Tables.profitRenderer());
        setToolTipText("Double-click a row for details");
    }

    /** Replaces all rows. Numbers stay numbers in the model, so sorting works correctly. */
    void setRows(List<Opportunity> opportunities) {
        shown = opportunities;
        rows.setRowCount(0);
        for (Opportunity o : opportunities) {
            rows.addRow(new Object[] {o.id(), Formats.dateTime(o.detectedAt()), o.pathText(),
                o.path().size() - 1, o.profitPercent(), String.format("%.3f", o.fee() * 100), o.source().name()});
        }
    }

    boolean hasSelection() {
        return getSelectedRow() >= 0;
    }

    /** The opportunity of the selected row. The row index changes when the table is sorted. */
    Opportunity selected() {
        return shown.get(convertRowIndexToModel(getSelectedRow()));
    }
}
