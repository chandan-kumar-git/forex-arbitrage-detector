package com.arbitrage.cap7.service;

import com.arbitrage.feed.RateStore;
import com.arbitrage.model.RateSnapshot;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Real-life use: "I must send 50,000 INR to EUR. What is the cheapest way?"
 * Banks quote rare pairs (INR to JPY) with a wide spread, so changing through a common currency
 * (usually USD) can give more money. This finds the best route with Bellman-Ford on -log(rate)
 * (the same graph idea as the arbitrage detector, used as a shortest-path search).
 */
@Service
public class RouteService {

    /** The best route and how it compares with changing directly. */
    public record Route(String from, String to, double amount, List<String> path, double received,
                        double directReceived, double saving, List<Double> stepRates) {
    }

    /** Currencies traded so much that banks quote them with a tight spread. */
    private static final Set<String> MAJORS = Set.of("USD", "EUR", "GBP", "JPY", "CHF");
    private static final double MAJOR_SPREAD = 0.0005;     // 0.05 %
    private static final double MINOR_SPREAD = 0.004;      // 0.40 %
    private static final double CROSS_PENALTY = 0.008;     // 0.80 % extra for a pair without USD
    private static final double TRANSFER_FEE = 0.001;      // 0.10 % per conversion

    private final RateStore rates;

    public RouteService(RateStore rates) {
        this.rates = rates;
    }

    /** The bank's spread for a pair: wider for rare currencies, widest for crosses without USD. */
    static double bankSpread(String from, String to) {
        double spread = spreadOf(from) + spreadOf(to);
        if (!from.equals("USD") && !to.equals("USD")) {
            spread += CROSS_PENALTY;
        }
        return spread;
    }

    private static double spreadOf(String code) {
        return MAJORS.contains(code) ? MAJOR_SPREAD : MINOR_SPREAD;
    }

    public Route plan(String from, String to, double amount) {
        RateSnapshot snapshot = rates.getLatest();
        if (snapshot == null) {
            throw new NotFoundException("The market has not started yet; try again in a second");
        }
        List<String> codes = snapshot.codes();
        int src = codes.indexOf(from);
        int dst = codes.indexOf(to);
        if (src < 0 || dst < 0) {
            throw new NotFoundException("Unknown or inactive currency: " + (src < 0 ? from : to));
        }
        if (src == dst) {
            throw new ConflictException("Choose two different currencies");
        }
        int n = codes.size();
        // each currency's mid value in USD (the geometric mean of buy and sell, so the spread drops out);
        // pricing every pair from these values keeps one odd quote from changing the answer
        int usd = codes.indexOf("USD");
        double[] value = new double[n];
        for (int k = 0; k < n; k++) {
            value[k] = usd < 0 || k == usd ? 1 : Math.sqrt(snapshot.rate(k, usd) / snapshot.rate(usd, k));
        }
        double[][] bank = new double[n][n];       // what a bank really pays: mid rate minus spread and fee
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                double mid = value[i] / value[j];
                bank[i][j] = i == j ? 1 : mid * (1 - bankSpread(codes.get(i), codes.get(j))) * (1 - TRANSFER_FEE);
            }
        }
        List<Integer> path = bestPath(bank, src, dst);
        double money = amount;
        List<String> names = new ArrayList<>();
        List<Double> stepRates = new ArrayList<>();
        for (int k = 0; k < path.size(); k++) {
            names.add(codes.get(path.get(k)));
            if (k > 0) {
                double r = bank[path.get(k - 1)][path.get(k)];
                stepRates.add(r);
                money *= r;
            }
        }
        double direct = amount * bank[src][dst];
        return new Route(from, to, amount, names, money, direct, money - direct, stepRates);
    }

    /** Shortest path on weights -log(rate): the smallest sum of -log is the largest product of rates. */
    static List<Integer> bestPath(double[][] rate, int src, int dst) {
        int n = rate.length;
        double[] dist = new double[n];
        int[] parent = new int[n];
        java.util.Arrays.fill(dist, Double.POSITIVE_INFINITY);
        java.util.Arrays.fill(parent, -1);
        dist[src] = 0;
        for (int round = 1; round < n; round++) {   // n - 1 rounds are enough for a simple path
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if (i != j && dist[i] + -Math.log(rate[i][j]) < dist[j] - 1e-12) {
                        dist[j] = dist[i] - Math.log(rate[i][j]);
                        parent[j] = i;
                    }
                }
            }
        }
        List<Integer> path = new ArrayList<>();
        boolean[] seen = new boolean[n];
        for (int v = dst; v != -1 && !seen[v]; v = parent[v]) {   // seen[] stops at a loop, just in case
            seen[v] = true;
            path.add(v);
            if (v == src) {
                break;
            }
        }
        if (path.get(path.size() - 1) != src) {
            return List.of(src, dst);              // no better path found: go direct
        }
        Collections.reverse(path);
        return path;
    }
}
