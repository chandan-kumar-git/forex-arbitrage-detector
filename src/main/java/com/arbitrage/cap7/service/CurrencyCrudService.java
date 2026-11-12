package com.arbitrage.cap7.service;

import com.arbitrage.cap7.dto.Dtos.CurrencyDto;
import com.arbitrage.cap7.dto.Dtos.CurrencyRequest;
import com.arbitrage.cap7.entity.CurrencyEntity;
import com.arbitrage.cap7.repository.CurrencyRepository;
import com.arbitrage.model.Currency;
import com.arbitrage.service.CurrencyService;
import com.arbitrage.validation.CurrencyValidator;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Currency CRUD through JPA. After every change the engine's in-memory list is reloaded,
 * so the live market (Swing app, portal and web desk) uses the new currencies from the next tick.
 */
@Service
public class CurrencyCrudService {

    private final CurrencyRepository repository;
    private final CurrencyService engineCurrencies;

    public CurrencyCrudService(CurrencyRepository repository, CurrencyService engineCurrencies) {
        this.repository = repository;
        this.engineCurrencies = engineCurrencies;
    }

    @Transactional(readOnly = true)
    public List<CurrencyDto> list() {
        return repository.findAllByOrderByCodeAsc().stream().map(CurrencyCrudService::toDto).toList();
    }

    @Transactional(readOnly = true)
    public CurrencyDto get(String code) {
        return repository.findById(code).map(CurrencyCrudService::toDto)
                .orElseThrow(() -> new NotFoundException("Currency " + code + " does not exist"));
    }

    @Transactional
    public CurrencyDto create(CurrencyRequest request) {
        if (repository.existsById(request.code())) {
            throw new ConflictException("Currency " + request.code() + " already exists");
        }
        boolean active = request.active() == null || request.active();
        CurrencyEntity saved = repository.save(new CurrencyEntity(request.code(), request.name().trim(),
                request.usdPrice(), active));
        syncEngine();
        return toDto(saved);
    }

    /** No save() call needed: changing a managed entity is enough (Hibernate dirty checking). */
    @Transactional
    public CurrencyDto update(String code, CurrencyRequest request) {
        CurrencyEntity entity = repository.findById(code)
                .orElseThrow(() -> new NotFoundException("Currency " + code + " does not exist"));
        boolean active = request.active() == null || request.active();
        if (CurrencyValidator.BASE_CODE.equals(code) && !active) {
            throw new ConflictException("USD is the base currency and cannot be deactivated");
        }
        entity.update(request.name().trim(), request.usdPrice(), active);
        repository.flush();
        syncEngine();
        return toDto(entity);
    }

    @Transactional
    public void delete(String code) {
        if (CurrencyValidator.BASE_CODE.equals(code)) {
            throw new ConflictException("USD is the base currency and cannot be deleted");
        }
        if (!repository.existsById(code)) {
            throw new NotFoundException("Currency " + code + " does not exist");
        }
        try {
            repository.deleteById(code);
            repository.flush();                   // run the DELETE now, so a foreign-key error happens here
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Used in history – deactivate it instead");
        }
        syncEngine();
    }

    private void syncEngine() {
        List<Currency> all = repository.findAllByOrderByCodeAsc().stream()
                .map(c -> new Currency(c.getCode(), c.getName(), c.getUsdPrice(), c.isActive()))
                .toList();
        engineCurrencies.load(all);
    }

    private static CurrencyDto toDto(CurrencyEntity c) {
        return new CurrencyDto(c.getCode(), c.getName(), c.getUsdPrice(), c.isActive());
    }
}
