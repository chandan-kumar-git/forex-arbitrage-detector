package com.arbitrage.ui.screens;

import com.arbitrage.feed.FeedStatus;
import com.arbitrage.feed.RateFeedManager;
import com.arbitrage.feed.SocketRateProvider;
import com.arbitrage.model.FeedSource;
import com.arbitrage.ui.theme.Card;
import com.arbitrage.ui.theme.Formats;
import com.arbitrage.ui.theme.StatusChip;
import com.arbitrage.ui.theme.Theme;
import com.arbitrage.ui.theme.Ui;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;

/**
 * The "Status" card of the Feed screen: connection state, server address
 * and the message counters of the socket client.
 */
public class FeedStatusCard extends Card {

    private final StatusChip chip = new StatusChip(StatusChip.Kind.INFO, "Local simulator");
    private final JLabel addressLabel = new JLabel(" ");
    private final JLabel messagesLabel = new JLabel(" ");
    private final JLabel badLabel = new JLabel(" ");

    public FeedStatusCard() {
        super("Status");
        addressLabel.setFont(Theme.MONO);
        messagesLabel.setForeground(Theme.INK_2);
        badLabel.setForeground(Theme.INK_2);
        JPanel lines = new JPanel(new GridLayout(0, 1, 0, Theme.S2));
        lines.setOpaque(false);
        lines.add(Ui.row(FlowLayout.LEFT, chip, addressLabel));
        lines.add(messagesLabel);
        lines.add(badLabel);
        JPanel holder = new JPanel(new BorderLayout());
        holder.setOpaque(false);
        holder.add(lines, BorderLayout.NORTH);    // NORTH keeps the lines at the top of the card
        setContent(holder);
    }

    /** Called once a second on the EDT. It only reads values the network thread has stored. */
    public void refresh(RateFeedManager feed) {
        SocketRateProvider socket = feed.socket();
        if (feed.mode() == FeedSource.LOCAL && socket.status() != FeedStatus.FAILED) {
            chip.setStatus(StatusChip.Kind.INFO, "Local simulator");
            addressLabel.setText(" ");
            messagesLabel.setText("Rates are made inside this app. No network is used.");
            badLabel.setText(" ");
            return;
        }
        if (socket.status() == FeedStatus.CONNECTED) {
            chip.setStatus(StatusChip.Kind.GOOD, "Connected");
        } else if (socket.status() == FeedStatus.CONNECTING) {
            chip.setStatus(StatusChip.Kind.WARN, "Connecting");
        } else if (socket.status() == FeedStatus.FAILED) {
            chip.setStatus(StatusChip.Kind.BAD, "Connection failed");
        } else {
            chip.setStatus(StatusChip.Kind.BAD, "Disconnected");
        }
        addressLabel.setText(socket.address());
        messagesLabel.setText("Messages " + Formats.count(socket.messages()) + " · Last " + lastMessageAge(socket));
        badLabel.setText("Bad messages " + Formats.count(socket.badMessages()));
    }

    private String lastMessageAge(SocketRateProvider socket) {
        if (socket.lastMessageMillis() == 0) {
            return "none yet";
        }
        double seconds = (System.currentTimeMillis() - socket.lastMessageMillis()) / 1000.0;
        return String.format("%.1f s ago", seconds);
    }
}
