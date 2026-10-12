package com.arbitrage.model;

/**
 * Where the rates come from: the simulator inside this program (LOCAL)
 * or the Rate Feed Server over a socket (SOCKET).
 */
public enum FeedSource {
    LOCAL,
    SOCKET
}
