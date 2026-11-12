package com.arbitrage.cap7.rest;

import com.arbitrage.cap7.dto.Dtos.DayStatDto;
import com.arbitrage.cap7.dto.Dtos.OpportunityDto;
import com.arbitrage.cap7.dto.Dtos.PageDto;
import com.arbitrage.cap7.dto.Dtos.PairCountDto;
import com.arbitrage.cap7.dto.Dtos.SummaryDto;
import com.arbitrage.cap7.service.OpportunityQueryService;
import com.arbitrage.cap7.service.Period;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST resource: saved opportunities (read and delete; the engine creates them).
 *
 * GET    /api/v1/opportunities?period=today&minProfit=0.3&code=INR&page=0&size=20
 * GET    /api/v1/opportunities/{id}        one opportunity with its legs
 * DELETE /api/v1/opportunities/{id}        204 No Content
 * GET    /api/v1/opportunities/summary     count, average, best
 * GET    /api/v1/opportunities/top         the 10 best
 * GET    /api/v1/opportunities/pairs       how often each pair appears in a loop
 * GET    /api/v1/opportunities/per-day     loops per day, last 14 days
 */
@RestController
@RequestMapping("/api/v1/opportunities")
public class OpportunityController {

    private final OpportunityQueryService service;

    public OpportunityController(OpportunityQueryService service) {
        this.service = service;
    }

    @GetMapping
    public PageDto<OpportunityDto> list(@RequestParam(defaultValue = "all") String period,
                                        @RequestParam(defaultValue = "0") double minProfit,
                                        @RequestParam(defaultValue = "") String code,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "20") int size) {
        return service.page(Period.parse(period), minProfit, code, page, size);
    }

    @GetMapping("/{id}")
    public OpportunityDto get(@PathVariable long id) {
        return service.get(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/summary")
    public SummaryDto summary(@RequestParam(defaultValue = "all") String period) {
        return service.summary(Period.parse(period));
    }

    @GetMapping("/top")
    public List<OpportunityDto> top() {
        return service.top10();
    }

    @GetMapping("/pairs")
    public List<PairCountDto> pairs() {
        return service.pairFrequency();
    }

    @GetMapping("/per-day")
    public List<DayStatDto> perDay() {
        return service.perDay();
    }
}
