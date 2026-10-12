package com.arbitrage.service;

import com.arbitrage.db.CurrencyDao;
import com.arbitrage.db.DatabaseConnection;
import com.arbitrage.feed.RateFeedManager;
import com.arbitrage.model.Currency;
import com.arbitrage.validation.CurrencyValidator;
import com.arbitrage.validation.ValidationException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The one entry point for adding, changing and deleting currencies.
 * It validates the input, saves to MySQL (when online), keeps the list in memory
 * and tells the simulator about changes. Screens call it from a background thread.
 */
@Component
public class CurrencyService {

    private final RateFeedManager feedManager;
    private final CurrencyDao currencyDao;
    private final DatabaseConnection database;
    private final List<Currency> currencies = new ArrayList<>();
    private final Map<String, Integer> indexByCode = new HashMap<>();   // code -> position in the list

    public CurrencyService(RateFeedManager feedManager, CurrencyDao currencyDao, DatabaseConnection database) {
        this.feedManager = feedManager;
        this.currencyDao = currencyDao;
        this.database = database;
    }

    /** Replaces the whole list (used at startup and after a reconnect). */
    public synchronized void load(List<Currency> newList) {
        currencies.clear();
        currencies.addAll(newList);
        listChanged();
    }

    public synchronized List<Currency> all() {
        return List.copyOf(currencies);
    }

    public void add(String code, String name, String priceText, boolean active) throws ValidationException {
        Currency currency = CurrencyValidator.validate(code, name, priceText, active);
        CurrencyValidator.checkNotDuplicate(currency.code(), codes());
        if (database.isOnline()) {
            if (currencyDao.exists(currency.code())) {   // added by another desktop in the meantime
                throw new ValidationException("code", "Code already exists");
            }
            currencyDao.insert(currency);
        }
        put(currency);
    }

    public void update(String code, String name, String priceText, boolean active) throws ValidationException {
        Currency currency = CurrencyValidator.validate(code, name, priceText, active);
        if (!codes().contains(currency.code())) {
            throw new ValidationException("code", "Select a currency in the table first");
        }
        if (database.isOnline()) {
            currencyDao.update(currency);
        }
        put(currency);
    }

    public void delete(String code) throws ValidationException {
        CurrencyValidator.checkCanDelete(code);
        if (database.isOnline()) {
            currencyDao.delete(code);             // fails with a friendly message if used in history
        }
        remove(code);
    }

    /** A HashSet of all codes: checking "is this code taken?" is one fast lookup. */
    private synchronized Set<String> codes() {
        return new HashSet<>(indexByCode.keySet());
    }

    /** Adds the currency, or replaces the one with the same code. */
    private synchronized void put(Currency currency) {
        Integer index = indexByCode.get(currency.code());
        if (index == null) {
            currencies.add(currency);
        } else {
            currencies.set(index, currency);
        }
        listChanged();
    }

    private synchronized void remove(String code) {
        Integer index = indexByCode.get(code);
        if (index != null) {
            currencies.remove(index.intValue());
            listChanged();
        }
    }

    /** Rebuilds the code -> index map and gives the new list to the simulator. */
    private void listChanged() {
        indexByCode.clear();
        for (int i = 0; i < currencies.size(); i++) {
            indexByCode.put(currencies.get(i).code(), i);
        }
        feedManager.simulator().setCurrencies(currencies);
    }
}
