package com.arbitrage.engine;

import com.arbitrage.config.AppSettings;
import com.arbitrage.db.DatabaseConnection;
import com.arbitrage.db.DatabaseException;
import com.arbitrage.db.OpportunityDao;
import com.arbitrage.model.Edge;
import com.arbitrage.model.FeedSource;
import com.arbitrage.model.Opportunity;
import com.arbitrage.model.OpportunityLeg;
import com.arbitrage.model.RateSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs one detection: build the graph, look for a profitable cycle,
 * and store the opportunity (in memory and in MySQL) if there is one.
 */
@Component
public class ArbitrageService {

    private static final Logger log = LoggerFactory.getLogger(ArbitrageService.class);
    private static final int MIN_CURRENCIES = 3;

    private final AppSettings settings;
    private final GraphBuilder graphBuilder;
    private final BellmanFordDetector detector;
    private final ProfitCalculator profitCalculator;
    private final TriangleValidator triangleValidator;
    private final OpportunityStore store;
    private final TickStats stats;
    private final OpportunityDao opportunityDao;
    private final DatabaseConnection database;
    private boolean saveProblemLogged = false;    // only used by the scheduler thread

    public ArbitrageService(AppSettings settings, GraphBuilder graphBuilder, BellmanFordDetector detector,
                            ProfitCalculator profitCalculator, TriangleValidator triangleValidator,
                            OpportunityStore store, TickStats stats,
                            OpportunityDao opportunityDao, DatabaseConnection database) {
        this.settings = settings;
        this.graphBuilder = graphBuilder;
        this.detector = detector;
        this.profitCalculator = profitCalculator;
        this.triangleValidator = triangleValidator;
        this.store = store;
        this.stats = stats;
        this.opportunityDao = opportunityDao;
        this.database = database;
    }

    /** Called by the scheduler thread once per tick. */
    public void detect(RateSnapshot snapshot, FeedSource source) {
        if (snapshot.size() < MIN_CURRENCIES) {
            return;                               // a loop needs at least three currencies
        }
        double fee = settings.getFee();
        List<Edge> edges = graphBuilder.build(snapshot, fee);
        List<Integer> cycle = detector.findCycle(edges, snapshot.size());
        if (cycle.isEmpty()) {
            crossCheck(snapshot, fee);
            return;
        }
        double profit = profitCalculator.profitPercent(cycle, snapshot, fee);
        if (profit <= 0) {
            return;
        }
        Opportunity found = new Opportunity(0, codesOf(cycle, snapshot), profit, fee, source, Instant.now());
        Opportunity opportunity = save(found, profitCalculator.legs(cycle, snapshot));
        store.add(opportunity);
        stats.recordOpportunity(opportunity);
        log.info("Arbitrage found: {}  +{}%", opportunity.pathText(), String.format("%.3f", profit));
    }

    /**
     * Saves to MySQL and returns the opportunity with its new id.
     * If the database is offline or the save fails, the opportunity just stays in memory.
     */
    private Opportunity save(Opportunity opportunity, List<OpportunityLeg> legs) {
        if (!database.isOnline()) {
            return opportunity;
        }
        try {
            long id = opportunityDao.insertWithLegs(opportunity, legs);
            saveProblemLogged = false;
            return opportunity.withId(id);
        } catch (DatabaseException e) {
            if (!saveProblemLogged) {             // log the same problem once, not every second
                log.warn("Opportunity kept in memory only. {}", e.getMessage());
                saveProblemLogged = true;
            }
            return opportunity;
        }
    }

    /** Bellman-Ford found nothing, so the brute-force check must find nothing too. */
    private void crossCheck(RateSnapshot snapshot, double fee) {
        List<List<Integer>> triangles = triangleValidator.findProfitableTriangles(snapshot, fee);
        if (!triangles.isEmpty()) {
            log.warn("Bellman-Ford found no cycle but brute force found {} triangle(s)", triangles.size());
        }
    }

    private List<String> codesOf(List<Integer> cycle, RateSnapshot snapshot) {
        List<String> codes = new ArrayList<>();
        for (int index : cycle) {
            codes.add(snapshot.code(index));
        }
        return codes;
    }
}
