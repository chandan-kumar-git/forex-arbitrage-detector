package com.arbitrage;

import com.arbitrage.model.RateSnapshot;

import java.util.List;

/**
 * Hand-made rate snapshots shared by the unit tests.
 * Currency order: USD = 0, EUR = 1, GBP = 2, INR = 3.
 */
public final class TestSnapshots {

    public static final double FEE = 0.0005;
    public static final double SPREAD = 0.001;
    public static final int USD = 0;
    public static final int EUR = 1;
    public static final int GBP = 2;
    public static final int INR = 3;

    private static final List<String> CODES = List.of("USD", "EUR", "GBP", "INR");
    private static final double[] PRICES = {1.0, 1.08, 1.27, 0.012};

    private TestSnapshots() {
    }

    /** The reference case of the spec: EUR to GBP is 0.8 % too good. */
    public static RateSnapshot reference() {
        double[][] rates = matrix(SPREAD);
        rates[EUR][GBP] = rates[EUR][GBP] * 1.008;
        rates[GBP][EUR] = rates[GBP][EUR] / 1.008;
        return new RateSnapshot(CODES, rates, 0L);
    }

    /** A perfectly consistent market: no spread and no error, so no loop can win. */
    public static RateSnapshot fair() {
        return new RateSnapshot(CODES, matrix(0.0), 0L);
    }

    private static double[][] matrix(double spread) {
        int n = PRICES.length;
        double[][] rates = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                rates[i][j] = (i == j) ? 1.0 : PRICES[i] / PRICES[j] * (1 - spread);
            }
        }
        return rates;
    }
}
