package com.arbitrage.engine;

import com.arbitrage.TestSnapshots;
import com.arbitrage.model.OpportunityLeg;
import com.arbitrage.model.RateSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Checks the profit formula against the reference case of the spec. */
class ProfitCalculatorTest {

    private final ProfitCalculator calculator = new ProfitCalculator();
    private final List<Integer> cycle =
            List.of(TestSnapshots.INR, TestSnapshots.EUR, TestSnapshots.GBP, TestSnapshots.INR);

    @Test
    void referenceCycleGivesKnownProfit() {
        RateSnapshot snapshot = TestSnapshots.reference();

        double profit = calculator.profitPercent(cycle, snapshot, TestSnapshots.FEE);

        assertEquals(0.3472, profit, 0.0005);
    }

    @Test
    void fairMarketLosesTheFees() {
        RateSnapshot snapshot = TestSnapshots.fair();

        assertTrue(calculator.profitPercent(cycle, snapshot, TestSnapshots.FEE) < 0);
    }

    @Test
    void legsFollowTheCycle() {
        RateSnapshot snapshot = TestSnapshots.reference();

        List<OpportunityLeg> legs = calculator.legs(cycle, snapshot);

        assertEquals(3, legs.size());
        assertEquals(new OpportunityLeg(1, "INR", "EUR", snapshot.rate(TestSnapshots.INR, TestSnapshots.EUR)),
                legs.get(0));
        assertEquals("GBP", legs.get(2).fromCode());
        assertEquals("INR", legs.get(2).toCode());
    }
}
