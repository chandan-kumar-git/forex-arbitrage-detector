package com.arbitrage.db;

/**
 * Thrown by the DAO classes when a database call fails.
 * The message is written for the user; the original SQLException is kept as the cause for the log.
 */
public class DatabaseException extends RuntimeException {

    public DatabaseException(String userMessage, Throwable cause) {
        super(userMessage, cause);
    }
}
