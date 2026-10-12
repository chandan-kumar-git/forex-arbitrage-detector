package com.arbitrage.feed;

import com.arbitrage.model.FeedSource;
import com.arbitrage.model.RateSnapshot;

/**
 * Anything that can give us exchange rates. The engine only knows this interface,
 * so the simulator and the socket client can be swapped without changing the engine.
 */
public interface RateProvider {

    /** Latest snapshot, or null if none is available yet. */
    RateSnapshot fetchLatest();

    FeedSource source();
}
