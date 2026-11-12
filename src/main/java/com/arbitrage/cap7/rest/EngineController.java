package com.arbitrage.cap7.rest;

import com.arbitrage.api.StateWriter;
import com.arbitrage.cap7.dto.Dtos.SettingsDto;
import com.arbitrage.cap7.dto.Dtos.SettingsRequest;
import com.arbitrage.cap7.service.EngineSettingsService;
import com.arbitrage.feed.TickScheduler;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * The live engine over HTTP.
 *
 * GET  /api/state                     everything the React web desk needs, every second
 * POST /api/pause?paused=true|false   pause or resume ticking
 * GET  /api/v1/engine/settings        fee, spread, inject chance
 * PUT  /api/v1/engine/settings        change them (validated), saved in MySQL
 */
@RestController
public class EngineController {

    private final StateWriter stateWriter;
    private final TickScheduler scheduler;
    private final EngineSettingsService settings;

    public EngineController(StateWriter stateWriter, TickScheduler scheduler, EngineSettingsService settings) {
        this.stateWriter = stateWriter;
        this.scheduler = scheduler;
        this.settings = settings;
    }

    @GetMapping(value = "/api/state", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> state() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(stateWriter.stateJson());
    }

    @PostMapping("/api/pause")
    public Map<String, Boolean> pause(@RequestParam boolean paused) {
        scheduler.setPaused(paused);
        return Map.of("paused", scheduler.isPaused());
    }

    @GetMapping("/api/v1/engine/settings")
    public SettingsDto getSettings() {
        return settings.current();
    }

    @PutMapping("/api/v1/engine/settings")
    public SettingsDto updateSettings(@Valid @RequestBody SettingsRequest request) {
        return settings.update(request);
    }
}
