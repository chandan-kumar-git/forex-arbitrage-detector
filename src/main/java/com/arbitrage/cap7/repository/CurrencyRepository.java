package com.arbitrage.cap7.repository;

import com.arbitrage.cap7.entity.CurrencyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** CRUD for currencies: save, findById, findAll, deleteById come from JpaRepository. */
public interface CurrencyRepository extends JpaRepository<CurrencyEntity, String> {

    List<CurrencyEntity> findAllByOrderByCodeAsc();

    long countByActiveTrue();
}
