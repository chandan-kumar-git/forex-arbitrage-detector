package com.arbitrage.server;

import com.arbitrage.config.AppSettings;
import com.arbitrage.feed.FeedProtocol;
import com.arbitrage.feed.SimulatedRateProvider;
import com.arbitrage.model.Currency;
import com.arbitrage.ui.theme.AppButton;
import com.arbitrage.ui.theme.BackgroundTask;
import com.arbitrage.ui.theme.Card;
import com.arbitrage.ui.theme.StatusChip;
import com.arbitrage.ui.theme.Theme;
import com.arbitrage.ui.theme.Ui;
import com.arbitrage.validation.SettingsValidator;
import com.arbitrage.validation.ValidationException;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.net.BindException;

/**
 * The window of the Rate Feed Server: a port field, a Start / Stop button,
 * the list of connected clients, a log, and counters at the bottom.
 */
public class ServerWindow extends JFrame {

    private final AppSettings settings = new AppSettings();
    private final JTextField portField = new JTextField(String.valueOf(FeedProtocol.DEFAULT_PORT));
    private final AppButton startButton = new AppButton("Start server", AppButton.Variant.PRIMARY);
    private final StatusChip statusChip = new StatusChip(StatusChip.Kind.WARN, "Stopped");
    private final JLabel messageLabel = new JLabel(" ");
    private final DefaultListModel<String> clientModel = new DefaultListModel<>();
    private final JTextArea logArea = new JTextArea();
    private final JLabel countersLabel = new JLabel(" ");
    private FeedServer server;                    // null until Start is pressed for the first time

    public ServerWindow() {
        super("Rate Feed Server");
        JPanel content = new JPanel(new BorderLayout(0, Theme.S4));
        content.setBackground(Theme.APP_BG);
        content.setBorder(BorderFactory.createEmptyBorder(Theme.S4, Theme.S4, Theme.S4, Theme.S4));
        content.add(buildTopCard(), BorderLayout.NORTH);
        content.add(buildMiddle(), BorderLayout.CENTER);
        countersLabel.setFont(Theme.SMALL);
        countersLabel.setForeground(Theme.INK_2);
        content.add(countersLabel, BorderLayout.SOUTH);
        setContentPane(content);

        setSize(760, 480);
        setMinimumSize(new Dimension(640, 400));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);  // closing the window ends the program and all its threads
        startButton.addActionListener(e -> toggleServer());
        new Timer(1000, e -> refresh()).start();  // Swing Timer: refresh() runs on the EDT
        refresh();
    }

    private Card buildTopCard() {
        Ui.styleField(portField);
        portField.setPreferredSize(new Dimension(80, Theme.FIELD_HEIGHT));
        messageLabel.setFont(Theme.SMALL);
        messageLabel.setForeground(Theme.BAD);
        JPanel row = new JPanel(new BorderLayout(Theme.S3, 0));
        row.setOpaque(false);
        row.add(Ui.row(FlowLayout.LEFT, new JLabel("Port"), portField, startButton, messageLabel), BorderLayout.CENTER);
        row.add(Ui.row(FlowLayout.RIGHT, statusChip), BorderLayout.EAST);
        Card card = new Card(null);
        card.setContent(row);
        return card;
    }

    private JPanel buildMiddle() {
        JList<String> clientList = new JList<>(clientModel);
        clientList.setFont(Theme.MONO);
        logArea.setFont(Theme.MONO);
        logArea.setEditable(false);
        Card clientsCard = new Card("Connected clients");
        clientsCard.setContent(plainScroll(clientList));
        Card logCard = new Card("Log");
        logCard.setContent(plainScroll(logArea));

        JPanel middle = new JPanel(new GridLayout(1, 2, Theme.S4, 0));
        middle.setOpaque(false);
        middle.add(clientsCard);
        middle.add(logCard);
        return middle;
    }

    private JScrollPane plainScroll(java.awt.Component view) {
        JScrollPane scrollPane = new JScrollPane(view);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        return scrollPane;
    }

    private void toggleServer() {
        messageLabel.setText(" ");
        if (server != null && server.isRunning()) {
            FeedServer running = server;
            startButton.setEnabled(false);
            BackgroundTask.run(() -> {
                running.stop();
                return true;
            }, done -> refresh(), this::showProblem);
            return;
        }
        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
            SettingsValidator.validatePort(port);
        } catch (NumberFormatException | ValidationException e) {
            messageLabel.setText("Port must be a number between 1024 and 65535");
            return;
        }
        FeedServer newServer = new FeedServer(port, new SimulatedRateProvider(Currency.defaults(), settings));
        startButton.setEnabled(false);
        // Binding the port is network work, so it runs off the EDT.
        BackgroundTask.run(() -> {
            newServer.start();
            return newServer;
        }, started -> {
            server = started;
            refresh();
        }, this::showProblem);
    }

    private void showProblem(Exception e) {
        if (e instanceof BindException) {
            messageLabel.setText("Port " + portField.getText().trim() + " is already in use");
        } else {
            messageLabel.setText("Problem: " + e.getMessage());
        }
        refresh();
    }

    /** Called every second by the timer: copies the server state into the window. */
    private void refresh() {
        boolean running = server != null && server.isRunning();
        startButton.setEnabled(true);
        startButton.setText(running ? "Stop server" : "Start server");
        startButton.setVariant(running ? AppButton.Variant.DANGER : AppButton.Variant.PRIMARY);
        portField.setEditable(!running);
        long sent = 0;
        clientModel.clear();
        if (server != null) {
            sent = server.snapshotsSent();
            for (ClientInfo client : server.clients()) {
                clientModel.addElement(client.name() + "   " + client.address());
            }
            logArea.setText(String.join("\n", server.recentLog()));
            logArea.setCaretPosition(logArea.getDocument().getLength());   // scroll to the newest line
        }
        int count = clientModel.size();
        if (running) {
            statusChip.setStatus(StatusChip.Kind.GOOD, "Running · " + count + (count == 1 ? " client" : " clients"));
        } else {
            statusChip.setStatus(StatusChip.Kind.WARN, "Stopped");
        }
        countersLabel.setText(String.format("Snapshots sent %,d · Interval 1000 ms · Inject chance %.0f %%",
                sent, settings.getInjectChance() * 100));
    }
}
