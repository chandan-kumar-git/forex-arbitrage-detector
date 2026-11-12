package com.arbitrage.cap7.service;

import com.arbitrage.cap7.dto.Dtos.SettingsDto;
import com.arbitrage.cap7.dto.Dtos.SettingsRequest;
import com.arbitrage.config.AppSettings;
import com.arbitrage.db.DatabaseConnection;
import com.arbitrage.db.SettingsDao;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

/** Reads and changes the live market settings (fee, spread, inject chance) and saves them in MySQL. */
@Service
public class EngineSettingsService {

    private final AppSettings settings;
    private final SettingsDao settingsDao;
    private final DatabaseConnection database;

    public EngineSettingsService(AppSettings settings, SettingsDao settingsDao, DatabaseConnection database) {
        this.settings = settings;
        this.settingsDao = settingsDao;
        this.database = database;
    }

    public SettingsDto current() {
        return new SettingsDto(settings.getFee(), settings.getSpread(), settings.getInjectChance());
    }

    /** Applies at once (next tick); saved too when MySQL is online. Ranges are checked by @Valid first. */
    public SettingsDto update(SettingsRequest request) {
        settings.setFee(request.fee());
        settings.setSpread(request.spread());
        settings.setInjectChance(request.injectChance());
        if (database.isOnline()) {
            settingsDao.saveAll(Map.of(
                    SettingsDao.KEY_FEE, plain(request.fee()),
                    SettingsDao.KEY_SPREAD, plain(request.spread()),
                    SettingsDao.KEY_INJECT_CHANCE, plain(request.injectChance())));
        }
        return current();
    }

    private static String plain(double value) {
        return BigDecimal.valueOf(value).toPlainString();
    }
}
