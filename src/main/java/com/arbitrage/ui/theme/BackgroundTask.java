package com.arbitrage.ui.theme;

import javax.swing.SwingWorker;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * Runs slow work (database, network) on a background thread, so the window never freezes.
 * The result or the error comes back on the Swing thread (EDT), where it is safe to touch the UI.
 */
public final class BackgroundTask {

    private BackgroundTask() {
    }

    public static <T> void run(Callable<T> work, Consumer<T> onSuccess, Consumer<Exception> onError) {
        SwingWorker<T, Void> worker = new SwingWorker<>() {
            @Override
            protected T doInBackground() throws Exception {
                return work.call();               // runs on a SwingWorker thread, NOT on the EDT
            }

            @Override
            protected void done() {               // runs on the EDT
                T result;
                try {
                    result = get();
                } catch (ExecutionException e) {
                    onError.accept(asException(e.getCause()));
                    return;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    onError.accept(e);
                    return;
                }
                onSuccess.accept(result);
            }
        };
        worker.execute();
    }

    private static Exception asException(Throwable cause) {
        if (cause instanceof Exception) {
            return (Exception) cause;
        }
        return new RuntimeException(cause);
    }
}
