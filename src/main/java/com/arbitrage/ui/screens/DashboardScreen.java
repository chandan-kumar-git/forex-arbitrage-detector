package com.arbitrage.ui.screens;

import com.arbitrage.engine.TickStats;
import com.arbitrage.model.Opportunity;
import com.arbitrage.ui.TopBar;
import com.arbitrage.ui.UiServices;
import com.arbitrage.ui.theme.Card;
import com.arbitrage.ui.theme.EmptyState;
import com.arbitrage.ui.theme.Formats;
import com.arbitrage.ui.theme.StatCard;
import com.arbitrage.ui.theme.Tables;
import com.arbitrage.ui.theme.Theme;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.List;

/**
 * The first screen: four stat cards, the live network graph,
 * and the ten most recent opportunities.
 */
public class DashboardScreen extends BaseScreen {

    private static final int RECENT_ROWS = 10;
    private static final long HIGHLIGHT_MILLIS = 1500;

    private final StatCard ticksCard = new StatCard("Ticks", "since start");
    private final StatCard foundCard = new StatCard("Opportunities", "this session");
    private final StatCard bestCard = new StatCard("Best profit", "no arbitrage yet");
    private final StatCard tickTimeCard = new StatCard("Avg tick time", "last 60 ticks");
    private final NetworkPanel networkPanel = new NetworkPanel();
    private final DefaultTableModel recentModel = Tables.readOnlyModel("Path", "Profit", "Time");
    private final CardLayout recentLayout = new CardLayout();
    private final JPanel recentBody = new JPanel(recentLayout);
    private Opportunity lastListed;

    public DashboardScreen(UiServices services, TopBar topBar) {
        super("Dashboard", "Live view of the currency network and detected arbitrage", services, topBar);
        JPanel body = new JPanel(new BorderLayout(0, Theme.S4));
        body.setOpaque(false);
        body.add(buildStatRow(), BorderLayout.NORTH);
        body.add(buildLowerRow(), BorderLayout.CENTER);
        setBody(body);
    }

    @Override
    public void onShow() {
        refresh();
    }

    @Override
    public void refresh() {
        updateStats();
        if (!services.scheduler().isPaused()) {
            updateNetwork();                      // while paused the picture stays frozen for study
        }
        updateRecent();
    }

    private JPanel buildStatRow() {
        JPanel row = new JPanel(new GridLayout(1, 4, Theme.S4, 0));   // four equal columns
        row.setOpaque(false);
        row.add(ticksCard);
        row.add(foundCard);
        row.add(bestCard);
        row.add(tickTimeCard);
        return row;
    }

    private JPanel buildLowerRow() {
        Card networkCard = new Card("Live network");
        networkCard.setContent(networkPanel);
        Card recentCard = new Card("Recent opportunities");
        recentCard.setContent(buildRecentBody());
        // Both cards ask for size 0, so the weights alone split the width 60 / 40.
        networkCard.setPreferredSize(new Dimension(0, 0));
        recentCard.setPreferredSize(new Dimension(0, 0));

        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.gridy = 0;
        c.weighty = 1.0;
        c.gridx = 0;
        c.weightx = 0.6;
        c.insets = new Insets(0, 0, 0, Theme.S4);
        row.add(networkCard, c);
        c.gridx = 1;
        c.weightx = 0.4;
        c.insets = new Insets(0, 0, 0, 0);
        row.add(recentCard, c);
        return row;
    }

    private JPanel buildRecentBody() {
        JTable table = new JTable(recentModel);
        Tables.style(table);
        table.getColumnModel().getColumn(0).setPreferredWidth(190);
        table.getColumnModel().getColumn(1).setCellRenderer(Tables.profitRenderer());
        table.getColumnModel().getColumn(1).setMinWidth(64);     // profit and time keep a fixed width,
        table.getColumnModel().getColumn(1).setMaxWidth(64);     // the path gets all the rest
        table.getColumnModel().getColumn(2).setMinWidth(68);
        table.getColumnModel().getColumn(2).setMaxWidth(68);
        recentBody.setOpaque(false);
        recentBody.add(new EmptyState("No arbitrage yet", "It usually appears within a few seconds."), "empty");
        recentBody.add(Tables.scroll(table), "table");
        return recentBody;
    }

    private void updateStats() {
        TickStats stats = services.stats();
        ticksCard.setValue(Formats.count(stats.ticks()));
        foundCard.setValue(Formats.count(stats.found()));
        bestCard.setValue(Formats.profit(stats.bestProfit()));
        bestCard.setHint(stats.bestPath().isEmpty() ? "no arbitrage yet" : stats.bestPath());
        tickTimeCard.setValue(Formats.tickTime(stats.averageMicros()));
    }

    /** The newest cycle stays red for 1.5 seconds after it was found. */
    private void updateNetwork() {
        Opportunity latest = services.opportunities().latest();
        boolean fresh = latest != null
                && System.currentTimeMillis() - latest.detectedAt().toEpochMilli() < HIGHLIGHT_MILLIS;
        networkPanel.update(services.rates().getLatest(), fresh ? latest : null);
    }

    private void updateRecent() {
        List<Opportunity> all = services.opportunities().all();
        Opportunity newest = all.isEmpty() ? null : all.get(0);
        if (newest == lastListed) {
            return;                               // nothing new: keep the table as it is
        }
        lastListed = newest;
        recentModel.setRowCount(0);
        for (int i = 0; i < all.size() && i < RECENT_ROWS; i++) {
            Opportunity o = all.get(i);
            String path = String.join("→", o.path());   // short form, so it fits the narrow card
            recentModel.addRow(new Object[] {path, o.profitPercent(), Formats.time(o.detectedAt())});
        }
        recentLayout.show(recentBody, "table");
    }
}
