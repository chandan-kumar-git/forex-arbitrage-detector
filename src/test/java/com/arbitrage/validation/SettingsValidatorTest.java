package com.arbitrage.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Checks that every setting is kept inside its allowed range. */
class SettingsValidatorTest {

    @Test
    void feeMustBeBetweenZeroAndFivePercent() {
        assertDoesNotThrow(() -> SettingsValidator.validateFee(0.0));
        assertDoesNotThrow(() -> SettingsValidator.validateFee(0.05));
        assertThrows(ValidationException.class, () -> SettingsValidator.validateFee(-0.001));
        assertThrows(ValidationException.class, () -> SettingsValidator.validateFee(0.051));
        assertThrows(ValidationException.class, () -> SettingsValidator.validateFee(Double.NaN));
    }

    @Test
    void spreadMustBeBetweenZeroAndTwoPercent() {
        assertDoesNotThrow(() -> SettingsValidator.validateSpread(0.001));
        assertThrows(ValidationException.class, () -> SettingsValidator.validateSpread(0.021));
        assertThrows(ValidationException.class, () -> SettingsValidator.validateSpread(-0.1));
    }

    @Test
    void injectChanceMustBeBetweenZeroAndOne() {
        assertDoesNotThrow(() -> SettingsValidator.validateInjectChance(0.30));
        assertDoesNotThrow(() -> SettingsValidator.validateInjectChance(1.0));
        assertThrows(ValidationException.class, () -> SettingsValidator.validateInjectChance(1.01));
    }

    @Test
    void hostMustNotBeBlank() {
        assertDoesNotThrow(() -> SettingsValidator.validateHost("localhost"));
        assertThrows(ValidationException.class, () -> SettingsValidator.validateHost("  "));
        assertThrows(ValidationException.class, () -> SettingsValidator.validateHost(null));
    }

    @Test
    void portMustBeInTheUserRange() {
        assertDoesNotThrow(() -> SettingsValidator.validatePort(5050));
        assertThrows(ValidationException.class, () -> SettingsValidator.validatePort(80));
        assertThrows(ValidationException.class, () -> SettingsValidator.validatePort(70000));
    }
}
