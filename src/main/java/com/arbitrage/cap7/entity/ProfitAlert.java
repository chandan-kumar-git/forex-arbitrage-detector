package com.arbitrage.cap7.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * A user's alert: "tell me when a loop pays at least X %, optionally only loops through this currency".
 * AlertService checks every new opportunity and counts the matches here.
 */
@Entity
@Table(name = "profit_alert")
public class ProfitAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String owner;

    @Column(name = "min_profit", nullable = false)
    private double minProfit;

    @Column(name = "currency_code", length = 3)
    private String currencyCode;              // null = any currency

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "trigger_count", nullable = false)
    private int triggerCount;

    @Column(name = "last_triggered_at")
    private LocalDateTime lastTriggeredAt;

    @Column(name = "last_path", length = 120)
    private String lastPath;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;          // set by MySQL (DEFAULT CURRENT_TIMESTAMP)

    protected ProfitAlert() {
    }

    public ProfitAlert(String owner, double minProfit, String currencyCode) {
        this.owner = owner;
        this.minProfit = minProfit;
        this.currencyCode = currencyCode;
    }

    /** True when this opportunity is one the user asked to hear about. */
    public boolean matches(double profitPercent, String pathText) {
        return active && profitPercent >= minProfit && (currencyCode == null || pathText.contains(currencyCode));
    }

    public void recordTrigger(String pathText, LocalDateTime when) {
        triggerCount++;
        lastPath = pathText;
        lastTriggeredAt = when;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public String getOwner() {
        return owner;
    }

    public double getMinProfit() {
        return minProfit;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public boolean isActive() {
        return active;
    }

    public int getTriggerCount() {
        return triggerCount;
    }

    public LocalDateTime getLastTriggeredAt() {
        return lastTriggeredAt;
    }

    public String getLastPath() {
        return lastPath;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
