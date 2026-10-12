package com.arbitrage.ui.screens;

import com.arbitrage.feed.RateFeedManager;
import com.arbitrage.model.FeedSource;
import com.arbitrage.ui.TopBar;
import com.arbitrage.ui.UiServices;
import com.arbitrage.ui.theme.AppButton;
import com.arbitrage.ui.theme.BackgroundTask;
import com.arbitrage.ui.theme.Card;
import com.arbitrage.ui.theme.FormRow;
import com.arbitrage.ui.theme.Theme;
import com.arbitrage.ui.theme.Ui;
import com.arbitrage.validation.SettingsValidator;
import com.arbitrage.validation.ValidationException;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.List;

/**
 * Chooses where the rates come from: the local simulator or a Rate Feed Server (socket).
 * Shows the connection status and the protocol messages.
 */
public class FeedScreen extends BaseScreen {

    private final JRadioButton localButton = new JRadioButton("Local simulator", true);
    private final JRadioButton socketButton = new JRadioButton("Socket feed");
    private final JTextField hostField = new JTextField();
    private final JSpinner portSpinner = new JSpinner(new SpinnerNumberModel(5050, 1, 65535, 1));
    private final FormRow hostRow = new FormRow("Host", hostField, "Name or IP address of the server");
    private final FormRow portRow = new FormRow("Port", portSpinner, "1024 to 65535");
    private final AppButton connectButton = new AppButton("Connect", AppButton.Variant.PRIMARY);
    private final AppButton disconnectButton = new AppButton("Disconnect", AppButton.Variant.SECONDARY);
    private final FeedStatusCard statusCard = new FeedStatusCard();
    private final JTextArea logArea = new JTextArea();
    private boolean connecting = false;
    private int shownLogLines = -1;

    public FeedScreen(UiServices services, TopBar topBar) {
        super("Feed", "Where rates come from", services, topBar);
        hostField.setText(services.feed().defaultHost());
        portSpinner.setValue(services.feed().defaultPort());
        portSpinner.setEditor(new JSpinner.NumberEditor(portSpinner, "#"));   // "#" = no thousands separator

        JPanel top = new JPanel(new GridLayout(1, 2, Theme.S4, 0));
        top.setOpaque(false);
        top.add(buildSourceCard());
        top.add(statusCard);
        JPanel body = new JPanel(new BorderLayout(0, Theme.S4));
        body.setOpaque(false);
        body.add(top, BorderLayout.NORTH);
        body.add(buildLogCard(), BorderLayout.CENTER);
        setBody(body);

        localButton.addActionListener(e -> useLocal());
        socketButton.addActionListener(e -> refresh());
        connectButton.addActionListener(e -> connect());
        disconnectButton.addActionListener(e -> useLocal());
    }

    @Override
    public void onShow() {
        refresh();
    }

    @Override
    public void refresh() {
        RateFeedManager feed = services.feed();
        boolean socketMode = feed.mode() == FeedSource.SOCKET;
        if (socketMode) {
            socketButton.setSelected(true);
        }
        boolean socketChosen = socketButton.isSelected();
        hostField.setEnabled(socketChosen && !socketMode);
        portSpinner.setEnabled(socketChosen && !socketMode);
        connectButton.setEnabled(socketChosen && !socketMode && !connecting);
        connectButton.setText(connecting ? "Connecting…" : "Connect");
        disconnectButton.setEnabled(socketMode);
        statusCard.refresh(feed);
        showLog(feed.socket().recentLog());
    }

    private Card buildSourceCard() {
        ButtonGroup group = new ButtonGroup();    // only one of the two can be selected
        group.add(localButton);
        group.add(socketButton);
        localButton.setOpaque(false);
        socketButton.setOpaque(false);

        JPanel address = new JPanel(new GridLayout(1, 2, Theme.S3, 0));
        address.setOpaque(false);
        address.add(hostRow);
        address.add(portRow);
        JPanel lines = new JPanel(new GridLayout(0, 1, 0, Theme.S1));
        lines.setOpaque(false);
        lines.add(localButton);
        lines.add(socketButton);
        JPanel content = new JPanel(new BorderLayout(0, Theme.S2));
        content.setOpaque(false);
        content.add(lines, BorderLayout.NORTH);
        content.add(address, BorderLayout.CENTER);
        content.add(Ui.row(FlowLayout.LEFT, connectButton, disconnectButton), BorderLayout.SOUTH);
        Card card = new Card("Source");
        card.setContent(content);
        return card;
    }

    private Card buildLogCard() {
        logArea.setFont(Theme.MONO);
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        Card card = new Card("Protocol log");
        card.setContent(scrollPane);
        return card;
    }

    private void connect() {
        String host = hostField.getText();        // read the fields here, on the EDT
        int port = ((Number) portSpinner.getValue()).intValue();
        hostRow.clearError();
        portRow.clearError();
        try {
            SettingsValidator.validateHost(host);
            SettingsValidator.validatePort(port);
        } catch (ValidationException e) {
            FormRow row = e.getField().equals("host") ? hostRow : portRow;
            row.showError(e.getMessage());
            return;
        }
        connecting = true;
        refresh();
        // connectSocket() can wait up to 3 seconds, so it must not run on the EDT.
        BackgroundTask.run(() -> {
            services.feed().connectSocket(host, port);
            return host.trim() + ":" + port;
        }, address -> {
            connecting = false;
            topBar.showToast("Connected to " + address);
            refresh();
        }, error -> {
            connecting = false;
            refresh();
            showError(error);
        });
    }

    private void useLocal() {
        BackgroundTask.run(() -> {
            services.feed().useLocal();
            return true;
        }, done -> {
            localButton.setSelected(true);
            refresh();
        }, this::showError);
    }

    /** Rewrites the text area only when new lines arrived, then scrolls to the end. */
    private void showLog(List<String> lines) {
        String last = lines.isEmpty() ? "" : lines.get(lines.size() - 1);
        int signature = lines.size() * 31 + last.hashCode();
        if (signature == shownLogLines) {
            return;
        }
        shownLogLines = signature;
        if (lines.isEmpty()) {
            logArea.setText("No messages yet. Choose Socket feed and press Connect to see the protocol here.");
        } else {
            logArea.setText(String.join("\n", lines));
            logArea.setCaretPosition(logArea.getDocument().getLength());
        }
    }
}
