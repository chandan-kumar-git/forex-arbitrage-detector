package com.arbitrage.ui.theme;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * One place for how numbers and times are written on screen,
 * so every screen shows them the same way.
 */
public final class Formats {

    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private Formats() {
    }

    public static String time(Instant instant) {
        return TIME.format(instant);
    }

    public static String timeNow() {
        return TIME.format(Instant.now());
    }

    public static String dateTime(Instant instant) {
        return DATE_TIME.format(instant);
    }

    /** Profit like 0.347%. */
    public static String profit(double percent) {
        return String.format("%.3f%%", percent);
    }

    /** Prices like 1.08000000. */
    public static String price(double price) {
        return String.format("%.8f", price);
    }

    /** Counts like 1,284. */
    public static String count(long number) {
        return String.format("%,d", number);
    }

    /** A rate on the graph: big rates need fewer decimals than small ones. */
    public static String rate(double rate) {
        if (rate >= 100) {
            return String.format("%.2f", rate);
        }
        if (rate >= 1) {
            return String.format("%.4f", rate);
        }
        return String.format("%.6f", rate);
    }

    /** Tick time in microseconds, or in milliseconds when it is 1000 µs or more. */
    public static String tickTime(double micros) {
        if (micros >= 1000) {
            return String.format("%.2f ms", micros / 1000);
        }
        return String.format("%.1f µs", micros);
    }
}
