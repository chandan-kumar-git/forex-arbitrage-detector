package com.arbitrage.ui.screens;

import com.arbitrage.model.Currency;
import com.arbitrage.ui.theme.AppButton;
import com.arbitrage.ui.theme.FormRow;
import com.arbitrage.ui.theme.Theme;
import com.arbitrage.ui.theme.Ui;
import com.arbitrage.validation.ValidationException;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * The form on the Currencies screen: code, name, USD price, active, and the four buttons.
 * It only holds and shows values; the screen decides what the buttons do.
 */
public class CurrencyForm extends JPanel {

    final AppButton addButton = new AppButton("Add", AppButton.Variant.PRIMARY);
    final AppButton updateButton = new AppButton("Update", AppButton.Variant.SECONDARY);
    final AppButton deleteButton = new AppButton("Delete", AppButton.Variant.DANGER);
    final AppButton clearButton = new AppButton("Clear", AppButton.Variant.SECONDARY);

    private final JTextField codeField = new JTextField();
    private final JTextField nameField = new JTextField();
    private final JTextField priceField = new JTextField();
    private final JCheckBox activeBox = new JCheckBox("Active", true);
    private final Map<String, FormRow> rowsByField = new HashMap<>();   // validator field name -> row

    public CurrencyForm() {
        setLayout(new GridBagLayout());
        setOpaque(false);
        activeBox.setOpaque(false);
        rowsByField.put("code", new FormRow("Code", codeField, "3 uppercase letters, like EUR"));
        rowsByField.put("name", new FormRow("Name", nameField, "2 to 40 characters"));
        rowsByField.put("usdPrice", new FormRow("USD price", priceField, "Price of 1 unit in US dollars"));
        rowsByField.put("active", new FormRow("Status", activeBox, "Inactive currencies are skipped"));

        JPanel bottomButtons = new JPanel(new BorderLayout());
        bottomButtons.setOpaque(false);
        bottomButtons.add(deleteButton, BorderLayout.WEST);
        bottomButtons.add(clearButton, BorderLayout.EAST);

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;                              // one column; every add() goes to the next row
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, Theme.S1, 0);
        add(rowsByField.get("code"), c);
        add(rowsByField.get("name"), c);
        add(rowsByField.get("usdPrice"), c);
        add(rowsByField.get("active"), c);
        c.insets = new Insets(Theme.S2, 0, Theme.S2, 0);
        add(Ui.row(FlowLayout.LEFT, addButton, updateButton), c);
        add(bottomButtons, c);
        clear();
    }

    String code() {
        return codeField.getText();
    }

    String currencyName() {
        return nameField.getText();
    }

    String priceText() {
        return priceField.getText();
    }

    boolean isActive() {
        return activeBox.isSelected();
    }

    /** Puts a table row into the form. The code becomes read-only because it is the key. */
    void fill(Currency currency) {
        clearErrors();
        codeField.setText(currency.code());
        nameField.setText(currency.name());
        priceField.setText(BigDecimal.valueOf(currency.usdPrice()).toPlainString());
        activeBox.setSelected(currency.active());
        setEditing(true);
    }

    void clear() {
        clearErrors();
        codeField.setText("");
        nameField.setText("");
        priceField.setText("");
        activeBox.setSelected(true);
        setEditing(false);
    }

    /** Shows the message under the field the validator complained about. */
    void showError(ValidationException e) {
        FormRow row = rowsByField.get(e.getField());
        if (row != null) {
            row.showError(e.getMessage());
        }
    }

    void clearErrors() {
        for (FormRow row : rowsByField.values()) {
            row.clearError();
        }
    }

    /** Editing an existing row: Update and Delete are allowed, Add is not. */
    private void setEditing(boolean editing) {
        codeField.setEditable(!editing);
        addButton.setEnabled(!editing);
        updateButton.setEnabled(editing);
        deleteButton.setEnabled(editing);
    }
}
