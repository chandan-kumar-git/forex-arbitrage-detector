package com.arbitrage.api;

import com.arbitrage.config.AppSettings;
import com.arbitrage.db.DatabaseConnection;
import com.arbitrage.engine.OpportunityStore;
import com.arbitrage.engine.TickStats;
import com.arbitrage.feed.RateFeedManager;
import com.arbitrage.feed.RateStore;
import com.arbitrage.feed.TickScheduler;
import com.arbitrage.model.Opportunity;
import com.arbitrage.model.RateSnapshot;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Writes the engine's current state as one JSON object for the web frontend:
 * the latest rates, the stats, the settings and the newest opportunities.
 */
@Component
public class StateWriter {

    private static final int MAX_RECENT = 20;

    private final RateStore rateStore;
    private final OpportunityStore opportunities;
    private final TickStats stats;
    private final TickScheduler scheduler;
    private final RateFeedManager feedManager;
    private final AppSettings settings;
    private final DatabaseConnection database;

    public StateWriter(RateStore rateStore, OpportunityStore opportunities, TickStats stats, TickScheduler scheduler,
                       RateFeedManager feedManager, AppSettings settings, DatabaseConnection database) {
        this.rateStore = rateStore;
        this.opportunities = opportunities;
        this.stats = stats;
        this.scheduler = scheduler;
        this.feedManager = feedManager;
        this.settings = settings;
        this.database = database;
    }

    public String stateJson() {
        return "{\"engine\":\"java\""
                + ",\"source\":" + Json.string(feedManager.mode().name())
                + ",\"paused\":" + scheduler.isPaused()
                + ",\"database\":" + database.isOnline()
                + ",\"fee\":" + Json.number(settings.getFee())
                + ",\"spread\":" + Json.number(settings.getSpread())
                + ",\"injectChance\":" + Json.number(settings.getInjectChance())
                + ",\"snapshot\":" + snapshotJson(rateStore.getLatest())
                + ",\"stats\":" + statsJson()
                + ",\"recent\":" + recentJson(opportunities.all())
                + "}";
    }

    private String snapshotJson(RateSnapshot snapshot) {
        if (snapshot == null) {
            return "null";                        // before the first tick
        }
        return "{\"timestamp\":" + snapshot.timestamp()
                + ",\"codes\":" + Json.strings(snapshot.codes())
                + ",\"rates\":" + Json.matrix(snapshot.rates()) + "}";
    }

    private String statsJson() {
        return "{\"ticks\":" + stats.ticks()
                + ",\"found\":" + stats.found()
                + ",\"bestProfit\":" + Json.number(stats.bestProfit())
                + ",\"bestPath\":" + Json.string(stats.bestPath())
                + ",\"avgTickMicros\":" + Json.number(stats.averageMicros()) + "}";
    }

    private String recentJson(List<Opportunity> list) {
        StringBuilder out = new StringBuilder("[");
        for (int i = 0; i < Math.min(list.size(), MAX_RECENT); i++) {
            Opportunity o = list.get(i);
            if (i > 0) {
                out.append(',');
            }
            out.append("{\"id\":").append(o.id())
                    .append(",\"path\":").append(Json.strings(o.path()))
                    .append(",\"profitPercent\":").append(Json.number(o.profitPercent()))
                    .append(",\"fee\":").append(Json.number(o.fee()))
                    .append(",\"source\":").append(Json.string(o.source().name()))
                    .append(",\"detectedAt\":").append(Json.string(o.detectedAt().toString()))
                    .append('}');
        }
        return out.append(']').toString();
    }
}
