package com.arbitrage.cap7.service;

/** Thrown when a request clashes with existing data (duplicate code, used in history); becomes 409 Conflict. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
