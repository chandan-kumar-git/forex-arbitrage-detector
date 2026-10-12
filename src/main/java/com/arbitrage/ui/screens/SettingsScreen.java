package com.arbitrage.ui.screens;

import com.arbitrage.config.AppSettings;
import com.arbitrage.db.SettingsDao;
import com.arbitrage.ui.TopBar;
import com.arbitrage.ui.UiServices;
import com.arbitrage.ui.theme.AppButton;
import com.arbitrage.ui.theme.BackgroundTask;
import com.arbitrage.ui.theme.Card;
import com.arbitrage.ui.theme.FormRow;
import com.arbitrage.ui.theme.Theme;
import com.arbitrage.ui.theme.Ui;
import com.arbitrage.validation.SettingsValidator;
import com.arbitrage.validation.ValidationException;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeListener;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Lets the user tune the market (fee, spread, inject chance) and check the database.
 * The spinners show percent; AppSettings stores fractions (0.05 % = 0.0005).
 */
public class SettingsScreen extends BaseScreen {

    private final JSpinner feeSpinner = percentSpinner(0.01);
    private final JSpinner spreadSpinner = percentSpinner(0.05);
    private final JSpinner injectSpinner = percentSpinner(5.0);
    private final Map<String, FormRow> rowsByField = new HashMap<>();   // validator field name -> row
    private final JLabel stateLabel = new JLabel(" ");
    private final DatabaseCard databaseCard;

    public SettingsScreen(UiServices services, TopBar topBar) {
        super("Settings", "Tune the simulated market and check the database", services, topBar);
        databaseCard = new DatabaseCard(services, this::onShow);
        JPanel cards = new JPanel(new GridLayout(1, 2, Theme.S4, 0));   // two cards side by side
        cards.setOpaque(false);
        cards.add(buildMarketCard());
        cards.add(databaseCard);

        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        body.add(cards, BorderLayout.NORTH);      // NORTH keeps the cards at their natural height
        setBody(body);
    }

    @Override
    public void onShow() {
        AppSettings settings = services.settings();
        showValues(settings.getFee(), settings.getSpread(), settings.getInjectChance());
        databaseCard.refresh();
    }

    @Override
    public void refresh() {
        databaseCard.refresh();
    }

    private Card buildMarketCard() {
        rowsByField.put("fee", new FormRow("Fee %", feeSpinner, "Cost of every conversion. 0 to 5 %"));
        rowsByField.put("spread", new FormRow("Spread %", spreadSpinner, "Gap between buy and sell rate. 0 to 2 %"));
        rowsByField.put("injectChance", new FormRow("Inject chance %", injectSpinner,
                "How often a wrong rate is added per tick. 0 to 100 %"));

        // ChangeListener: runs every time a spinner value changes.
        ChangeListener markUnsaved = e -> stateLabel.setText("Unsaved changes");
        feeSpinner.addChangeListener(markUnsaved);
        spreadSpinner.addChangeListener(markUnsaved);
        injectSpinner.addChangeListener(markUnsaved);

        AppButton saveButton = new AppButton("Save", AppButton.Variant.PRIMARY);
        AppButton resetButton = new AppButton("Reset defaults", AppButton.Variant.SECONDARY);
        saveButton.addActionListener(e -> save());
        resetButton.addActionListener(e -> resetDefaults());
        stateLabel.setFont(Theme.SMALL);
        stateLabel.setForeground(Theme.WARN);

        JPanel form = new JPanel(new GridLayout(0, 1, 0, Theme.S2));
        form.setOpaque(false);
        form.add(rowsByField.get("fee"));
        form.add(rowsByField.get("spread"));
        form.add(rowsByField.get("injectChance"));
        JPanel content = new JPanel(new BorderLayout(0, Theme.S3));
        content.setOpaque(false);
        content.add(form, BorderLayout.CENTER);
        content.add(Ui.row(FlowLayout.LEFT, saveButton, resetButton, stateLabel), BorderLayout.SOUTH);

        Card card = new Card("Market");
        card.setContent(content);
        return card;
    }

    /** Validates, applies the values to the next tick, and saves them to MySQL in the background. */
    private void save() {
        double fee = fraction(feeSpinner);
        double spread = fraction(spreadSpinner);
        double injectChance = fraction(injectSpinner);
        clearErrors();
        try {
            SettingsValidator.validateFee(fee);
            SettingsValidator.validateSpread(spread);
            SettingsValidator.validateInjectChance(injectChance);
        } catch (ValidationException e) {
            rowsByField.get(e.getField()).showError(e.getMessage());
            return;
        }
        AppSettings settings = services.settings();
        settings.setFee(fee);
        settings.setSpread(spread);
        settings.setInjectChance(injectChance);
        stateLabel.setText(" ");

        Map<String, String> values = new HashMap<>();
        values.put(SettingsDao.KEY_FEE, plain(fee));
        values.put(SettingsDao.KEY_SPREAD, plain(spread));
        values.put(SettingsDao.KEY_INJECT_CHANCE, plain(injectChance));
        boolean online = services.database().isOnline();
        BackgroundTask.run(() -> {
            if (online) {
                services.settingsDao().saveAll(values);
            }
            return online;
        }, saved -> topBar.showToast(saved ? "Settings saved" : "Settings applied – database offline, not saved"),
                this::showError);
    }

    private void resetDefaults() {
        showValues(AppSettings.DEFAULT_FEE, AppSettings.DEFAULT_SPREAD, AppSettings.DEFAULT_INJECT_CHANCE);
        save();
    }

    private void showValues(double fee, double spread, double injectChance) {
        feeSpinner.setValue(fee * 100);
        spreadSpinner.setValue(spread * 100);
        injectSpinner.setValue(injectChance * 100);
        clearErrors();
        stateLabel.setText(" ");
    }

    private void clearErrors() {
        for (FormRow row : rowsByField.values()) {
            row.clearError();
        }
    }

    /** A spinner for a percent value. The range is wide; the validator decides what is allowed. */
    private static JSpinner percentSpinner(double step) {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 100.0, step));
        spinner.setEditor(new JSpinner.NumberEditor(spinner, "0.00"));
        return spinner;
    }

    /** Percent from the spinner as a fraction, rounded to 6 decimals (5.0 % becomes 0.05). */
    private static double fraction(JSpinner spinner) {
        double percent = ((Number) spinner.getValue()).doubleValue();
        return Math.round(percent * 10_000) / 1_000_000.0;
    }

    /** A plain decimal string such as 0.000500 for the app_setting table. */
    private static String plain(double value) {
        return String.format(Locale.ROOT, "%.6f", value);
    }
}
