package com.arbitrage.feed;

import com.arbitrage.model.RateSnapshot;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Holds the newest rate snapshot. The scheduler thread writes it
 * and the UI thread reads it, so it uses an AtomicReference.
 */
@Component
public class RateStore {

    private final AtomicReference<RateSnapshot> latest = new AtomicReference<>();

    public void update(RateSnapshot snapshot) {
        latest.set(snapshot);
    }

    /** The newest snapshot, or null before the first tick. */
    public RateSnapshot getLatest() {
        return latest.get();
    }
}
