package com.arbitrage.ui.theme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;

/**
 * A white panel with rounded corners and a thin border.
 * It can have a title row with a place for buttons on the right.
 */
public class Card extends JPanel {

    private final JLabel titleLabel = new JLabel();
    private final JPanel actions = Ui.row(FlowLayout.RIGHT);

    /** Pass null as the title for a card without a title row. */
    public Card(String title) {
        setLayout(new BorderLayout(0, Theme.S3));
        setOpaque(false);                         // we paint the rounded background ourselves
        int pad = Theme.CARD_PADDING;
        setBorder(BorderFactory.createEmptyBorder(pad, pad, pad, pad));
        if (title != null) {
            titleLabel.setText(title);
            titleLabel.setFont(Theme.CARD_TITLE);
            titleLabel.setForeground(Theme.INK);
            JPanel header = new JPanel(new BorderLayout(Theme.S3, 0));
            header.setOpaque(false);
            header.add(titleLabel, BorderLayout.CENTER);
            header.add(actions, BorderLayout.EAST);
            add(header, BorderLayout.NORTH);
        }
    }

    public void setTitle(String title) {
        titleLabel.setText(title);
    }

    /** Puts the main content into the middle of the card. */
    public void setContent(JComponent content) {
        add(content, BorderLayout.CENTER);
    }

    /** Adds a button (or any component) to the right side of the title row. */
    public void addAction(JComponent action) {
        if (actions.getComponentCount() > 0) {
            actions.add(Box.createHorizontalStrut(Theme.S2));
        }
        actions.add(action);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Theme.smooth(g);
        RoundRectangle2D shape = new RoundRectangle2D.Float(
                0.5f, 0.5f, getWidth() - 1, getHeight() - 1, Theme.CARD_ARC, Theme.CARD_ARC);
        g2.setColor(Theme.SURFACE);
        g2.fill(shape);
        g2.setColor(Theme.BORDER);
        g2.draw(shape);
        g2.dispose();
    }
}
