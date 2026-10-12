package com.arbitrage.feed;

import com.arbitrage.config.AppSettings;
import com.arbitrage.model.Currency;
import com.arbitrage.model.FeedSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Owns the two rate providers and remembers which one is in use.
 * The scheduler always asks current() and does not care which provider it gets (polymorphism).
 */
@Component
public class RateFeedManager {

    private final SimulatedRateProvider simulator;
    private final SocketRateProvider socket = new SocketRateProvider();
    private final String defaultHost;
    private final int defaultPort;
    private volatile FeedSource mode = FeedSource.LOCAL;

    public RateFeedManager(AppSettings settings,
                           @Value("${feed.default-host}") String defaultHost,
                           @Value("${feed.default-port}") int defaultPort) {
        this.simulator = new SimulatedRateProvider(Currency.defaults(), settings);
        this.defaultHost = defaultHost;
        this.defaultPort = defaultPort;
    }

    /** The provider the scheduler should read from right now. */
    public RateProvider current() {
        return mode == FeedSource.SOCKET ? socket : simulator;
    }

    /** Back to the simulator inside this program. */
    public void useLocal() {
        socket.disconnect();
        mode = FeedSource.LOCAL;
    }

    /** Connects to a Rate Feed Server; the mode changes only if the connection worked. */
    public void connectSocket(String host, int port) throws FeedException {
        // The process id makes the name different for two desktops on one computer.
        socket.connect(host, port, "desk-" + ProcessHandle.current().pid());
        mode = FeedSource.SOCKET;
    }

    /** Closes the socket but keeps the mode (used when the program exits). */
    public void disconnect() {
        socket.disconnect();
    }

    /** True when socket mode is chosen but the connection is gone. */
    public boolean isFeedLost() {
        return mode == FeedSource.SOCKET && socket.status() != FeedStatus.CONNECTED;
    }

    public FeedSource mode() {
        return mode;
    }

    public SimulatedRateProvider simulator() {
        return simulator;
    }

    public SocketRateProvider socket() {
        return socket;
    }

    public String defaultHost() {
        return defaultHost;
    }

    public int defaultPort() {
        return defaultPort;
    }
}
