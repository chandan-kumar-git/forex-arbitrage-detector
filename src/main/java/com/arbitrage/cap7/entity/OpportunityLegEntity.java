package com.arbitrage.cap7.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * JPA entity for opportunity_leg. Its primary key has two columns (opportunity_id, leg_no),
 * so it uses @IdClass. The @ManyToOne side holds the foreign key to the opportunity.
 */
@Entity
@Table(name = "opportunity_leg")
@IdClass(LegId.class)
public class OpportunityLegEntity {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opportunity_id")
    private OpportunityEntity opportunity;

    @Id
    @Column(name = "leg_no")
    private Integer legNo;

    @Column(name = "from_code", nullable = false, length = 3)
    private String fromCode;

    @Column(name = "to_code", nullable = false, length = 3)
    private String toCode;

    @Column(nullable = false)
    private double rate;

    protected OpportunityLegEntity() {
    }

    public OpportunityEntity getOpportunity() {
        return opportunity;
    }

    public Integer getLegNo() {
        return legNo;
    }

    public String getFromCode() {
        return fromCode;
    }

    public String getToCode() {
        return toCode;
    }

    public double getRate() {
        return rate;
    }
}
