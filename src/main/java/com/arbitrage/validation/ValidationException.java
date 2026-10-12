package com.arbitrage.validation;

/**
 * Thrown when user input is wrong. It remembers which field was wrong,
 * so the screen can show the message under that field.
 */
public class ValidationException extends Exception {

    private final String field;

    public ValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
