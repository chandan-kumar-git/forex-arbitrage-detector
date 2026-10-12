package com.arbitrage.model;

/**
 * One arrow in the currency graph: from one currency index to another.
 * The weight is -log(rate after fee), so a profitable loop has a negative total.
 */
public record Edge(int from, int to, double weight) {
}
