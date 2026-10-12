package com.arbitrage.engine;

import com.arbitrage.model.Edge;
import com.arbitrage.model.RateSnapshot;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns a rate snapshot into graph edges for Bellman-Ford.
 * Each pair of currencies becomes one edge with weight -log(rate after fee).
 */
@Component
public class GraphBuilder {

    /**
     * Why -log? Multiplying rates around a loop gives the profit. log turns the
     * multiplication into addition, and the minus sign turns "product above 1"
     * into "sum below 0", which is exactly a negative cycle.
     */
    public List<Edge> build(RateSnapshot snapshot, double fee) {
        int n = snapshot.size();
        List<Edge> edges = new ArrayList<>();
        for (int from = 0; from < n; from++) {
            for (int to = 0; to < n; to++) {
                if (from != to) {
                    double rateAfterFee = snapshot.rate(from, to) * (1 - fee);
                    edges.add(new Edge(from, to, -Math.log(rateAfterFee)));
                }
            }
        }
        return edges;
    }
}
