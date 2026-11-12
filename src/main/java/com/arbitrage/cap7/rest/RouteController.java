package com.arbitrage.cap7.rest;

import com.arbitrage.cap7.service.ConflictException;
import com.arbitrage.cap7.service.RouteService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Real-life use: the cheapest way to change money.
 * GET /api/v1/route?from=INR&to=JPY&amount=50000
 */
@RestController
public class RouteController {

    private final RouteService routes;

    public RouteController(RouteService routes) {
        this.routes = routes;
    }

    @GetMapping("/api/v1/route")
    public RouteService.Route route(@RequestParam String from, @RequestParam String to,
                                    @RequestParam(defaultValue = "1000") double amount) {
        if (amount <= 0 || amount > 1e12) {
            throw new ConflictException("Amount must be more than 0");
        }
        return routes.plan(from.trim().toUpperCase(), to.trim().toUpperCase(), amount);
    }
}
