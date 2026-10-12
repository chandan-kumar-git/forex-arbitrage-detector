package com.arbitrage.ui.theme;

import javax.swing.SwingUtilities;
import javax.swing.border.AbstractBorder;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;

/**
 * A thin rounded outline for text fields and spinners.
 * It turns teal while the field (or something inside it) has the keyboard focus.
 */
public class RoundedBorder extends AbstractBorder {

    private final int sidePadding;

    public RoundedBorder(int sidePadding) {
        this.sidePadding = sidePadding;
    }

    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        insets.set(2, sidePadding, 2, sidePadding);
        return insets;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Graphics2D g2 = Theme.smooth(g);
        g2.setColor(hasFocus(c) ? Theme.FOCUS : Theme.CONTROL_BORDER);
        g2.drawRoundRect(x, y, width - 1, height - 1, Theme.CONTROL_ARC, Theme.CONTROL_ARC);
        g2.dispose();
    }

    /** True when the focused component is this one or sits inside it (a spinner's text field). */
    private boolean hasFocus(Component c) {
        Component focused = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        return focused != null && SwingUtilities.isDescendingFrom(focused, c);
    }
}
