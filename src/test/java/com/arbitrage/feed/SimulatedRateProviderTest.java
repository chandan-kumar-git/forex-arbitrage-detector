package com.arbitrage.feed;

import com.arbitrage.config.AppSettings;
import com.arbitrage.model.Currency;
import com.arbitrage.model.RateSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Checks that the simulated market always produces sensible rate matrices. */
class SimulatedRateProviderTest {

    private static final int ROUNDS = 200;

    private final SimulatedRateProvider provider =
            new SimulatedRateProvider(Currency.defaults(), new AppSettings());

    @Test
    void diagonalIsAlwaysOne() {
        for (int round = 0; round < ROUNDS; round++) {
            RateSnapshot snapshot = provider.fetchLatest();
            for (int i = 0; i < snapshot.size(); i++) {
                assertEquals(1.0, snapshot.rate(i, i), 1e-12);
            }
        }
    }

    @Test
    void goingThereAndBackNeverWins() {
        for (int round = 0; round < ROUNDS; round++) {
            RateSnapshot snapshot = provider.fetchLatest();
            for (int i = 0; i < snapshot.size(); i++) {
                for (int j = i + 1; j < snapshot.size(); j++) {
                    assertTrue(snapshot.rate(i, j) * snapshot.rate(j, i) < 1.0, "no 2-cycle arbitrage");
                }
            }
        }
    }

    @Test
    void onlyActiveCurrenciesAreUsed() {
        provider.setCurrencies(List.of(
                new Currency("USD", "US Dollar", 1.0, true),
                new Currency("EUR", "Euro", 1.08, true),
                new Currency("CHF", "Swiss Franc", 1.12, false)));

        RateSnapshot snapshot = provider.fetchLatest();

        assertEquals(List.of("USD", "EUR"), snapshot.codes());
    }
}
