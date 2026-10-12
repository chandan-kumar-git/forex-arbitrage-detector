package com.arbitrage.ui.theme;

import javax.swing.JButton;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * A button that paints its own rounded background.
 * PRIMARY is the main action, SECONDARY a normal action, DANGER a destructive one.
 */
public class AppButton extends JButton {

    public enum Variant { PRIMARY, SECONDARY, DANGER }

    private Variant variant;
    private boolean hover = false;

    public AppButton(String text, Variant variant) {
        super(text);
        this.variant = variant;
        setFont(Theme.BODY_BOLD);
        setContentAreaFilled(false);              // switch off the Nimbus painting
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hover = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hover = false;
                repaint();
            }
        });
    }

    public void setVariant(Variant variant) {
        this.variant = variant;
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics metrics = getFontMetrics(getFont());
        return new Dimension(metrics.stringWidth(getText()) + 2 * Theme.S4, Theme.BUTTON_HEIGHT);
    }

    /** Minimum and maximum are the same as preferred, so no layout squeezes or stretches the button. */
    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Theme.smooth(g);
        if (!isEnabled()) {
            g2.setComposite(AlphaComposite.SrcOver.derive(0.45f));   // disabled = faded
        }
        RoundRectangle2D shape = new RoundRectangle2D.Float(
                0.5f, 0.5f, getWidth() - 1, getHeight() - 1, Theme.CONTROL_ARC, Theme.CONTROL_ARC);
        g2.setColor(fillColor());
        g2.fill(shape);
        g2.setColor(isFocusOwner() ? Theme.FOCUS : borderColor());
        g2.setStroke(new BasicStroke(isFocusOwner() ? 2f : 1f));
        g2.draw(shape);

        FontMetrics metrics = g2.getFontMetrics(getFont());
        int x = (getWidth() - metrics.stringWidth(getText())) / 2;
        int y = (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
        g2.setFont(getFont());
        g2.setColor(variant == Variant.PRIMARY ? Color.WHITE : borderOrInk());
        g2.drawString(getText(), x, y);
        g2.dispose();
    }

    private Color fillColor() {
        boolean lit = hover && isEnabled();
        if (variant == Variant.PRIMARY) {
            return lit ? Theme.PRIMARY_HOVER : Theme.PRIMARY;
        }
        if (variant == Variant.DANGER) {
            return lit ? Theme.BAD_TINT : Theme.SURFACE;
        }
        return lit ? Theme.APP_BG : Theme.SURFACE;
    }

    private Color borderColor() {
        if (variant == Variant.PRIMARY) {
            return fillColor();
        }
        return variant == Variant.DANGER ? Theme.BAD : Theme.CONTROL_BORDER;
    }

    /** Text colour of the two white variants. */
    private Color borderOrInk() {
        return variant == Variant.DANGER ? Theme.BAD : Theme.INK;
    }
}
