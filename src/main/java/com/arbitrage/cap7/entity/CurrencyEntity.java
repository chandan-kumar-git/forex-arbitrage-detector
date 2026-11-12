package com.arbitrage.cap7.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA entity for the currency table. The primary key is the 3-letter code itself (a natural key),
 * so there is no @GeneratedValue. created_at / updated_at are filled by MySQL and not mapped.
 */
@Entity
@Table(name = "currency")
public class CurrencyEntity {

    @Id
    @Column(length = 3)
    private String code;

    @Column(nullable = false, length = 40)
    private String name;

    @Column(name = "usd_price", nullable = false)
    private double usdPrice;

    @Column(nullable = false)
    private boolean active;

    protected CurrencyEntity() {
    }

    public CurrencyEntity(String code, String name, double usdPrice, boolean active) {
        this.code = code;
        this.name = name;
        this.usdPrice = usdPrice;
        this.active = active;
    }

    /** Changes the editable fields; Hibernate writes them at commit (dirty checking). */
    public void update(String newName, double newPrice, boolean newActive) {
        this.name = newName;
        this.usdPrice = newPrice;
        this.active = newActive;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public double getUsdPrice() {
        return usdPrice;
    }

    public boolean isActive() {
        return active;
    }
}
