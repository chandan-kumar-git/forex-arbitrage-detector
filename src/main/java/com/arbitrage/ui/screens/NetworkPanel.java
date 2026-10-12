package com.arbitrage.ui.screens;

import com.arbitrage.model.Opportunity;
import com.arbitrage.model.RateSnapshot;
import com.arbitrage.ui.theme.Formats;
import com.arbitrage.ui.theme.Theme;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Path2D;
import java.util.List;

/**
 * Draws the currency network: currencies on a circle, grey lines for every pair,
 * and thick red arrows with rate labels for the profitable cycle.
 */
public class NetworkPanel extends JPanel {

    private static final int NODE_RADIUS = 28;
    private static final int LEGEND_HEIGHT = 28;
    private static final Color CYCLE_GLOW = new Color(0xC0, 0x39, 0x2B, 64);   // CYCLE at 25 % alpha

    private RateSnapshot snapshot;
    private List<String> cyclePath = List.of();

    public NetworkPanel() {
        setBackground(Theme.SURFACE);
    }

    /** Gives the panel new data. Pass null as the highlight when no cycle should be red. */
    public void update(RateSnapshot snapshot, Opportunity highlight) {
        this.snapshot = snapshot;
        this.cyclePath = highlight == null ? List.of() : highlight.path();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);                  // 1. background
        Graphics2D g2 = Theme.smooth(g);
        if (snapshot == null) {
            drawEmpty(g2, "Waiting for the first rates", "A new snapshot arrives every second.");
        } else if (snapshot.size() < 3) {
            drawEmpty(g2, "Add at least 3 active currencies", "Open Currencies to add or activate one.");
        } else {
            Point[] nodes = nodePositions();
            drawAllEdges(g2, nodes);              // 2. every pair, faint grey
            drawCycle(g2, nodes, false);          // 3. the profitable cycle: red arrows
            drawNodes(g2, nodes);                 // 4. the currencies on top
            drawCycle(g2, nodes, true);           // 5. the rate labels, so no node hides them
        }
        drawLegend(g2);                           // 6. legend
        g2.dispose();
    }

    /** Places the nodes evenly on a circle, starting at the top (angle -90 degrees). */
    private Point[] nodePositions() {
        int n = snapshot.size();
        int height = getHeight() - LEGEND_HEIGHT;
        int radius = Math.max(40, Math.min(getWidth(), height) / 2 - NODE_RADIUS - 16);
        Point[] nodes = new Point[n];
        for (int i = 0; i < n; i++) {
            double angle = Math.toRadians(-90 + 360.0 * i / n);
            int x = (int) (getWidth() / 2.0 + radius * Math.cos(angle));
            int y = (int) (height / 2.0 + radius * Math.sin(angle));
            nodes[i] = new Point(x, y);
        }
        return nodes;
    }

    private void drawAllEdges(Graphics2D g2, Point[] nodes) {
        g2.setColor(Theme.EDGE);
        g2.setStroke(new BasicStroke(1.2f));
        for (int i = 0; i < nodes.length; i++) {
            for (int j = i + 1; j < nodes.length; j++) {
                g2.drawLine(nodes[i].x, nodes[i].y, nodes[j].x, nodes[j].y);
            }
        }
    }

    /** Goes through the steps of the cycle and draws either the arrows or the rate labels. */
    private void drawCycle(Graphics2D g2, Point[] nodes, boolean labels) {
        for (int i = 0; i < cyclePath.size() - 1; i++) {
            int from = snapshot.codes().indexOf(cyclePath.get(i));
            int to = snapshot.codes().indexOf(cyclePath.get(i + 1));
            if (from < 0 || to < 0) {
                continue;                         // a currency that was just removed
            }
            if (labels) {
                drawRateLabel(g2, nodes[from], nodes[to], Formats.rate(snapshot.rate(from, to)));
            } else {
                drawArrow(g2, nodes[from], nodes[to]);
            }
        }
    }

    /** A thick line from the edge of one node to the edge of the other, with a filled arrowhead. */
    private void drawArrow(Graphics2D g2, Point from, Point to) {
        double angle = Math.atan2(to.y - from.y, to.x - from.x);
        double gap = NODE_RADIUS + 5;
        double tipX = to.x - gap * Math.cos(angle);
        double tipY = to.y - gap * Math.sin(angle);
        g2.setColor(Theme.CYCLE);
        g2.setStroke(new BasicStroke(3f));
        g2.drawLine((int) (from.x + gap * Math.cos(angle)), (int) (from.y + gap * Math.sin(angle)),
                (int) (tipX - 8 * Math.cos(angle)), (int) (tipY - 8 * Math.sin(angle)));

        Path2D head = new Path2D.Double();
        head.moveTo(tipX, tipY);
        head.lineTo(tipX - 14 * Math.cos(angle - 0.4), tipY - 14 * Math.sin(angle - 0.4));
        head.lineTo(tipX - 14 * Math.cos(angle + 0.4), tipY - 14 * Math.sin(angle + 0.4));
        head.closePath();
        g2.fill(head);
    }

    private void drawRateLabel(Graphics2D g2, Point from, Point to, String text) {
        g2.setFont(Theme.MONO_SMALL);
        FontMetrics metrics = g2.getFontMetrics();
        int width = metrics.stringWidth(text) + 12;
        int height = metrics.getHeight() + 4;
        // Middle of the arrow, moved 14 px sideways so the label does not cover the line.
        double angle = Math.atan2(to.y - from.y, to.x - from.x);
        int x = (int) ((from.x + to.x) / 2.0 - 14 * Math.sin(angle)) - width / 2;
        int y = (int) ((from.y + to.y) / 2.0 + 14 * Math.cos(angle)) - height / 2;
        g2.setColor(Theme.SURFACE);
        g2.fillRoundRect(x, y, width, height, 8, 8);
        g2.setColor(Theme.CYCLE);
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(x, y, width, height, 8, 8);
        g2.drawString(text, x + 6, y + 2 + metrics.getAscent());
    }

    private void drawNodes(Graphics2D g2, Point[] nodes) {
        g2.setFont(Theme.BODY_BOLD);
        FontMetrics metrics = g2.getFontMetrics();
        for (int i = 0; i < nodes.length; i++) {
            String code = snapshot.code(i);
            boolean inCycle = cyclePath.contains(code);
            int r = NODE_RADIUS;
            if (inCycle) {
                g2.setColor(CYCLE_GLOW);
                g2.fillOval(nodes[i].x - r - 6, nodes[i].y - r - 6, 2 * r + 12, 2 * r + 12);
            }
            g2.setColor(Theme.NODE_FILL);
            g2.fillOval(nodes[i].x - r, nodes[i].y - r, 2 * r, 2 * r);
            g2.setColor(inCycle ? Theme.CYCLE : Theme.PRIMARY);
            g2.setStroke(new BasicStroke(inCycle ? 3f : 2f));
            g2.drawOval(nodes[i].x - r, nodes[i].y - r, 2 * r, 2 * r);
            g2.setColor(Theme.INK);
            g2.drawString(code, nodes[i].x - metrics.stringWidth(code) / 2,
                    nodes[i].y - metrics.getHeight() / 2 + metrics.getAscent());
        }
    }

    private void drawLegend(Graphics2D g2) {
        int y = getHeight() - 10;
        g2.setFont(Theme.SMALL);
        g2.setColor(Theme.EDGE);
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawLine(0, y - 4, 22, y - 4);
        g2.setColor(Theme.INK_2);
        g2.drawString("exchange pair", 30, y);
        g2.setColor(Theme.CYCLE);
        g2.setStroke(new BasicStroke(3f));
        g2.drawLine(130, y - 4, 152, y - 4);
        g2.setColor(Theme.INK_2);
        g2.drawString("profitable cycle", 160, y);
    }

    private void drawEmpty(Graphics2D g2, String title, String hint) {
        int middle = (getHeight() - LEGEND_HEIGHT) / 2;
        g2.setFont(Theme.BODY_BOLD);
        g2.setColor(Theme.INK_2);
        g2.drawString(title, (getWidth() - g2.getFontMetrics().stringWidth(title)) / 2, middle - 4);
        g2.setFont(Theme.SMALL);
        g2.setColor(Theme.MUTE);
        g2.drawString(hint, (getWidth() - g2.getFontMetrics().stringWidth(hint)) / 2, middle + 16);
    }
}
