package com.arbitrage.ui.theme;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 * A small rounded pill with a coloured dot and a short text, for example "● Live".
 * The kind decides the colour: GOOD green, WARN amber, BAD red, INFO teal.
 */
public class StatusChip extends JLabel {

    public enum Kind { GOOD, WARN, BAD, INFO }

    private static final int DOT = 8;

    private Kind kind;

    public StatusChip(Kind kind, String text) {
        setFont(Theme.SMALL_BOLD);
        // Left padding leaves room for the dot.
        setBorder(BorderFactory.createEmptyBorder(5, Theme.S3 + DOT + 6, 5, Theme.S3));
        setStatus(kind, text);
    }

    public void setStatus(Kind kind, String text) {
        this.kind = kind;
        setText(text);
        setForeground(strongColor());
        repaint();
    }

    /** Keeps layouts such as BoxLayout from stretching the pill. */
    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Theme.smooth(g);
        g2.setColor(tintColor());
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
        g2.setColor(strongColor());
        g2.fillOval(Theme.S3, (getHeight() - DOT) / 2, DOT, DOT);
        g2.dispose();
        super.paintComponent(g);                  // the JLabel draws the text on top
    }

    private Color strongColor() {
        switch (kind) {
            case GOOD:
                return Theme.GOOD;
            case WARN:
                return Theme.WARN;
            case BAD:
                return Theme.BAD;
            default:
                return Theme.PRIMARY;
        }
    }

    private Color tintColor() {
        switch (kind) {
            case GOOD:
                return Theme.GOOD_TINT;
            case WARN:
                return Theme.WARN_TINT;
            case BAD:
                return Theme.BAD_TINT;
            default:
                return Theme.PRIMARY_TINT;
        }
    }
}
