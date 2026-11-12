package com.arbitrage.cap6.view;

import com.arbitrage.model.Currency;
import com.arbitrage.validation.CurrencyValidator;

/** One currency for the JSP pages (JavaBean getters for EL). */
public class CurrencyView {

    private final String code;
    private final String name;
    private final double usdPrice;
    private final boolean active;

    public CurrencyView(Currency currency) {
        this.code = currency.code();
        this.name = currency.name();
        this.usdPrice = currency.usdPrice();
        this.active = currency.active();
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public double getUsdPrice() {
        return usdPrice;
    }

    public boolean isActive() {
        return active;
    }

    /** USD is the base currency: the page hides its delete and deactivate buttons. */
    public boolean isBase() {
        return CurrencyValidator.BASE_CODE.equals(code);
    }
}
