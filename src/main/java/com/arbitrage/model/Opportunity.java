package com.arbitrage.model;

import java.time.Instant;
import java.util.List;

/**
 * One detected arbitrage: the loop of currency codes (for example INR, EUR, GBP, INR)
 * and the profit in percent after fees. The id is 0 until it is saved in MySQL.
 */
public record Opportunity(long id, List<String> path, double profitPercent, double fee,
                          FeedSource source, Instant detectedAt) {

    public Opportunity {
        path = List.copyOf(path);
    }

    /** The path as text, for example "INR → EUR → GBP → INR". */
    public String pathText() {
        return String.join(" → ", path);
    }

    /** The same opportunity with the id given by the database. */
    public Opportunity withId(long newId) {
        return new Opportunity(newId, path, profitPercent, fee, source, detectedAt);
    }
}
