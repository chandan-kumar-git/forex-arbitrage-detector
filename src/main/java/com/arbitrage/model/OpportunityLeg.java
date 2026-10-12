package com.arbitrage.model;

/**
 * One step of an arbitrage loop: convert fromCode to toCode at this rate.
 * Leg numbers start at 1.
 */
public record OpportunityLeg(int legNo, String fromCode, String toCode, double rate) {
}
