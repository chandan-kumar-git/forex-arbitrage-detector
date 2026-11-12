package com.arbitrage.cap7.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA entity for the opportunity table. One opportunity has many legs (@OneToMany);
 * the legs are loaded only when they are first used (lazy, the default for collections).
 */
@Entity
@Table(name = "opportunity")
public class OpportunityEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)   // MySQL AUTO_INCREMENT gives the id
    private Long id;

    @Column(name = "path_text", nullable = false, length = 120)
    private String pathText;

    @Column(name = "path_length", nullable = false)
    private int pathLength;

    @Column(name = "profit_percent", nullable = false)
    private double profitPercent;

    @Column(nullable = false)
    private double fee;

    @Column(nullable = false, length = 10)
    private String source;

    @Column(name = "detected_at", nullable = false)
    private LocalDateTime detectedAt;

    /** mappedBy: the leg side owns the foreign key column opportunity_id. */
    @OneToMany(mappedBy = "opportunity", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("legNo ASC")
    private List<OpportunityLegEntity> legs = new ArrayList<>();

    /** JPA needs a no-argument constructor; it is protected so normal code cannot use it. */
    protected OpportunityEntity() {
    }

    public Long getId() {
        return id;
    }

    public String getPathText() {
        return pathText;
    }

    public int getPathLength() {
        return pathLength;
    }

    public double getProfitPercent() {
        return profitPercent;
    }

    public double getFee() {
        return fee;
    }

    public String getSource() {
        return source;
    }

    public LocalDateTime getDetectedAt() {
        return detectedAt;
    }

    public List<OpportunityLegEntity> getLegs() {
        return legs;
    }
}
