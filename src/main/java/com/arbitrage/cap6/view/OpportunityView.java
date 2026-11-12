package com.arbitrage.cap6.view;

import com.arbitrage.model.Opportunity;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * One opportunity as the JSP pages see it. JSP Expression Language (EL) reads JavaBean
 * getters, so ${o.pathText} calls getPathText(). Records have no getX() methods, hence this class.
 */
public class OpportunityView {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm:ss", Locale.ENGLISH);

    private final long id;
    private final String pathText;
    private final int legCount;
    private final double profitPercent;
    private final double fee;
    private final String source;
    private final String detectedAt;
    private final List<LegView> legs;

    public OpportunityView(long id, String pathText, int legCount, double profitPercent, double fee,
                           String source, String detectedAt, List<LegView> legs) {
        this.id = id;
        this.pathText = pathText;
        this.legCount = legCount;
        this.profitPercent = profitPercent;
        this.fee = fee;
        this.source = source;
        this.detectedAt = detectedAt;
        this.legs = List.copyOf(legs);
    }

    /** From the engine's record (CAP-1 model). */
    public static OpportunityView of(Opportunity o) {
        String time = TIME.format(o.detectedAt().atZone(ZoneId.systemDefault()));
        return new OpportunityView(o.id(), o.pathText(), o.path().size() - 1, o.profitPercent(), o.fee(),
                o.source().name(), time, List.of());
    }

    public static String format(java.time.LocalDateTime when) {
        return when == null ? "" : TIME.format(when);
    }

    public long getId() {
        return id;
    }

    public String getPathText() {
        return pathText;
    }

    public int getLegCount() {
        return legCount;
    }

    public double getProfitPercent() {
        return profitPercent;
    }

    public double getFee() {
        return fee;
    }

    public String getSource() {
        return source;
    }

    public String getDetectedAt() {
        return detectedAt;
    }

    public List<LegView> getLegs() {
        return legs;
    }
}
