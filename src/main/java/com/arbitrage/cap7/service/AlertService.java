package com.arbitrage.cap7.service;

import com.arbitrage.cap7.dto.Dtos.AlertDto;
import com.arbitrage.cap7.dto.Dtos.AlertRequest;
import com.arbitrage.cap7.entity.ProfitAlert;
import com.arbitrage.cap7.repository.ProfitAlertRepository;
import com.arbitrage.db.DatabaseConnection;
import com.arbitrage.engine.OpportunityStore;
import com.arbitrage.model.Opportunity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Profit alerts: CRUD through JPA, and a scheduled check that compares every new opportunity
 * with the active alerts. Fired alerts are counted in MySQL and kept in memory for the portal.
 */
@Service
public class AlertService {

    /** One fired alert, shown on the portal dashboard. */
    public record Hit(long alertId, String owner, String pathText, double profitPercent, LocalDateTime at) {
    }

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);
    private static final int MAX_HITS = 20;

    private final ProfitAlertRepository repository;
    private final OpportunityStore store;
    private final DatabaseConnection database;
    private final CopyOnWriteArrayList<Hit> hits = new CopyOnWriteArrayList<>();
    private volatile Instant lastSeen = Instant.now();

    public AlertService(ProfitAlertRepository repository, OpportunityStore store, DatabaseConnection database) {
        this.repository = repository;
        this.store = store;
        this.database = database;
    }

    @Transactional(readOnly = true)
    public List<AlertDto> list(String owner) {
        List<ProfitAlert> alerts = owner == null ? repository.findAll() : repository.findByOwnerOrderByIdDesc(owner);
        return alerts.stream().map(AlertService::toDto).toList();
    }

    @Transactional
    public AlertDto create(AlertRequest request, String defaultOwner) {
        String owner = request.owner() == null || request.owner().isBlank() ? defaultOwner : request.owner().trim();
        String code = request.currencyCode() == null || request.currencyCode().isBlank() ? null : request.currencyCode();
        return toDto(repository.save(new ProfitAlert(owner, request.minProfit(), code)));
    }

    @Transactional
    public AlertDto setActive(long id, boolean active) {
        ProfitAlert alert = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Alert " + id + " does not exist"));
        alert.setActive(active);                  // saved at commit by dirty checking
        return toDto(alert);
    }

    @Transactional
    public void delete(long id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException("Alert " + id + " does not exist");
        }
        repository.deleteById(id);
    }

    /** Fired alerts for one user (or everyone when owner is null), newest first. */
    public List<Hit> recentHits(String owner) {
        return hits.stream().filter(h -> owner == null || h.owner().equals(owner)).toList();
    }

    /** Every 2 seconds on Spring's scheduler thread: match new opportunities against active alerts. */
    @Scheduled(fixedDelay = 2000, initialDelay = 5000)
    @Transactional
    public void checkNewOpportunities() {
        if (!database.isOnline()) {
            return;                               // alerts live in MySQL; nothing to do while it is down
        }
        List<Opportunity> fresh = store.all().stream().filter(o -> o.detectedAt().isAfter(lastSeen)).toList();
        if (fresh.isEmpty()) {
            return;
        }
        lastSeen = fresh.get(0).detectedAt();     // the store keeps the newest first
        try {
            List<ProfitAlert> active = repository.findByActiveTrue();
            for (Opportunity o : fresh) {
                LocalDateTime at = LocalDateTime.ofInstant(o.detectedAt(), ZoneId.systemDefault());
                for (ProfitAlert alert : active) {
                    if (alert.matches(o.profitPercent(), o.pathText())) {
                        alert.recordTrigger(o.pathText(), at);
                        hits.add(0, new Hit(alert.getId(), alert.getOwner(), o.pathText(), o.profitPercent(), at));
                    }
                }
            }
            while (hits.size() > MAX_HITS) {
                hits.remove(hits.size() - 1);
            }
        } catch (DataAccessException e) {
            log.warn("Alert check skipped: {}", e.getMostSpecificCause().getMessage());
        }
    }

    private static AlertDto toDto(ProfitAlert a) {
        return new AlertDto(a.getId(), a.getOwner(), a.getMinProfit(), a.getCurrencyCode(), a.isActive(),
                a.getTriggerCount(), a.getLastTriggeredAt(), a.getLastPath());
    }
}
