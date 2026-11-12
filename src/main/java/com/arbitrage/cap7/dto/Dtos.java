package com.arbitrage.cap7.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Data Transfer Objects: what the REST API sends and receives as JSON (Jackson turns records into JSON).
 * Entities are never sent directly, so the API shape does not change when the database does.
 * The *Request records carry Bean Validation annotations, checked by @Valid in the controllers.
 */
public final class Dtos {

    private Dtos() {
    }

    public record LegDto(int legNo, String from, String to, double rate) {
    }

    /** legDetails is null in list responses and filled in the single-item response. */
    public record OpportunityDto(long id, String pathText, int legs, double profitPercent, double fee,
                                 String source, LocalDateTime detectedAt, List<LegDto> legDetails) {
    }

    public record PageDto<T>(List<T> items, int page, int size, long totalItems, int totalPages) {
    }

    public record SummaryDto(String period, long count, double averageProfit, double bestProfit) {
    }

    public record PairCountDto(String from, String to, long count) {
    }

    public record DayStatDto(String day, long count, double averageProfit) {
    }

    public record CurrencyDto(String code, String name, double usdPrice, boolean active) {
    }

    public record CurrencyRequest(
            @NotBlank @Pattern(regexp = "[A-Z]{3}", message = "must be 3 uppercase letters, like AUD") String code,
            @NotBlank @Size(min = 2, max = 40) String name,
            @NotNull @Positive Double usdPrice,
            Boolean active) {
    }

    public record AlertDto(long id, String owner, double minProfit, String currencyCode, boolean active,
                           int triggerCount, LocalDateTime lastTriggeredAt, String lastPath) {
    }

    public record AlertRequest(
            @NotNull @DecimalMin("0.0") @DecimalMax("100.0") Double minProfit,
            @Pattern(regexp = "[A-Z]{3}", message = "must be 3 uppercase letters, or left out") String currencyCode,
            @Size(max = 40) String owner) {
    }

    public record SettingsDto(double fee, double spread, double injectChance) {
    }

    public record SettingsRequest(
            @NotNull @DecimalMin("0.0") @DecimalMax("0.05") Double fee,
            @NotNull @DecimalMin("0.0") @DecimalMax("0.02") Double spread,
            @NotNull @DecimalMin("0.0") @DecimalMax("1.0") Double injectChance) {
    }

    /** The body of every error response, so clients always get the same shape. */
    public record ApiError(Instant timestamp, int status, String error, String message, String path,
                           Map<String, String> fieldErrors) {
    }
}
