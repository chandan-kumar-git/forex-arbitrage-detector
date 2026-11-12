package com.arbitrage.cap7.repository;

import com.arbitrage.cap7.entity.LegId;
import com.arbitrage.cap7.entity.OpportunityLegEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/** Read-only statistics over the legs. */
public interface LegRepository extends JpaRepository<OpportunityLegEntity, LegId> {

    /** How often each currency pair appears in a profitable loop: [from, to, count], most first. */
    @Query("SELECT l.fromCode, l.toCode, COUNT(l) FROM OpportunityLegEntity l "
            + "GROUP BY l.fromCode, l.toCode ORDER BY COUNT(l) DESC")
    List<Object[]> pairFrequency();
}
