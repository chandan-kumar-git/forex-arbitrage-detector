package com.arbitrage.engine;

import com.arbitrage.model.OpportunityLeg;
import com.arbitrage.model.RateSnapshot;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Works out the profit of a cycle and lists its conversion steps (legs).
 * A cycle is a list of currency indexes whose first and last entries are equal.
 */
@Component
public class ProfitCalculator {

    /** Profit in percent: (product of every rate after fee - 1) x 100. */
    public double profitPercent(List<Integer> cycle, RateSnapshot snapshot, double fee) {
        double money = 1.0;
        for (int i = 0; i < cycle.size() - 1; i++) {
            double rate = snapshot.rate(cycle.get(i), cycle.get(i + 1));
            money = money * rate * (1 - fee);
        }
        return (money - 1.0) * 100.0;
    }

    /** One leg for every step of the cycle, numbered from 1. */
    public List<OpportunityLeg> legs(List<Integer> cycle, RateSnapshot snapshot) {
        List<OpportunityLeg> legs = new ArrayList<>();
        for (int i = 0; i < cycle.size() - 1; i++) {
            int from = cycle.get(i);
            int to = cycle.get(i + 1);
            legs.add(new OpportunityLeg(i + 1, snapshot.code(from), snapshot.code(to), snapshot.rate(from, to)));
        }
        return legs;
    }
}
