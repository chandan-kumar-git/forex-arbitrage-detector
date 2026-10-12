package com.arbitrage.ui;

import com.arbitrage.ui.theme.NavItem;
import com.arbitrage.ui.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The dark panel on the left: the app name, one NavItem per screen, and the version.
 * Clicking an item tells the main window which screen to show.
 */
public class Sidebar extends JPanel {

    private final Map<String, NavItem> items = new LinkedHashMap<>();   // screen name -> its nav row
    private final JPanel navList = new JPanel();
    private final Consumer<String> onNavigate;

    public Sidebar(Consumer<String> onNavigate) {
        this.onNavigate = onNavigate;
        setLayout(new BorderLayout());
        setBackground(Theme.SIDEBAR_BG);
        setPreferredSize(new Dimension(Theme.SIDEBAR_WIDTH, 0));

        navList.setLayout(new BoxLayout(navList, BoxLayout.Y_AXIS));   // items stacked top to bottom
        navList.setOpaque(false);

        add(buildWordmark(), BorderLayout.NORTH);
        add(navList, BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
    }

    public void addItem(String glyph, String screenName) {
        NavItem item = new NavItem(glyph, screenName);
        item.addActionListener(e -> onNavigate.accept(screenName));
        items.put(screenName, item);
        navList.add(item);
    }

    /** Marks one item as the current screen. */
    public void setActive(String screenName) {
        for (Map.Entry<String, NavItem> entry : items.entrySet()) {
            entry.getValue().setActive(entry.getKey().equals(screenName));
        }
    }

    private JPanel buildWordmark() {
        JLabel small = new JLabel("Forex");
        small.setFont(Theme.BODY);
        small.setForeground(Theme.SIDEBAR_TEXT);
        JLabel big = new JLabel("Arbitrage");
        big.setFont(Theme.font(18, true));
        big.setForeground(Color.WHITE);

        JPanel wordmark = new JPanel(new GridLayout(2, 1, 0, 2));
        wordmark.setOpaque(false);
        wordmark.setBorder(BorderFactory.createEmptyBorder(Theme.S5, Theme.S5, Theme.S5, Theme.S5));
        wordmark.add(small);
        wordmark.add(big);
        return wordmark;
    }

    private JLabel buildFooter() {
        JLabel footer = new JLabel("v1.0 · CAP-5");
        footer.setFont(Theme.SMALL);
        footer.setForeground(Theme.SIDEBAR_TEXT);
        footer.setBorder(BorderFactory.createEmptyBorder(Theme.S4, Theme.S5, Theme.S4, Theme.S5));
        return footer;
    }
}
