package com.arbitrage.engine;

import com.arbitrage.model.Edge;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Finds a negative cycle in the currency graph with the Bellman-Ford algorithm.
 * A negative cycle is a loop of conversions that ends with more money than it started with.
 */
@Component
public class BellmanFordDetector {

    /** Tiny tolerance so that rounding errors are not reported as arbitrage. */
    private static final double EPS = 1e-12;

    /**
     * Returns the cycle as node indexes, first and last equal, for example [3, 1, 2, 3].
     * Returns an empty list when there is no negative cycle.
     */
    public List<Integer> findCycle(List<Edge> edges, int n) {
        double[] dist = new double[n];            // all 0.0 = virtual source
        int[] parent = new int[n];
        Arrays.fill(parent, -1);
        int lastUpdated = -1;
        for (int round = 1; round <= n; round++) { // round n is the check round
            lastUpdated = -1;
            for (Edge e : edges) {
                double candidate = dist[e.from()] + e.weight();
                if (candidate < dist[e.to()] - EPS) {
                    dist[e.to()] = candidate;
                    parent[e.to()] = e.from();
                    lastUpdated = e.to();
                }
            }
            if (lastUpdated == -1) {
                return List.of();                 // nothing changed: no negative cycle
            }
        }
        int v = lastUpdated;
        for (int i = 0; i < n; i++) {
            v = parent[v];                        // walk n steps back: now inside the cycle
        }
        List<Integer> cycle = new ArrayList<>();
        int current = v;
        do {
            cycle.add(current);
            current = parent[current];
        } while (current != v);
        cycle.add(v);
        Collections.reverse(cycle);
        return cycle;
    }
}
