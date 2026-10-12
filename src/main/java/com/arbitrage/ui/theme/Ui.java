package com.arbitrage.ui.theme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import java.awt.FlowLayout;

/**
 * Two small helpers used by many screens: a row of buttons,
 * and the flat rounded outline for input fields.
 */
public final class Ui {

    private Ui() {
    }

    /** A see-through row of components with an 8 px gap (FlowLayout), used for button rows. */
    public static JPanel row(int flowAlign, JComponent... items) {
        JPanel row = new JPanel(new FlowLayout(flowAlign, 0, 0));
        row.setOpaque(false);
        for (int i = 0; i < items.length; i++) {
            if (i > 0) {
                row.add(Box.createHorizontalStrut(Theme.S2));
            }
            row.add(items[i]);
        }
        return row;
    }

    /** Gives a text field, spinner or combo box the flat rounded outline of the design. */
    public static void styleField(JComponent field) {
        if (field instanceof JTextField) {
            field.setBorder(new RoundedBorder(Theme.S2));
        }
        if (field instanceof JComboBox) {
            field.setBorder(new RoundedBorder(2));
        }
        if (field instanceof JSpinner) {
            JSpinner spinner = (JSpinner) field;
            spinner.setBorder(new RoundedBorder(2));
            JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor) spinner.getEditor();
            editor.getTextField().setBorder(BorderFactory.createEmptyBorder(0, Theme.S2, 0, Theme.S2));
        }
    }
}
