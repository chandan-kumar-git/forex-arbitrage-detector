package com.arbitrage.cap7.entity;

import java.io.Serializable;
import java.util.Objects;

/**
 * The two-column primary key of a leg. JPA requires an id class to be Serializable
 * and to implement equals and hashCode. The field names match the @Id fields of the entity.
 */
public class LegId implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long opportunity;                  // the id of the OpportunityEntity
    private Integer legNo;

    public LegId() {
    }

    public LegId(Long opportunity, Integer legNo) {
        this.opportunity = opportunity;
        this.legNo = legNo;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof LegId id && Objects.equals(opportunity, id.opportunity)
                && Objects.equals(legNo, id.legNo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(opportunity, legNo);
    }
}
