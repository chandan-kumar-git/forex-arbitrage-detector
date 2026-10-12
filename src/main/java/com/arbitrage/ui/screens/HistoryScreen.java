package com.arbitrage.ui.screens;

import com.arbitrage.db.OpportunityDao;
import com.arbitrage.model.Opportunity;
import com.arbitrage.ui.TopBar;
import com.arbitrage.ui.UiServices;
import com.arbitrage.ui.theme.AppButton;
import com.arbitrage.ui.theme.BackgroundTask;
import com.arbitrage.ui.theme.Card;
import com.arbitrage.ui.theme.Formats;
import com.arbitrage.ui.theme.Tables;
import com.arbitrage.ui.theme.Theme;
import com.arbitrage.validation.ValidationException;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Every opportunity saved in MySQL: filter, sort, open the details, delete.
 * All database calls run through BackgroundTask, never on the EDT.
 */
public class HistoryScreen extends BaseScreen {

    /** What one background load brings back. */
    private record Loaded(List<Opportunity> rows, OpportunityDao.Summary summary) {
    }

    private final HistoryFilters filters = new HistoryFilters();
    private final HistoryTable table = new HistoryTable();
    private final Card resultCard = new Card("Loading…");
    private final AppButton deleteButton = new AppButton("Delete selected", AppButton.Variant.SECONDARY);
    private final AppButton clearButton = new AppButton("Clear all", AppButton.Variant.DANGER);
    private final CardLayout bodyLayout = new CardLayout();
    private final JPanel body = new JPanel(bodyLayout);
    private final OfflineCard offlineCard;

    public HistoryScreen(UiServices services, TopBar topBar) {
        super("History", "Every opportunity saved in MySQL", services, topBar);
        offlineCard = new OfflineCard(services, this::load);
        resultCard.addAction(deleteButton);
        resultCard.addAction(clearButton);
        resultCard.setContent(Tables.scroll(table));

        JPanel data = new JPanel(new BorderLayout(0, Theme.S4));
        data.setOpaque(false);
        data.add(filters, BorderLayout.NORTH);
        data.add(resultCard, BorderLayout.CENTER);
        body.setOpaque(false);
        body.add(data, "data");
        body.add(offlineCard, "offline");
        setBody(body);

        filters.applyButton.addActionListener(e -> load());
        filters.resetButton.addActionListener(e -> {
            filters.reset();
            load();
        });
        deleteButton.addActionListener(e -> deleteSelected());
        clearButton.addActionListener(e -> clearAll());
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.hasSelection()) {   // double-click opens the details
                    openDetails(table.selected());
                }
            }
        });
    }

    @Override
    public void onShow() {
        load();
    }

    /** Reads the filters on the EDT, then queries MySQL in the background. */
    private void load() {
        if (!services.database().isOnline()) {
            showOffline();
            return;
        }
        LocalDateTime since = filters.since();
        double minProfit = filters.minProfit();
        String codeLike;
        try {
            codeLike = filters.codeLike();
            filters.clearError();
        } catch (ValidationException e) {
            filters.showError(e.getMessage());
            return;
        }
        setBusy(true);
        bodyLayout.show(body, "data");
        OpportunityDao dao = services.history();
        BackgroundTask.run(
                () -> new Loaded(dao.search(since, minProfit, codeLike), dao.summary(since, minProfit, codeLike)),
                this::showLoaded, this::failed);
    }

    private void showLoaded(Loaded loaded) {
        table.setRows(loaded.rows());
        OpportunityDao.Summary summary = loaded.summary();
        if (summary.count() == 0) {
            resultCard.setTitle("No opportunities match – widen the filters or wait for new ones");
        } else {
            resultCard.setTitle(Formats.count(summary.count()) + " opportunities · avg "
                    + Formats.profit(summary.averageProfit()) + " · best " + Formats.profit(summary.bestProfit()));
        }
        setBusy(false);
    }

    private void failed(Exception e) {
        setBusy(false);
        if (services.database().isOnline()) {
            resultCard.setTitle("Could not load the history – press Apply to try again");
            showError(e);
        } else {
            showOffline();
        }
    }

    private void showOffline() {
        offlineCard.showLastError();
        bodyLayout.show(body, "offline");
    }

    /** While loading, the summary line says so and the buttons cannot be clicked. */
    private void setBusy(boolean busy) {
        if (busy) {
            resultCard.setTitle("Loading…");
        }
        deleteButton.setEnabled(!busy);
        clearButton.setEnabled(!busy);
        filters.applyButton.setEnabled(!busy);
    }

    private void openDetails(Opportunity opportunity) {
        BackgroundTask.run(() -> services.history().findLegs(opportunity.id()),
                legs -> new OpportunityDialog(SwingUtilities.getWindowAncestor(this), opportunity, legs)
                        .setVisible(true),
                this::failed);
    }

    private void deleteSelected() {
        if (!table.hasSelection()) {
            JOptionPane.showMessageDialog(this, "Select a row in the table first.");
            return;
        }
        Opportunity opportunity = table.selected();
        int answer = JOptionPane.showConfirmDialog(this, "Delete opportunity #" + opportunity.id() + "?",
                "Delete opportunity", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }
        BackgroundTask.run(() -> services.history().delete(opportunity.id()),
                rows -> afterDelete(rows), this::failed);
    }

    private void clearAll() {
        String typed = JOptionPane.showInputDialog(this,
                "This deletes every saved opportunity. Type DELETE to confirm.",
                "Clear all history", JOptionPane.WARNING_MESSAGE);
        if (!"DELETE".equals(typed)) {
            return;
        }
        BackgroundTask.run(() -> services.history().deleteAll(), rows -> afterDelete(rows), this::failed);
    }

    private void afterDelete(int rows) {
        topBar.showToast(Formats.count(rows) + (rows == 1 ? " opportunity deleted" : " opportunities deleted"));
        load();
    }
}
