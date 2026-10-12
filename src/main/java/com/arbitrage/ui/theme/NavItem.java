package com.arbitrage.ui.theme;

import javax.swing.JButton;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * One row of the sidebar: a small symbol and a label.
 * The active row gets a lighter background and a gold bar on the left.
 */
public class NavItem extends JButton {

    private static final int HEIGHT = 44;
    private static final Color HOVER = new Color(255, 255, 255, 20);

    private final String glyph;
    private boolean active = false;
    private boolean hover = false;

    public NavItem(String glyph, String label) {
        super(label);
        this.glyph = glyph;
        setFont(Theme.BODY);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setAlignmentX(LEFT_ALIGNMENT);
        setPreferredSize(new Dimension(Theme.SIDEBAR_WIDTH, HEIGHT));
        setMaximumSize(new Dimension(Theme.SIDEBAR_WIDTH, HEIGHT));
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

    public void setActive(boolean active) {
        this.active = active;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Theme.smooth(g);
        if (active) {
            g2.setColor(Theme.SIDEBAR_ACTIVE);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(Theme.ACCENT);
            g2.fillRect(0, 0, 3, getHeight());
        } else if (hover) {
            g2.setColor(HOVER);
            g2.fillRect(0, 0, getWidth(), getHeight());
        }
        if (isFocusOwner() && !active) {          // keyboard focus: a thin outline
            g2.setColor(Theme.FOCUS);
            g2.drawRect(2, 2, getWidth() - 5, getHeight() - 5);
        }
        g2.setColor(active ? Color.WHITE : Theme.SIDEBAR_TEXT);

        g2.setFont(Theme.GLYPH);
        FontMetrics glyphMetrics = g2.getFontMetrics();
        g2.drawString(glyph, Theme.S5, baseline(glyphMetrics));

        g2.setFont(active ? Theme.BODY_BOLD : Theme.BODY);
        g2.drawString(getText(), Theme.S5 + 28, baseline(g2.getFontMetrics()));
        g2.dispose();
    }

    /** The y position that puts one line of text in the vertical middle of the row. */
    private int baseline(FontMetrics metrics) {
        return (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
    }
}
