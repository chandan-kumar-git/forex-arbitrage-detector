package com.arbitrage.cap7.service;

/** Thrown when an id does not exist; the REST error handler turns it into 404 Not Found. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
