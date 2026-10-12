package com.arbitrage.server;

import com.arbitrage.ui.theme.Theme;

import javax.swing.SwingUtilities;

/**
 * Starting point of the Rate Feed Server, the second program of the project.
 * It is plain Java: no Spring and no database.
 */
public class FeedServerApp {

    public static void main(String[] args) {
        // Smooth text. These two lines must run before any Swing class is used.
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        Theme.install();
        SwingUtilities.invokeLater(() -> new ServerWindow().setVisible(true));
    }
}
