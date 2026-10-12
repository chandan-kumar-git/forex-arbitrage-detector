package com.arbitrage.config;

import org.springframework.stereotype.Component;

/**
 * The three market settings the user can change: fee, spread and inject chance.
 * The fields are volatile because the UI thread writes them and the scheduler thread reads them.
 */
@Component
public class AppSettings {

    public static final double DEFAULT_FEE = 0.0005;
    public static final double DEFAULT_SPREAD = 0.001;
    public static final double DEFAULT_INJECT_CHANCE = 0.30;

    private volatile double fee = DEFAULT_FEE;
    private volatile double spread = DEFAULT_SPREAD;
    private volatile double injectChance = DEFAULT_INJECT_CHANCE;

    public double getFee() {
        return fee;
    }

    public void setFee(double fee) {
        this.fee = fee;
    }

    public double getSpread() {
        return spread;
    }

    public void setSpread(double spread) {
        this.spread = spread;
    }

    public double getInjectChance() {
        return injectChance;
    }

    public void setInjectChance(double injectChance) {
        this.injectChance = injectChance;
    }
}
