package com.arbitrage.service;

import com.arbitrage.config.AppSettings;
import com.arbitrage.db.CurrencyDao;
import com.arbitrage.db.DatabaseConnection;
import com.arbitrage.db.DatabaseException;
import com.arbitrage.db.DatabaseInitializer;
import com.arbitrage.db.SettingsDao;
import com.arbitrage.feed.TickScheduler;
import com.arbitrage.model.Currency;
import com.arbitrage.validation.SettingsValidator;
import com.arbitrage.validation.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Runs once after Spring has started: connects to MySQL, loads settings and currencies,
 * and then lets the scheduler begin ticking. If MySQL is down the app starts with defaults.
 */
@Component
public class StartupService {

    private static final Logger log = LoggerFactory.getLogger(StartupService.class);

    private final AppSettings settings;
    private final CurrencyService currencyService;
    private final TickScheduler tickScheduler;
    private final DatabaseConnection database;
    private final DatabaseInitializer initializer;
    private final SettingsDao settingsDao;
    private final CurrencyDao currencyDao;

    public StartupService(AppSettings settings, CurrencyService currencyService, TickScheduler tickScheduler,
                          DatabaseConnection database, DatabaseInitializer initializer,
                          SettingsDao settingsDao, CurrencyDao currencyDao) {
        this.settings = settings;
        this.currencyService = currencyService;
        this.tickScheduler = tickScheduler;
        this.database = database;
        this.initializer = initializer;
        this.settingsDao = settingsDao;
        this.currencyDao = currencyDao;
    }

    public void start() {
        try {
            long millis = connectDatabase();
            log.info("Database connected in {} ms", millis);
        } catch (DatabaseException e) {
            // No stack trace for the user: the app simply runs in memory with the defaults.
            log.warn("Database offline, using default currencies and settings. {}", e.getMessage());
            currencyService.load(Currency.defaults());
        }
        tickScheduler.setReady(true);
    }

    /**
     * Connects, creates the tables, seeds them and loads the saved data.
     * Also used by the Retry buttons. Returns the connect time in ms; throws DatabaseException.
     */
    public long connectDatabase() {
        long millis = database.test();
        initializer.createTables();
        initializer.seedIfEmpty();
        applySettings(settingsDao.loadAll());
        currencyService.load(currencyDao.findAll());
        return millis;
    }

    /** Uses the saved values; a missing or broken value keeps its default. */
    private void applySettings(Map<String, String> saved) {
        try {
            double fee = Double.parseDouble(saved.getOrDefault(SettingsDao.KEY_FEE, "" + AppSettings.DEFAULT_FEE));
            double spread = Double.parseDouble(
                    saved.getOrDefault(SettingsDao.KEY_SPREAD, "" + AppSettings.DEFAULT_SPREAD));
            double injectChance = Double.parseDouble(
                    saved.getOrDefault(SettingsDao.KEY_INJECT_CHANCE, "" + AppSettings.DEFAULT_INJECT_CHANCE));
            SettingsValidator.validateFee(fee);
            SettingsValidator.validateSpread(spread);
            SettingsValidator.validateInjectChance(injectChance);
            settings.setFee(fee);
            settings.setSpread(spread);
            settings.setInjectChance(injectChance);
        } catch (NumberFormatException | ValidationException e) {
            log.warn("Saved settings are not valid, keeping the current ones. {}", e.getMessage());
        }
    }
}
