package com.arbitrage.cap7.repository;

import com.arbitrage.cap7.entity.ProfitAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** CRUD for alerts, plus two derived queries. */
public interface ProfitAlertRepository extends JpaRepository<ProfitAlert, Long> {

    List<ProfitAlert> findByActiveTrue();

    List<ProfitAlert> findByOwnerOrderByIdDesc(String owner);
}
