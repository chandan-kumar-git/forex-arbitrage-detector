package com.arbitrage.ui.theme;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * A centred message for "nothing to show": a title, a hint that says what to do next,
 * and an optional button (for example Retry).
 */
public class EmptyState extends JPanel {

    private final JLabel hintLabel = new JLabel();
    private final GridBagConstraints position = new GridBagConstraints();

    public EmptyState(String title, String hint) {
        setLayout(new GridBagLayout());           // GridBagLayout centres its content by default
        setOpaque(false);
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(Theme.BODY_BOLD);
        titleLabel.setForeground(Theme.INK_2);
        hintLabel.setText(hint);
        hintLabel.setFont(Theme.SMALL);
        hintLabel.setForeground(Theme.MUTE);

        position.gridx = 0;
        position.insets = new Insets(Theme.S1, 0, Theme.S1, 0);
        add(titleLabel, position);
        add(hintLabel, position);
    }

    public void setHint(String hint) {
        hintLabel.setText(hint);
    }

    /** Adds a button under the text. */
    public void addButton(AppButton button) {
        position.insets = new Insets(Theme.S3, 0, 0, 0);
        add(button, position);
    }
}
