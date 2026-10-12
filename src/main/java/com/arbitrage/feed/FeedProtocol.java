package com.arbitrage.feed;

import com.arbitrage.model.RateSnapshot;

import java.util.List;
import java.util.Locale;

/**
 * The text protocol between the Rate Feed Server and the desktop app.
 * One message per line, fields separated by one space. Both programs use this class.
 */
public final class FeedProtocol {

    public static final int DEFAULT_PORT = 5050;
    public static final String SERVER_NAME = "RateFeedServer";
    public static final String HELLO = "HELLO";
    public static final String WELCOME = "WELCOME";
    public static final String SNAPSHOT = "SNAPSHOT";
    public static final String PING = "PING";
    public static final String PONG = "PONG";
    public static final String BYE = "BYE";
    public static final String ERROR = "ERROR";

    private static final int MIN_CURRENCIES = 3;
    private static final int MAX_CURRENCIES = 20;
    private static final String CODE_PATTERN = "^[A-Z]{3}$";

    private FeedProtocol() {
    }

    public static String hello(String clientName) {
        return HELLO + " " + clientName;
    }

    public static String welcome(String serverName, int intervalMs) {
        return WELCOME + " " + serverName + " " + intervalMs;
    }

    /** Builds "SNAPSHOT <epochMillis> <codes> <rates>" with the matrix written row by row. */
    public static String encode(RateSnapshot snapshot) {
        StringBuilder rates = new StringBuilder();
        for (int i = 0; i < snapshot.size(); i++) {
            for (int j = 0; j < snapshot.size(); j++) {
                if (rates.length() > 0) {
                    rates.append(',');
                }
                // Locale.ROOT: always a dot as decimal separator, on every computer
                rates.append(String.format(Locale.ROOT, "%.8f", snapshot.rate(i, j)));
            }
        }
        return SNAPSHOT + " " + snapshot.timestamp() + " " + String.join(",", snapshot.codes()) + " " + rates;
    }

    /** Turns a SNAPSHOT line back into a RateSnapshot, checking every rule of the protocol. */
    public static RateSnapshot decode(String line) throws FeedException {
        String[] parts = line.split(" ");
        if (parts.length != 4) {
            throw bad("expected 4 parts but got " + parts.length);
        }
        if (!parts[0].equals(SNAPSHOT)) {
            throw bad("expected SNAPSHOT but got " + parts[0]);
        }
        long timestamp;
        try {
            timestamp = Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            throw bad("timestamp is not a number");
        }
        List<String> codes = parseCodes(parts[2]);
        double[][] rates = parseRates(parts[3], codes.size());
        return new RateSnapshot(codes, rates, timestamp);
    }

    private static List<String> parseCodes(String text) throws FeedException {
        List<String> codes = List.of(text.split(","));
        if (codes.size() < MIN_CURRENCIES || codes.size() > MAX_CURRENCIES) {
            throw bad("needs 3 to 20 currencies but has " + codes.size());
        }
        for (String code : codes) {
            if (!code.matches(CODE_PATTERN)) {
                throw bad("currency code " + code + " is not 3 uppercase letters");
            }
        }
        return codes;
    }

    private static double[][] parseRates(String text, int n) throws FeedException {
        String[] numbers = text.split(",");
        if (numbers.length != n * n) {
            throw bad("expected " + (n * n) + " rates but got " + numbers.length);
        }
        double[][] rates = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                rates[i][j] = parseRate(numbers[i * n + j]);
            }
            if (Math.abs(rates[i][i] - 1.0) > 1e-9) {
                throw bad("the rate of " + i + " to itself must be 1");
            }
        }
        return rates;
    }

    private static double parseRate(String text) throws FeedException {
        double rate;
        try {
            rate = Double.parseDouble(text);
        } catch (NumberFormatException e) {
            throw bad("rate " + text + " is not a number");
        }
        if (Double.isNaN(rate) || Double.isInfinite(rate) || rate <= 0) {
            throw bad("rate " + text + " must be more than 0");
        }
        return rate;
    }

    private static FeedException bad(String reason) {
        return new FeedException("Bad snapshot: " + reason, null);
    }
}
