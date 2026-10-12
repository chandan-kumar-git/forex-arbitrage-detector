package com.arbitrage.ui.theme;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;

/**
 * One row of a form: a label above the field and one line below it.
 * The line shows the help text in grey, or an error message in red.
 */
public class FormRow extends JPanel {

    private final JLabel messageLabel = new JLabel();
    private final String help;

    /** The help text may be empty; the line is kept so the form does not jump when an error shows. */
    public FormRow(String label, JComponent field, String help) {
        this.help = help.isEmpty() ? " " : help;
        setLayout(new BorderLayout(0, 2));
        setOpaque(false);

        JLabel nameLabel = new JLabel(label);
        nameLabel.setFont(Theme.SMALL_BOLD);
        nameLabel.setForeground(Theme.INK_2);
        nameLabel.setLabelFor(field);
        Ui.styleField(field);
        field.setPreferredSize(new Dimension(field.getPreferredSize().width, Theme.FIELD_HEIGHT));
        messageLabel.setFont(Theme.SMALL);

        add(nameLabel, BorderLayout.NORTH);
        add(field, BorderLayout.CENTER);
        add(messageLabel, BorderLayout.SOUTH);
        clearError();
    }

    public void showError(String message) {
        messageLabel.setText(message);
        messageLabel.setForeground(Theme.BAD);
    }

    public void clearError() {
        messageLabel.setText(help);
        messageLabel.setForeground(Theme.MUTE);
    }
}
