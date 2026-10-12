package com.arbitrage.server;

/**
 * What the server window shows about one connected client:
 * the name it sent in HELLO and its network address.
 */
public record ClientInfo(String name, String address) {
}
