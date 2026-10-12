package com.arbitrage.ui.screens;

import com.arbitrage.db.DatabaseConnection;
import com.arbitrage.ui.UiServices;
import com.arbitrage.ui.theme.AppButton;
import com.arbitrage.ui.theme.BackgroundTask;
import com.arbitrage.ui.theme.Card;
import com.arbitrage.ui.theme.FormRow;
import com.arbitrage.ui.theme.StatusChip;
import com.arbitrage.ui.theme.Theme;
import com.arbitrage.ui.theme.Ui;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;

/**
 * The "Database" card of the Settings screen: the JDBC URL, the user,
 * the current status, and a button that tests the connection.
 */
public class DatabaseCard extends Card {

    private final UiServices services;
    private final Runnable onReconnected;
    private final StatusChip statusChip = new StatusChip(StatusChip.Kind.BAD, "DB offline");
    private final AppButton testButton = new AppButton("Test connection", AppButton.Variant.SECONDARY);
    private final JLabel resultLabel = new JLabel(" ");

    /** onReconnected runs on the EDT when a test brings an offline database back. */
    public DatabaseCard(UiServices services, Runnable onReconnected) {
        super("Database");
        this.services = services;
        this.onReconnected = onReconnected;
        DatabaseConnection database = services.database();
        JTextField urlField = readOnlyField(database.url());
        urlField.setFont(Theme.MONO);
        resultLabel.setFont(Theme.SMALL);
        resultLabel.setForeground(Theme.INK_2);

        JPanel form = new JPanel(new GridLayout(0, 1, 0, Theme.S2));
        form.setOpaque(false);
        form.add(new FormRow("JDBC URL", urlField, "Set in application.properties (db.url)"));
        form.add(new FormRow("User", readOnlyField(database.username()), "The password is never shown"));
        form.add(Ui.row(FlowLayout.LEFT, statusChip));
        JPanel content = new JPanel(new BorderLayout(0, Theme.S3));
        content.setOpaque(false);
        content.add(form, BorderLayout.CENTER);
        content.add(Ui.row(FlowLayout.LEFT, testButton, resultLabel), BorderLayout.SOUTH);
        setContent(content);

        testButton.addActionListener(e -> testConnection());
        refresh();
    }

    public void refresh() {
        if (services.database().isOnline()) {
            statusChip.setStatus(StatusChip.Kind.GOOD, "DB connected");
        } else {
            statusChip.setStatus(StatusChip.Kind.BAD, "DB offline");
        }
    }

    private void testConnection() {
        boolean wasOffline = !services.database().isOnline();
        testButton.setEnabled(false);
        resultLabel.setText("Testing…");
        // When the database was offline, a full reconnect also creates the tables and loads the data.
        BackgroundTask.run(
                () -> wasOffline ? services.startup().connectDatabase() : services.database().test(),
                millis -> {
                    showResult("Connected in " + millis + " ms");
                    if (wasOffline) {
                        onReconnected.run();
                    }
                },
                error -> showResult(error.getMessage()));
    }

    private void showResult(String text) {
        testButton.setEnabled(true);
        resultLabel.setText(text);
        resultLabel.setToolTipText(text);         // the full text, in case the line is too long
        refresh();
    }

    private static JTextField readOnlyField(String text) {
        JTextField field = new JTextField(text);
        field.setEditable(false);
        field.setCaretPosition(0);                // show the start of a long URL
        return field;
    }
}
