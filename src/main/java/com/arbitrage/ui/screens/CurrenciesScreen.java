package com.arbitrage.ui.screens;

import com.arbitrage.model.Currency;
import com.arbitrage.ui.TopBar;
import com.arbitrage.ui.UiServices;
import com.arbitrage.ui.theme.BackgroundTask;
import com.arbitrage.ui.theme.Card;
import com.arbitrage.ui.theme.Formats;
import com.arbitrage.ui.theme.Tables;
import com.arbitrage.ui.theme.Theme;
import com.arbitrage.validation.ValidationException;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.List;

/**
 * Add, edit, deactivate and delete currencies.
 * The form is on the left; the table of all currencies is on the right.
 */
public class CurrenciesScreen extends BaseScreen {

    private static final int FORM_WIDTH = 340;

    private final CurrencyForm form = new CurrencyForm();
    private final DefaultTableModel model = Tables.readOnlyModel("Code", "Name", "USD price", "Active");
    private final JTable table = new JTable(model);
    private final CardLayout bodyLayout = new CardLayout();
    private final JPanel body = new JPanel(bodyLayout);
    private final OfflineCard offlineCard;
    private List<Currency> shown = List.of();     // the currencies in the table, same order as the rows

    public CurrenciesScreen(UiServices services, TopBar topBar) {
        super("Currencies", "Add, edit or deactivate the currencies used by the simulator", services, topBar);
        Card formCard = new Card("Currency");
        JPanel formHolder = new JPanel(new BorderLayout());
        formHolder.setOpaque(false);
        formHolder.add(form, BorderLayout.NORTH);  // NORTH keeps the form at its natural height
        formCard.setContent(formHolder);
        formCard.setPreferredSize(new Dimension(FORM_WIDTH, 0));

        Card tableCard = new Card("All currencies");
        tableCard.setContent(Tables.scroll(buildTable()));

        JPanel data = new JPanel(new BorderLayout(Theme.S4, 0));
        data.setOpaque(false);
        data.add(formCard, BorderLayout.WEST);
        data.add(tableCard, BorderLayout.CENTER);
        offlineCard = new OfflineCard(services, this::onShow);
        body.setOpaque(false);
        body.add(data, "data");
        body.add(offlineCard, "offline");
        setBody(body);

        form.addButton.addActionListener(e -> addCurrency());
        form.updateButton.addActionListener(e -> updateCurrency());
        form.deleteButton.addActionListener(e -> deleteCurrency());
        form.clearButton.addActionListener(e -> clearSelection());
    }

    /** Currencies live in MySQL, so the form is replaced by "Database offline" when it is down. */
    @Override
    public void onShow() {
        if (services.database().isOnline()) {
            bodyLayout.show(body, "data");
            reload();
        } else {
            offlineCard.showLastError();
            bodyLayout.show(body, "offline");
        }
    }

    private JTable buildTable() {
        Tables.style(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setCellRenderer(rowRenderer(Theme.MONO));
        table.getColumnModel().getColumn(1).setCellRenderer(rowRenderer(Theme.BODY));
        table.getColumnModel().getColumn(2).setCellRenderer(rowRenderer(Theme.MONO));
        table.getColumnModel().getColumn(0).setPreferredWidth(60);
        table.getColumnModel().getColumn(1).setPreferredWidth(170);
        table.getColumnModel().getColumn(2).setPreferredWidth(130);
        table.getColumnModel().getColumn(3).setPreferredWidth(60);
        // Clicking a row fills the form with that currency.
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() >= 0) {
                form.fill(shown.get(table.getSelectedRow()));
            }
        });
        return table;
    }

    /** Inactive currencies are drawn in grey. */
    private TableCellRenderer rowRenderer(Font font) {
        return new Tables.PaddedRenderer(font) {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                                                           boolean focused, int row, int column) {
                super.getTableCellRendererComponent(t, value, selected, focused, row, column);
                if (!selected) {
                    setForeground(shown.get(row).active() ? Theme.INK : Theme.MUTE);
                }
                return this;
            }
        };
    }

    private void reload() {
        shown = services.currencies().all();
        model.setRowCount(0);
        for (Currency c : shown) {
            model.addRow(new Object[] {c.code(), c.name(), Formats.price(c.usdPrice()), c.active()});
        }
        form.clear();
    }

    private void addCurrency() {
        String code = form.code();                // read the fields here, on the EDT
        String name = form.currencyName();
        String price = form.priceText();
        boolean active = form.isActive();
        form.clearErrors();
        BackgroundTask.run(() -> {
            services.currencies().add(code, name, price, active);
            return code.trim() + " added";
        }, this::saved, this::failed);
    }

    private void updateCurrency() {
        String code = form.code();
        String name = form.currencyName();
        String price = form.priceText();
        boolean active = form.isActive();
        form.clearErrors();
        BackgroundTask.run(() -> {
            services.currencies().update(code, name, price, active);
            return code + " updated";
        }, this::saved, this::failed);
    }

    private void deleteCurrency() {
        String code = form.code();
        int answer = JOptionPane.showConfirmDialog(this, "Delete " + code + "? This cannot be undone.",
                "Delete currency", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }
        form.clearErrors();
        BackgroundTask.run(() -> {
            services.currencies().delete(code);
            return code + " deleted";
        }, this::saved, this::failed);
    }

    private void clearSelection() {
        table.clearSelection();
        form.clear();
    }

    private void saved(String message) {
        topBar.showToast(message);
        reload();
    }

    /** Validation problems go under the field; anything else becomes a dialog. */
    private void failed(Exception e) {
        if (e instanceof ValidationException) {
            form.showError((ValidationException) e);
        } else {
            showError(e);
            onShow();                             // the database may have just gone offline
        }
    }
}
