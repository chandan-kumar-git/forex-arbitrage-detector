package com.arbitrage.ui.theme;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;

/**
 * A card that shows one headline number: a small caption,
 * the big value, and a hint line under it.
 */
public class StatCard extends Card {

    private final JLabel valueLabel = new JLabel("0");
    private final JLabel hintLabel = new JLabel(" ");

    public StatCard(String caption, String hint) {
        super(null);
        JLabel captionLabel = new JLabel(caption);
        captionLabel.setFont(Theme.SMALL);
        captionLabel.setForeground(Theme.INK_2);
        valueLabel.setFont(Theme.STAT);
        valueLabel.setForeground(Theme.INK);
        hintLabel.setFont(Theme.SMALL);
        hintLabel.setForeground(Theme.MUTE);
        hintLabel.setText(hint);

        JPanel lines = new JPanel(new BorderLayout(0, Theme.S1));
        lines.setOpaque(false);
        lines.add(captionLabel, BorderLayout.NORTH);
        lines.add(valueLabel, BorderLayout.CENTER);
        lines.add(hintLabel, BorderLayout.SOUTH);
        setContent(lines);
    }

    public void setValue(String value) {
        valueLabel.setText(value);
    }

    public void setHint(String hint) {
        hintLabel.setText(hint);
    }
}
