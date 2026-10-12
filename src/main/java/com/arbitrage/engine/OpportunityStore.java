package com.arbitrage.engine;

import com.arbitrage.model.Opportunity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Keeps the newest 100 opportunities in memory, newest first.
 * The scheduler thread adds to it and the UI thread reads it, so the list is thread-safe.
 */
@Component
public class OpportunityStore {

    private static final int MAX_SIZE = 100;

    private final CopyOnWriteArrayList<Opportunity> recent = new CopyOnWriteArrayList<>();

    public void add(Opportunity opportunity) {
        recent.add(0, opportunity);
        while (recent.size() > MAX_SIZE) {
            recent.remove(recent.size() - 1);
        }
    }

    /** The newest opportunity, or null when nothing was found yet. */
    public Opportunity latest() {
        List<Opportunity> copy = all();
        return copy.isEmpty() ? null : copy.get(0);
    }

    /** A copy of the list, so callers can loop over it safely. */
    public List<Opportunity> all() {
        return List.copyOf(recent);
    }
}
