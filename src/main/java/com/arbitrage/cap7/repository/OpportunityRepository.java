package com.arbitrage.cap7.repository;

import com.arbitrage.cap7.entity.OpportunityEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA writes the implementation of this interface at startup.
 * Method names become queries (derived queries); @Query methods use JPQL, written against entities.
 */
public interface OpportunityRepository extends JpaRepository<OpportunityEntity, Long> {

    /** Derived query: WHERE detected_at >= ? AND profit_percent >= ? AND path_text LIKE %?% */
    Page<OpportunityEntity> findByDetectedAtGreaterThanEqualAndProfitPercentGreaterThanEqualAndPathTextContaining(
            LocalDateTime since, double minProfit, String code, Pageable page);

    /** The 10 best loops ever found. */
    List<OpportunityEntity> findTop10ByOrderByProfitPercentDesc();

    /** JPQL with JOIN FETCH: loads the opportunity and its legs in one SQL query (no N+1 problem). */
    @Query("SELECT o FROM OpportunityEntity o LEFT JOIN FETCH o.legs WHERE o.id = :id")
    Optional<OpportunityEntity> findWithLegs(@Param("id") Long id);

    /** One row: [count, average profit, best profit] since the given time. */
    @Query("SELECT COUNT(o), AVG(o.profitPercent), MAX(o.profitPercent) FROM OpportunityEntity o "
            + "WHERE o.detectedAt >= :since")
    List<Object[]> summarySince(@Param("since") LocalDateTime since);

    /** Loops found per day: [date, count, average profit], newest first. */
    @Query(value = "SELECT DATE(detected_at), COUNT(*), AVG(profit_percent) FROM opportunity "
            + "GROUP BY DATE(detected_at) ORDER BY DATE(detected_at) DESC LIMIT 14", nativeQuery = true)
    List<Object[]> perDay();
}
