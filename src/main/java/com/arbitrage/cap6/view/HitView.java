package com.arbitrage.cap6.view;

import com.arbitrage.cap7.service.AlertService;

import java.util.List;

/** A fired alert for the JSP pages (JavaBean getters for EL). */
public class HitView {

    private final String pathText;
    private final double profitPercent;
    private final String at;

    public HitView(AlertService.Hit hit) {
        this.pathText = hit.pathText();
        this.profitPercent = hit.profitPercent();
        this.at = OpportunityView.format(hit.at());
    }

    public static List<HitView> of(List<AlertService.Hit> hits) {
        return hits.stream().map(HitView::new).toList();
    }

    public String getPathText() {
        return pathText;
    }

    public double getProfitPercent() {
        return profitPercent;
    }

    public String getAt() {
        return at;
    }
}
