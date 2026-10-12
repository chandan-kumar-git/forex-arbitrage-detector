package com.arbitrage.feed;

/**
 * The state of the socket connection to the Rate Feed Server.
 */
public enum FeedStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    FAILED
}
