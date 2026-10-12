package com.arbitrage.ui;

import com.arbitrage.model.FeedSource;
import com.arbitrage.model.Opportunity;
import com.arbitrage.ui.theme.AppButton;
import com.arbitrage.ui.theme.Formats;
import com.arbitrage.ui.theme.StatusChip;
import com.arbitrage.ui.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.Dimension;

/**
 * The white bar above every screen: status chips on the left, a short message
 * (toast) in the middle, and the Pause / Resume button on the right.
 */
public class TopBar extends JPanel {

    private static final int TOAST_MILLIS = 3000;

    private final UiServices services;
    private final StatusChip feedChip = new StatusChip(StatusChip.Kind.GOOD, "Live");
    private final StatusChip sourceChip = new StatusChip(StatusChip.Kind.INFO, "Local simulator");
    private final StatusChip databaseChip = new StatusChip(StatusChip.Kind.BAD, "DB offline");
    private final StatusChip toast = new StatusChip(StatusChip.Kind.GOOD, "");
    private final AppButton pauseButton = new AppButton("Pause", AppButton.Variant.SECONDARY);
    private final Timer toastTimer = new Timer(TOAST_MILLIS, e -> toast.setVisible(false));
    private Opportunity lastAnnounced;

    public TopBar(UiServices services) {
        this.services = services;
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setBackground(Theme.SURFACE);
        setPreferredSize(new Dimension(0, Theme.TOP_BAR_HEIGHT));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                BorderFactory.createEmptyBorder(0, Theme.S5, 0, Theme.S5)));

        add(feedChip);
        add(Box.createHorizontalStrut(Theme.S2));
        add(sourceChip);
        add(Box.createHorizontalStrut(Theme.S2));
        add(databaseChip);
        add(Box.createHorizontalGlue());
        add(toast);
        add(Box.createHorizontalGlue());
        add(pauseButton);

        toast.setVisible(false);
        toast.setMinimumSize(new Dimension(40, 0));   // only the toast may shrink when space is tight
        toastTimer.setRepeats(false);                 // one shot: hide the toast after 3 s
        pauseButton.addActionListener(e -> togglePause());
        refresh();
    }

    /** Shows a green message in the middle of the bar for three seconds. */
    public void showToast(String text) {
        toast.setStatus(StatusChip.Kind.GOOD, text);
        toast.setVisible(true);
        toastTimer.restart();
    }

    /** Called by the main window once a second (on the EDT). */
    public void refresh() {
        boolean paused = services.scheduler().isPaused();
        if (services.feed().isFeedLost()) {
            feedChip.setStatus(StatusChip.Kind.BAD, "Disconnected");
        } else if (paused) {
            feedChip.setStatus(StatusChip.Kind.WARN, "Paused");
        } else {
            feedChip.setStatus(StatusChip.Kind.GOOD, "Live");
        }
        pauseButton.setText(paused ? "Resume" : "Pause");
        boolean local = services.feed().mode() == FeedSource.LOCAL;
        sourceChip.setStatus(StatusChip.Kind.INFO, local ? "Local simulator" : "Socket feed");
        if (services.database().isOnline()) {
            databaseChip.setStatus(StatusChip.Kind.GOOD, "DB connected");
        } else {
            databaseChip.setStatus(StatusChip.Kind.BAD, "DB offline");
        }
        announceNewOpportunity();
    }

    private void togglePause() {
        services.scheduler().setPaused(!services.scheduler().isPaused());
        refresh();
    }

    private void announceNewOpportunity() {
        Opportunity latest = services.opportunities().latest();
        if (latest != null && latest != lastAnnounced) {
            lastAnnounced = latest;
            showToast("New arbitrage: " + latest.pathText() + "  +" + Formats.profit(latest.profitPercent()));
        }
    }
}
