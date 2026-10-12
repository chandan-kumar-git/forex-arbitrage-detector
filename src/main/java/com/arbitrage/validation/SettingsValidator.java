package com.arbitrage.validation;

/**
 * Checks the market settings and the host and port of the feed server.
 * Fee, spread and inject chance are fractions here (0.0005 means 0.05 %).
 */
public final class SettingsValidator {

    public static final double MAX_FEE = 0.05;
    public static final double MAX_SPREAD = 0.02;
    public static final double MAX_INJECT_CHANCE = 1.0;
    public static final int MIN_PORT = 1024;
    public static final int MAX_PORT = 65535;

    private SettingsValidator() {
    }

    public static void validateFee(double fee) throws ValidationException {
        checkRange("fee", "Fee must be between 0 and 5 %", fee, MAX_FEE);
    }

    public static void validateSpread(double spread) throws ValidationException {
        checkRange("spread", "Spread must be between 0 and 2 %", spread, MAX_SPREAD);
    }

    public static void validateInjectChance(double chance) throws ValidationException {
        checkRange("injectChance", "Inject chance must be between 0 and 100 %", chance, MAX_INJECT_CHANCE);
    }

    public static void validateHost(String host) throws ValidationException {
        if (host == null || host.isBlank()) {
            throw new ValidationException("host", "Host must not be empty");
        }
    }

    public static void validatePort(int port) throws ValidationException {
        if (port < MIN_PORT || port > MAX_PORT) {
            throw new ValidationException("port", "Port must be between 1024 and 65535");
        }
    }

    /** The value must be a real number from 0 up to max (NaN fails both comparisons). */
    private static void checkRange(String field, String message, double value, double max)
            throws ValidationException {
        if (!(value >= 0 && value <= max)) {
            throw new ValidationException(field, message);
        }
    }
}
