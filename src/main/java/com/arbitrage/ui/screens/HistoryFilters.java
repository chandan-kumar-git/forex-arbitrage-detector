package com.arbitrage.ui.screens;

import com.arbitrage.ui.theme.AppButton;
import com.arbitrage.ui.theme.Card;
import com.arbitrage.ui.theme.Theme;
import com.arbitrage.ui.theme.Ui;
import com.arbitrage.validation.CurrencyValidator;
import com.arbitrage.validation.ValidationException;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * The filter card of the History screen: period, minimum profit and a currency code.
 * It turns what the user chose into the three values the DAO needs.
 */
public class HistoryFilters extends Card {

    private static final String[] PERIODS = {"Last hour", "Today", "Last 7 days", "All"};

    final AppButton applyButton = new AppButton("Apply", AppButton.Variant.PRIMARY);
    final AppButton resetButton = new AppButton("Reset", AppButton.Variant.SECONDARY);

    private final JComboBox<String> periodBox = new JComboBox<>(PERIODS);
    private final JSpinner minProfitSpinner = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 100.0, 0.05));
    private final JTextField currencyField = new JTextField();
    private final JLabel errorLabel = new JLabel(" ");

    public HistoryFilters() {
        super("Filters");
        minProfitSpinner.setEditor(new JSpinner.NumberEditor(minProfitSpinner, "0.00"));
        errorLabel.setFont(Theme.SMALL);
        errorLabel.setForeground(Theme.BAD);
        addAction(errorLabel);                    // errors show on the right of the card title
        setContent(Ui.row(FlowLayout.LEFT,
                new JLabel("Period"), sized(periodBox, 130),
                new JLabel("Min profit %"), sized(minProfitSpinner, 90),
                new JLabel("Currency"), sized(currencyField, 80),
                applyButton, resetButton));
        reset();
    }

    /** The oldest time to show. "All" starts in 1970, long before any saved row. */
    LocalDateTime since() {
        String period = (String) periodBox.getSelectedItem();
        if ("Last hour".equals(period)) {
            return LocalDateTime.now().minusHours(1);
        }
        if ("Today".equals(period)) {
            return LocalDate.now().atStartOfDay();
        }
        if ("Last 7 days".equals(period)) {
            return LocalDateTime.now().minusDays(7);
        }
        return LocalDateTime.of(1970, 1, 1, 0, 0);
    }

    double minProfit() {
        return ((Number) minProfitSpinner.getValue()).doubleValue();
    }

    /** The LIKE pattern for the path: "%INR%", or "%" when the box is empty. */
    String codeLike() throws ValidationException {
        return "%" + CurrencyValidator.validateCodeFilter(currencyField.getText()) + "%";
    }

    void reset() {
        periodBox.setSelectedItem("Today");
        minProfitSpinner.setValue(0.0);
        currencyField.setText("");
        clearError();
    }

    void showError(String message) {
        errorLabel.setText(message);
    }

    void clearError() {
        errorLabel.setText(" ");
    }

    private static JComponent sized(JComponent field, int width) {
        Ui.styleField(field);
        field.setPreferredSize(new Dimension(width, Theme.FIELD_HEIGHT));
        return field;
    }
}
