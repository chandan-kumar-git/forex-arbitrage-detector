package com.arbitrage.validation;

import com.arbitrage.model.Currency;

import java.util.Set;

/**
 * Checks the currency form before anything is saved.
 * Every method throws ValidationException with the name of the wrong field.
 */
public final class CurrencyValidator {

    public static final String BASE_CODE = "USD";
    public static final int MIN_NAME_LENGTH = 2;
    public static final int MAX_NAME_LENGTH = 40;
    public static final double MAX_PRICE = 1_000_000;
    private static final String CODE_PATTERN = "^[A-Z]{3}$";
    private static final String FILTER_PATTERN = "^[A-Z]{0,3}$";

    private CurrencyValidator() {
    }

    /** Checks the form values and returns a Currency built from them. */
    public static Currency validate(String code, String name, String priceText, boolean active)
            throws ValidationException {
        String cleanCode = code == null ? "" : code.trim();
        if (!cleanCode.matches(CODE_PATTERN)) {
            throw new ValidationException("code", "Code must be 3 uppercase letters, like EUR");
        }
        String cleanName = name == null ? "" : name.trim();
        if (cleanName.length() < MIN_NAME_LENGTH || cleanName.length() > MAX_NAME_LENGTH) {
            throw new ValidationException("name", "Name must be 2 to 40 characters");
        }
        double price = parsePrice(priceText);
        if (cleanCode.equals(BASE_CODE) && !active) {
            throw new ValidationException("active", "USD is the base currency and must stay active");
        }
        return new Currency(cleanCode, cleanName, price, active);
    }

    /** Used by Add: the same code must not be in the list already. */
    public static void checkNotDuplicate(String code, Set<String> existingCodes) throws ValidationException {
        if (existingCodes.contains(code)) {
            throw new ValidationException("code", "Code already exists");
        }
    }

    /** USD is the base currency of the simulator, so it can never be deleted. */
    public static void checkCanDelete(String code) throws ValidationException {
        if (BASE_CODE.equals(code)) {
            throw new ValidationException("code", "USD is the base currency and cannot be deleted");
        }
    }

    /** Checks the currency search box of the History screen. Returns the text in upper case. */
    public static String validateCodeFilter(String text) throws ValidationException {
        String clean = text == null ? "" : text.trim().toUpperCase();
        if (!clean.matches(FILTER_PATTERN)) {
            throw new ValidationException("currency", "Use up to 3 letters, like INR");
        }
        return clean;
    }

    private static double parsePrice(String priceText) throws ValidationException {
        double price;
        try {
            price = Double.parseDouble(priceText == null ? "" : priceText.trim());
        } catch (NumberFormatException e) {
            throw new ValidationException("usdPrice", "USD price must be a number, like 1.08");
        }
        if (!(price > 0) || price > MAX_PRICE) {
            throw new ValidationException("usdPrice", "USD price must be more than 0 and at most 1,000,000");
        }
        return price;
    }
}
