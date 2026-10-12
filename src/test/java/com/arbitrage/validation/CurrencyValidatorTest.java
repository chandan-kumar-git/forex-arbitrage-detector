package com.arbitrage.validation;

import com.arbitrage.model.Currency;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Checks every rule of the currency form. */
class CurrencyValidatorTest {

    @Test
    void goodInputPasses() throws ValidationException {
        Currency currency = CurrencyValidator.validate("AUD", " Australian Dollar ", "0.66", true);

        assertEquals(new Currency("AUD", "Australian Dollar", 0.66, true), currency);
    }

    @Test
    void badCodeFails() {
        assertField("code", () -> CurrencyValidator.validate("aud", "Australian Dollar", "0.66", true));
        assertField("code", () -> CurrencyValidator.validate("AUDX", "Australian Dollar", "0.66", true));
        assertField("code", () -> CurrencyValidator.validate("", "Australian Dollar", "0.66", true));
    }

    @Test
    void emptyNameFails() {
        assertField("name", () -> CurrencyValidator.validate("AUD", "  ", "0.66", true));
    }

    @Test
    void priceMustBeAPositiveNumber() {
        assertField("usdPrice", () -> CurrencyValidator.validate("AUD", "Australian Dollar", "0", true));
        assertField("usdPrice", () -> CurrencyValidator.validate("AUD", "Australian Dollar", "-1", true));
        assertField("usdPrice", () -> CurrencyValidator.validate("AUD", "Australian Dollar", "abc", true));
        assertField("usdPrice", () -> CurrencyValidator.validate("AUD", "Australian Dollar", "2000000", true));
    }

    @Test
    void duplicateCodeFails() {
        Set<String> existing = Set.of("USD", "EUR");

        assertField("code", () -> CurrencyValidator.checkNotDuplicate("EUR", existing));
        assertDoesNotThrow(() -> CurrencyValidator.checkNotDuplicate("AUD", existing));
    }

    @Test
    void usdCannotBeDeletedOrDeactivated() {
        assertField("code", () -> CurrencyValidator.checkCanDelete("USD"));
        assertField("active", () -> CurrencyValidator.validate("USD", "US Dollar", "1", false));
        assertDoesNotThrow(() -> CurrencyValidator.checkCanDelete("EUR"));
    }

    @Test
    void codeFilterAllowsUpToThreeLetters() throws ValidationException {
        assertEquals("INR", CurrencyValidator.validateCodeFilter(" inr "));
        assertEquals("", CurrencyValidator.validateCodeFilter(""));
        assertField("currency", () -> CurrencyValidator.validateCodeFilter("IN%"));
    }

    /** The action must throw a ValidationException that points at the given field. */
    private void assertField(String field, ValidatorCall call) {
        ValidationException e = assertThrows(ValidationException.class, call::run);
        assertEquals(field, e.getField());
    }

    private interface ValidatorCall {
        void run() throws ValidationException;
    }
}
