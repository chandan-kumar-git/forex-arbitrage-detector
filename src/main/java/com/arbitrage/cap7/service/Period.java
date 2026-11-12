package com.arbitrage.cap7.service;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** The history filter periods, shared by the REST API and the JSP history page. */
public enum Period {
    HOUR, TODAY, WEEK, ALL;

    /** "today", "TODAY" or null (= ALL); an unknown word also means ALL. */
    public static Period parse(String text) {
        if (text == null || text.isBlank()) {
            return ALL;
        }
        try {
            return valueOf(text.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ALL;
        }
    }

    public LocalDateTime since() {
        return switch (this) {
            case HOUR -> LocalDateTime.now().minusHours(1);
            case TODAY -> LocalDate.now().atStartOfDay();
            case WEEK -> LocalDateTime.now().minusDays(7);
            case ALL -> LocalDateTime.of(2000, 1, 1, 0, 0);
        };
    }
}
