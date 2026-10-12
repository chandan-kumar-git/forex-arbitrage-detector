package com.arbitrage.feed;

import com.arbitrage.engine.ArbitrageService;
import com.arbitrage.engine.TickStats;
import com.arbitrage.model.RateSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * The heartbeat of the app: once a second it fetches new rates and runs detection.
 * Spring calls tick() on its own scheduler thread, never on the Swing thread.
 */
@Component
public class TickScheduler {

    private static final Logger log = LoggerFactory.getLogger(TickScheduler.class);

    private final RateFeedManager feedManager;
    private final RateStore rateStore;
    private final ArbitrageService arbitrageService;
    private final TickStats stats;

    // volatile: written by the UI or main thread, read by the scheduler thread
    private volatile boolean paused = false;
    private volatile boolean ready = false;

    public TickScheduler(RateFeedManager feedManager, RateStore rateStore,
                         ArbitrageService arbitrageService, TickStats stats) {
        this.feedManager = feedManager;
        this.rateStore = rateStore;
        this.arbitrageService = arbitrageService;
        this.stats = stats;
    }

    @Scheduled(fixedRate = 1000)
    public void tick() {
        if (!ready || paused) {
            return;
        }
        long start = System.nanoTime();
        try {
            RateProvider provider = feedManager.current();
            RateSnapshot snapshot = provider.fetchLatest();
            if (snapshot == null) {
                return;                           // nothing new from the feed yet
            }
            rateStore.update(snapshot);
            arbitrageService.detect(snapshot, provider.source());
            stats.recordTick(System.nanoTime() - start);
        } catch (RuntimeException e) {
            // Catch everything, so one bad tick never stops the schedule.
            log.error("Tick failed", e);
        }
    }

    public boolean isPaused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }
}
