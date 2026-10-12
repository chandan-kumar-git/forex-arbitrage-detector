package com.arbitrage.feed;

import com.arbitrage.config.AppSettings;
import com.arbitrage.model.Currency;
import com.arbitrage.model.FeedSource;
import com.arbitrage.model.RateSnapshot;
import com.arbitrage.validation.CurrencyValidator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A pretend forex market. Prices move a little on every call, and sometimes one
 * rate is made slightly wrong on purpose so that an arbitrage loop appears.
 * It is a plain class (no Spring) because the feed server uses it too.
 */
public class SimulatedRateProvider implements RateProvider {

    private static final double VOLATILITY = 0.0005;
    private static final double MIN_ERROR = 0.006;
    private static final double MAX_ERROR = 0.012;

    private final AppSettings settings;
    private final Random random = new Random();
    private List<String> codes = List.of();
    private double[] prices = new double[0];

    public SimulatedRateProvider(List<Currency> currencies, AppSettings settings) {
        this.settings = settings;
        setCurrencies(currencies);
    }

    /** Replaces the currency list. Only the active currencies are used. */
    public synchronized void setCurrencies(List<Currency> currencies) {
        List<String> newCodes = new ArrayList<>();
        List<Double> newPrices = new ArrayList<>();
        for (Currency currency : currencies) {
            if (currency.active()) {
                newCodes.add(currency.code());
                newPrices.add(currency.usdPrice());
            }
        }
        codes = newCodes;
        prices = new double[newPrices.size()];
        for (int i = 0; i < prices.length; i++) {
            prices[i] = newPrices.get(i);
        }
    }

    @Override
    public synchronized RateSnapshot fetchLatest() {
        movePrices();
        double[][] rates = buildMatrix();
        if (codes.size() >= 2 && random.nextDouble() < settings.getInjectChance()) {
            injectError(rates);
        }
        return new RateSnapshot(codes, rates, System.currentTimeMillis());
    }

    @Override
    public FeedSource source() {
        return FeedSource.LOCAL;
    }

    /** Random walk: every price moves by a tiny random percent. USD stays at 1. */
    private void movePrices() {
        for (int i = 0; i < prices.length; i++) {
            if (!codes.get(i).equals(CurrencyValidator.BASE_CODE)) {
                prices[i] = prices[i] * (1 + random.nextGaussian() * VOLATILITY);
            }
        }
    }

    /** rate i to j = price of i / price of j, made a bit worse by the spread. */
    private double[][] buildMatrix() {
        int n = prices.length;
        double spread = settings.getSpread();
        double[][] rates = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                rates[i][j] = (i == j) ? 1.0 : prices[i] / prices[j] * (1 - spread);
            }
        }
        return rates;
    }

    /** Makes one random pair 0.6 % to 1.2 % too good. The reverse rate gets worse by the same factor. */
    private void injectError(double[][] rates) {
        int from = random.nextInt(codes.size());
        int to = random.nextInt(codes.size() - 1);
        if (to >= from) {
            to++;                                 // skip "from" so the two currencies differ
        }
        double factor = 1 + MIN_ERROR + random.nextDouble() * (MAX_ERROR - MIN_ERROR);
        rates[from][to] = rates[from][to] * factor;
        rates[to][from] = rates[to][from] / factor;
    }
}
