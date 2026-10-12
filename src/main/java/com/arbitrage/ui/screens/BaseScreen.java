package com.arbitrage.ui.screens;

import com.arbitrage.ui.TopBar;
import com.arbitrage.ui.UiServices;
import com.arbitrage.ui.theme.Theme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;

/**
 * The parent of every screen: it draws the title and subtitle and gives the
 * screens the same padding. Each screen fills in onShow() in its own way.
 */
public abstract class BaseScreen extends JPanel {

    private static final Logger log = LoggerFactory.getLogger(BaseScreen.class);

    protected final UiServices services;
    protected final TopBar topBar;
    private final String title;

    protected BaseScreen(String title, String subtitle, UiServices services, TopBar topBar) {
        this.title = title;
        this.services = services;
        this.topBar = topBar;
        setLayout(new BorderLayout(0, Theme.S4));
        setBackground(Theme.APP_BG);
        setBorder(BorderFactory.createEmptyBorder(Theme.S5, Theme.S5, Theme.S5, Theme.S5));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(Theme.TITLE);
        titleLabel.setForeground(Theme.INK);
        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(Theme.SUBTITLE);
        subtitleLabel.setForeground(Theme.INK_2);

        JPanel header = new JPanel(new BorderLayout(0, Theme.S1));
        header.setOpaque(false);
        header.add(titleLabel, BorderLayout.NORTH);
        header.add(subtitleLabel, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);
    }

    public String title() {
        return title;
    }

    /** Called when the user opens this screen. */
    public abstract void onShow();

    /** Called once a second while this screen is visible. Screens with live data override it. */
    public void refresh() {
    }

    protected void setBody(JComponent body) {
        add(body, BorderLayout.CENTER);
    }

    /** Shows the problem in plain words and writes the details to the log. */
    protected void showError(Exception e) {
        log.warn("{} screen: {}", title, e.getMessage(), e);
        JOptionPane.showMessageDialog(this, e.getMessage(), "Something went wrong", JOptionPane.ERROR_MESSAGE);
    }
}
