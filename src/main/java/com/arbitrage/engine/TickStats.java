package com.arbitrage.engine;

import com.arbitrage.model.Opportunity;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The numbers shown on the dashboard stat cards: ticks, opportunities found,
 * best profit, and the average time of the last 60 ticks.
 */
@Component
public class TickStats {

    private static final int WINDOW = 60;

    private final AtomicLong ticks = new AtomicLong();
    private final AtomicLong found = new AtomicLong();
    private final ArrayDeque<Long> durations = new ArrayDeque<>();
    private double bestProfit = 0.0;
    private String bestPath = "";

    /** Called by the scheduler after every tick with the time the tick took. */
    public synchronized void recordTick(long nanos) {
        ticks.incrementAndGet();
        durations.addLast(nanos);
        if (durations.size() > WINDOW) {
            durations.removeFirst();          // drop the oldest: the queue is a rolling window
        }
    }

    public synchronized void recordOpportunity(Opportunity opportunity) {
        found.incrementAndGet();
        if (opportunity.profitPercent() > bestProfit) {
            bestProfit = opportunity.profitPercent();
            bestPath = opportunity.pathText();
        }
    }

    /** Average tick time in microseconds over the rolling window. */
    public synchronized double averageMicros() {
        if (durations.isEmpty()) {
            return 0.0;
        }
        long total = 0;
        for (long nanos : durations) {
            total += nanos;
        }
        return total / 1000.0 / durations.size();
    }

    public long ticks() {
        return ticks.get();
    }

    public long found() {
        return found.get();
    }

    public synchronized double bestProfit() {
        return bestProfit;
    }

    public synchronized String bestPath() {
        return bestPath;
    }
}
