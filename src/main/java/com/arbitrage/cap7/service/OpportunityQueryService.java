package com.arbitrage.cap7.service;

import com.arbitrage.cap7.dto.Dtos.DayStatDto;
import com.arbitrage.cap7.dto.Dtos.LegDto;
import com.arbitrage.cap7.dto.Dtos.OpportunityDto;
import com.arbitrage.cap7.dto.Dtos.PageDto;
import com.arbitrage.cap7.dto.Dtos.PairCountDto;
import com.arbitrage.cap7.dto.Dtos.SummaryDto;
import com.arbitrage.cap7.entity.OpportunityEntity;
import com.arbitrage.cap7.repository.LegRepository;
import com.arbitrage.cap7.repository.OpportunityRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads and deletes saved opportunities through JPA. readOnly transactions let Hibernate skip
 * dirty checking; the entity-to-DTO mapping happens inside the transaction, while lazy legs can load.
 */
@Service
public class OpportunityQueryService {

    private static final int MAX_PAGE_SIZE = 100;

    private final OpportunityRepository opportunities;
    private final LegRepository legs;

    public OpportunityQueryService(OpportunityRepository opportunities, LegRepository legs) {
        this.opportunities = opportunities;
        this.legs = legs;
    }

    @Transactional(readOnly = true)
    public PageDto<OpportunityDto> page(Period period, double minProfit, String code, int page, int size) {
        int safeSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        PageRequest request = PageRequest.of(Math.max(0, page), safeSize, Sort.by(Sort.Direction.DESC, "detectedAt"));
        Page<OpportunityEntity> result = opportunities
                .findByDetectedAtGreaterThanEqualAndProfitPercentGreaterThanEqualAndPathTextContaining(
                        period.since(), minProfit, code == null ? "" : code.trim().toUpperCase(), request);
        List<OpportunityDto> items = result.getContent().stream().map(o -> toDto(o, false)).toList();
        return new PageDto<>(items, result.getNumber(), result.getSize(), result.getTotalElements(),
                result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public OpportunityDto get(long id) {
        return opportunities.findWithLegs(id)
                .map(o -> toDto(o, true))
                .orElseThrow(() -> new NotFoundException("Opportunity " + id + " does not exist"));
    }

    @Transactional
    public void delete(long id) {
        if (!opportunities.existsById(id)) {
            throw new NotFoundException("Opportunity " + id + " does not exist");
        }
        opportunities.deleteById(id);             // cascade = ALL removes the legs too
    }

    @Transactional(readOnly = true)
    public SummaryDto summary(Period period) {
        Object[] row = opportunities.summarySince(period.since()).get(0);
        long count = ((Number) row[0]).longValue();
        double average = row[1] == null ? 0 : ((Number) row[1]).doubleValue();
        double best = row[2] == null ? 0 : ((Number) row[2]).doubleValue();
        return new SummaryDto(period.name().toLowerCase(), count, average, best);
    }

    @Transactional(readOnly = true)
    public List<OpportunityDto> top10() {
        return opportunities.findTop10ByOrderByProfitPercentDesc().stream().map(o -> toDto(o, false)).toList();
    }

    @Transactional(readOnly = true)
    public List<PairCountDto> pairFrequency() {
        List<PairCountDto> pairs = new ArrayList<>();
        for (Object[] row : legs.pairFrequency()) {
            pairs.add(new PairCountDto((String) row[0], (String) row[1], ((Number) row[2]).longValue()));
        }
        return pairs;
    }

    @Transactional(readOnly = true)
    public List<DayStatDto> perDay() {
        List<DayStatDto> days = new ArrayList<>();
        for (Object[] row : opportunities.perDay()) {
            days.add(new DayStatDto(String.valueOf(row[0]), ((Number) row[1]).longValue(),
                    ((Number) row[2]).doubleValue()));
        }
        return days;
    }

    private static OpportunityDto toDto(OpportunityEntity o, boolean withLegs) {
        List<LegDto> legDtos = withLegs
                ? o.getLegs().stream().map(l -> new LegDto(l.getLegNo(), l.getFromCode(), l.getToCode(), l.getRate())).toList()
                : null;
        return new OpportunityDto(o.getId(), o.getPathText(), o.getPathLength(), o.getProfitPercent(), o.getFee(),
                o.getSource(), o.getDetectedAt(), legDtos);
    }
}
