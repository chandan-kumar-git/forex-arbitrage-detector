package com.arbitrage.model;

import java.util.List;

/**
 * All exchange rates at one moment. rates[i][j] means:
 * how many units of currency j you get for 1 unit of currency i.
 */
public record RateSnapshot(List<String> codes, double[][] rates, long timestamp) {

    /** Copies the list and the matrix, so nobody can change a snapshot later. */
    public RateSnapshot {
        codes = List.copyOf(codes);
        rates = copyOf(rates);
    }

    /** Returns a copy, never the array kept inside the record. */
    @Override
    public double[][] rates() {
        return copyOf(rates);
    }

    /** Number of currencies in this snapshot. */
    public int size() {
        return codes.size();
    }

    public double rate(int from, int to) {
        return rates[from][to];
    }

    public String code(int index) {
        return codes.get(index);
    }

    private static double[][] copyOf(double[][] matrix) {
        double[][] copy = new double[matrix.length][];
        for (int i = 0; i < matrix.length; i++) {
            copy[i] = matrix[i].clone();
        }
        return copy;
    }
}
