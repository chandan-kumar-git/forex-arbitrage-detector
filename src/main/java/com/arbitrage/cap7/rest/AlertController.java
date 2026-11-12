package com.arbitrage.cap7.rest;

import com.arbitrage.cap7.dto.Dtos.AlertDto;
import com.arbitrage.cap7.dto.Dtos.AlertRequest;
import com.arbitrage.cap7.service.AlertService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST resource: profit alerts.
 *
 * GET    /api/v1/alerts?owner=demo        list (all owners when left out)
 * POST   /api/v1/alerts                   201, body {"minProfit":0.5,"currencyCode":"INR","owner":"demo"}
 * PATCH  /api/v1/alerts/{id}?active=false switch on or off
 * DELETE /api/v1/alerts/{id}              204
 */
@RestController
@RequestMapping("/api/v1/alerts")
public class AlertController {

    private final AlertService service;

    public AlertController(AlertService service) {
        this.service = service;
    }

    @GetMapping
    public List<AlertDto> list(@RequestParam(required = false) String owner) {
        return service.list(owner);
    }

    @PostMapping
    public ResponseEntity<AlertDto> create(@Valid @RequestBody AlertRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request, "api"));
    }

    @PatchMapping("/{id}")
    public AlertDto setActive(@PathVariable long id, @RequestParam boolean active) {
        return service.setActive(id, active);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
