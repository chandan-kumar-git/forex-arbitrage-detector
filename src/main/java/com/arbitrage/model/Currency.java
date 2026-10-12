package com.arbitrage.model;

import java.util.List;

/**
 * One currency: its 3-letter code, its name, its price in US dollars,
 * and whether the simulator should use it.
 */
public record Currency(String code, String name, double usdPrice, boolean active) {

    /** Built-in list used when the database is offline and by the feed server. */
    public static List<Currency> defaults() {
        return List.of(
                new Currency("USD", "US Dollar", 1.0, true),
                new Currency("EUR", "Euro", 1.08, true),
                new Currency("GBP", "British Pound", 1.27, true),
                new Currency("INR", "Indian Rupee", 0.012, true),
                new Currency("JPY", "Japanese Yen", 0.0067, true));
    }
}
