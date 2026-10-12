package com.arbitrage.ui;

import com.arbitrage.config.AppSettings;
import com.arbitrage.db.DatabaseConnection;
import com.arbitrage.db.OpportunityDao;
import com.arbitrage.db.SettingsDao;
import com.arbitrage.engine.OpportunityStore;
import com.arbitrage.engine.TickStats;
import com.arbitrage.feed.RateFeedManager;
import com.arbitrage.feed.RateStore;
import com.arbitrage.feed.TickScheduler;
import com.arbitrage.service.CurrencyService;
import com.arbitrage.service.StartupService;
import org.springframework.stereotype.Component;

/**
 * A simple bundle of the Spring objects the screens need.
 * Passing this one object around avoids very long constructor lists in the UI.
 */
@Component
public class UiServices {

    private final AppSettings settings;
    private final RateFeedManager feedManager;
    private final RateStore rateStore;
    private final OpportunityStore opportunityStore;
    private final TickStats stats;
    private final TickScheduler scheduler;
    private final CurrencyService currencyService;
    private final OpportunityDao opportunityDao;
    private final SettingsDao settingsDao;
    private final DatabaseConnection database;
    private final StartupService startupService;

    public UiServices(AppSettings settings, RateFeedManager feedManager, RateStore rateStore,
                      OpportunityStore opportunityStore, TickStats stats, TickScheduler scheduler,
                      CurrencyService currencyService, OpportunityDao opportunityDao, SettingsDao settingsDao,
                      DatabaseConnection database, StartupService startupService) {
        this.settings = settings;
        this.feedManager = feedManager;
        this.rateStore = rateStore;
        this.opportunityStore = opportunityStore;
        this.stats = stats;
        this.scheduler = scheduler;
        this.currencyService = currencyService;
        this.opportunityDao = opportunityDao;
        this.settingsDao = settingsDao;
        this.database = database;
        this.startupService = startupService;
    }

    public AppSettings settings() {
        return settings;
    }

    public RateFeedManager feed() {
        return feedManager;
    }

    public RateStore rates() {
        return rateStore;
    }

    public OpportunityStore opportunities() {
        return opportunityStore;
    }

    public TickStats stats() {
        return stats;
    }

    public TickScheduler scheduler() {
        return scheduler;
    }

    public CurrencyService currencies() {
        return currencyService;
    }

    public OpportunityDao history() {
        return opportunityDao;
    }

    public SettingsDao settingsDao() {
        return settingsDao;
    }

    public DatabaseConnection database() {
        return database;
    }

    public StartupService startup() {
        return startupService;
    }
}
