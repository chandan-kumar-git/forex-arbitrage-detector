package com.arbitrage.cap7.rest;

import com.arbitrage.cap7.dto.Dtos.CurrencyDto;
import com.arbitrage.cap7.dto.Dtos.CurrencyRequest;
import com.arbitrage.cap7.service.ConflictException;
import com.arbitrage.cap7.service.CurrencyCrudService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * REST resource: currencies, full CRUD.
 *
 * GET    /api/v1/currencies          200 list
 * GET    /api/v1/currencies/{code}   200 or 404
 * POST   /api/v1/currencies          201 Created + Location header, 400 invalid, 409 duplicate
 * PUT    /api/v1/currencies/{code}   200, 400, 404
 * DELETE /api/v1/currencies/{code}   204, 404, 409 (USD or used in history)
 */
@RestController
@RequestMapping("/api/v1/currencies")
public class CurrencyController {

    private final CurrencyCrudService service;

    public CurrencyController(CurrencyCrudService service) {
        this.service = service;
    }

    @GetMapping
    public List<CurrencyDto> list() {
        return service.list();
    }

    @GetMapping("/{code}")
    public CurrencyDto get(@PathVariable String code) {
        return service.get(code.toUpperCase());
    }

    /** @Valid runs the Bean Validation annotations on CurrencyRequest before this method is called. */
    @PostMapping
    public ResponseEntity<CurrencyDto> create(@Valid @RequestBody CurrencyRequest request) {
        CurrencyDto created = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{code}")
                .buildAndExpand(created.code()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{code}")
    public CurrencyDto update(@PathVariable String code, @Valid @RequestBody CurrencyRequest request) {
        if (!code.equalsIgnoreCase(request.code())) {
            throw new ConflictException("The code in the URL and in the body must be the same");
        }
        return service.update(code.toUpperCase(), request);
    }

    @DeleteMapping("/{code}")
    public ResponseEntity<Void> delete(@PathVariable String code) {
        service.delete(code.toUpperCase());
        return ResponseEntity.noContent().build();
    }
}
