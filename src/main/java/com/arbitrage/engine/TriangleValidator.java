package com.arbitrage.engine;

import com.arbitrage.model.RateSnapshot;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * A slow but obvious cross-check for Bellman-Ford:
 * tries every loop of three currencies and keeps the profitable ones.
 */
@Component
public class TriangleValidator {

    private static final double EPS = 1e-12;

    /** Each result is a triangle like [0, 2, 3, 0]. */
    public List<List<Integer>> findProfitableTriangles(RateSnapshot snapshot, double fee) {
        List<List<Integer>> triangles = new ArrayList<>();
        int n = snapshot.size();
        double keep = (1 - fee) * (1 - fee) * (1 - fee);   // the fee is paid three times
        // "a" is always the smallest index, so the same triangle is not listed three times.
        for (int a = 0; a < n; a++) {
            for (int b = a + 1; b < n; b++) {
                for (int c = a + 1; c < n; c++) {
                    if (b == c) {
                        continue;
                    }
                    double product = snapshot.rate(a, b) * snapshot.rate(b, c) * snapshot.rate(c, a) * keep;
                    if (product > 1.0 + EPS) {
                        triangles.add(List.of(a, b, c, a));
                    }
                }
            }
        }
        return triangles;
    }
}
