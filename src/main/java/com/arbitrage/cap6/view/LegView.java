package com.arbitrage.cap6.view;

/** One conversion step for the JSP pages (JavaBean getters for EL). */
public class LegView {

    private final int legNo;
    private final String fromCode;
    private final String toCode;
    private final double rate;

    public LegView(int legNo, String fromCode, String toCode, double rate) {
        this.legNo = legNo;
        this.fromCode = fromCode;
        this.toCode = toCode;
        this.rate = rate;
    }

    public int getLegNo() {
        return legNo;
    }

    public String getFromCode() {
        return fromCode;
    }

    public String getToCode() {
        return toCode;
    }

    public double getRate() {
        return rate;
    }
}
