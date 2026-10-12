package com.arbitrage.engine;

import com.arbitrage.TestSnapshots;
import com.arbitrage.model.Edge;
import com.arbitrage.model.RateSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Checks that Bellman-Ford finds the known cycle and stays quiet on a fair market. */
class BellmanFordDetectorTest {

    private final GraphBuilder graphBuilder = new GraphBuilder();
    private final BellmanFordDetector detector = new BellmanFordDetector();
    private final ProfitCalculator profitCalculator = new ProfitCalculator();
    private final TriangleValidator triangleValidator = new TriangleValidator();

    @Test
    void findsTheKnownProfitableCycle() {
        RateSnapshot snapshot = TestSnapshots.reference();
        List<Edge> edges = graphBuilder.build(snapshot, TestSnapshots.FEE);

        List<Integer> cycle = detector.findCycle(edges, snapshot.size());

        assertFalse(cycle.isEmpty(), "a cycle must be found");
        assertEquals(cycle.get(0), cycle.get(cycle.size() - 1), "a cycle starts and ends at the same node");
        assertTrue(cycle.size() >= 4, "a cycle has at least three different currencies");
        assertTrue(usesEdge(cycle, TestSnapshots.EUR, TestSnapshots.GBP), "the cycle must use the wrong rate");
        assertTrue(profitCalculator.profitPercent(cycle, snapshot, TestSnapshots.FEE) > 0);
    }

    @Test
    void fairMarketHasNoCycle() {
        RateSnapshot snapshot = TestSnapshots.fair();
        List<Edge> edges = graphBuilder.build(snapshot, TestSnapshots.FEE);

        assertTrue(detector.findCycle(edges, snapshot.size()).isEmpty());
    }

    @Test
    void bruteForceAgreesWithBellmanFord() {
        RateSnapshot profitable = TestSnapshots.reference();
        RateSnapshot fair = TestSnapshots.fair();

        assertFalse(triangleValidator.findProfitableTriangles(profitable, TestSnapshots.FEE).isEmpty());
        assertTrue(triangleValidator.findProfitableTriangles(fair, TestSnapshots.FEE).isEmpty());
    }

    @Test
    void graphHasOneEdgeForEveryOrderedPair() {
        RateSnapshot snapshot = TestSnapshots.reference();

        assertEquals(4 * 3, graphBuilder.build(snapshot, TestSnapshots.FEE).size());
    }

    private boolean usesEdge(List<Integer> cycle, int from, int to) {
        for (int i = 0; i < cycle.size() - 1; i++) {
            if (cycle.get(i) == from && cycle.get(i + 1) == to) {
                return true;
            }
        }
        return false;
    }
}
