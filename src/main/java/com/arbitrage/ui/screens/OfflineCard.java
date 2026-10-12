package com.arbitrage.ui.screens;

import com.arbitrage.ui.UiServices;
import com.arbitrage.ui.theme.AppButton;
import com.arbitrage.ui.theme.BackgroundTask;
import com.arbitrage.ui.theme.Card;
import com.arbitrage.ui.theme.EmptyState;

/**
 * Shown instead of a screen's content when MySQL cannot be reached:
 * "Database offline", the reason in plain words, and a Retry button.
 */
public class OfflineCard extends Card {

    private final UiServices services;
    private final Runnable onReconnected;
    private final EmptyState message = new EmptyState("Database offline", " ");
    private final AppButton retryButton = new AppButton("Retry", AppButton.Variant.PRIMARY);

    /** onReconnected runs on the EDT after a successful retry. */
    public OfflineCard(UiServices services, Runnable onReconnected) {
        super(null);
        this.services = services;
        this.onReconnected = onReconnected;
        message.addButton(retryButton);
        setContent(message);
        retryButton.addActionListener(e -> retry());
    }

    /** Shows the newest error text. Call this before the card becomes visible. */
    public void showLastError() {
        message.setHint(services.database().lastError());
    }

    private void retry() {
        retryButton.setEnabled(false);
        message.setHint("Connecting…");
        // connectDatabase() talks to MySQL, so it runs on a background thread.
        BackgroundTask.run(() -> services.startup().connectDatabase(),
                millis -> {
                    retryButton.setEnabled(true);
                    onReconnected.run();
                },
                error -> {
                    retryButton.setEnabled(true);
                    message.setHint(error.getMessage());
                });
    }
}
