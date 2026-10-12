package com.arbitrage.feed;

/**
 * Thrown when the socket feed has a problem: the server cannot be reached,
 * or a message does not follow the protocol.
 */
public class FeedException extends Exception {

    public FeedException(String message, Throwable cause) {
        super(message, cause);
    }
}
