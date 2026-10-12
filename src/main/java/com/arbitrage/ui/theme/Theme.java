package com.arbitrage.ui.theme;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.Painter;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * The design tokens of the app: every colour, font and spacing value lives here,
 * so all screens look the same. install() switches Swing to the tuned Nimbus look.
 */
public final class Theme {

    public static final Color APP_BG = new Color(0xF4F6F8);
    public static final Color SURFACE = Color.WHITE;
    public static final Color BORDER = new Color(0xE3E8EB);
    public static final Color INK = new Color(0x1E2933);
    public static final Color INK_2 = new Color(0x56626B);
    public static final Color MUTE = new Color(0x86929A);
    public static final Color PRIMARY = new Color(0x1D5B76);
    public static final Color PRIMARY_HOVER = new Color(0x174B61);
    public static final Color PRIMARY_TINT = new Color(0xE6F0F4);
    public static final Color SIDEBAR_BG = new Color(0x143F52);
    public static final Color SIDEBAR_TEXT = new Color(0xCFE2EA);
    public static final Color SIDEBAR_ACTIVE = new Color(0x1D5B76);
    public static final Color ACCENT = new Color(0xE2B04A);
    public static final Color GOOD = new Color(0x1E7A55);
    public static final Color GOOD_TINT = new Color(0xE3F2EA);
    public static final Color WARN = new Color(0xA86F00);
    public static final Color WARN_TINT = new Color(0xFBF1DC);
    public static final Color BAD = new Color(0xB23A2E);
    public static final Color BAD_TINT = new Color(0xFBE8E5);
    public static final Color CYCLE = new Color(0xC0392B);
    public static final Color EDGE = new Color(0xD3DADF);
    public static final Color NODE_FILL = new Color(0xF0F6F8);
    public static final Color FOCUS = new Color(0x5E93AA);
    public static final Color CONTROL_BORDER = new Color(0xC9D3D9);

    /** Spacing scale in pixels. */
    public static final int S1 = 4;
    public static final int S2 = 8;
    public static final int S3 = 12;
    public static final int S4 = 16;
    public static final int S5 = 24;
    public static final int S6 = 32;
    public static final int CARD_PADDING = 20;
    public static final int CARD_ARC = 24;        // corner radius 12
    public static final int CONTROL_ARC = 16;     // corner radius 8
    public static final int SIDEBAR_WIDTH = 220;
    public static final int TOP_BAR_HEIGHT = 56;
    public static final int ROW_HEIGHT = 34;
    public static final int BUTTON_HEIGHT = 36;
    public static final int FIELD_HEIGHT = 34;

    private static final String SANS = firstInstalled(
            "Inter", "Segoe UI", "SF Pro Text", "Helvetica Neue", "Arial", Font.SANS_SERIF);
    private static final String MONOSPACE = firstInstalled(
            "JetBrains Mono", "Consolas", "Menlo", Font.MONOSPACED);
    private static final String SYMBOLS = firstInstalled(
            "Segoe UI Symbol", "Apple Symbols", "DejaVu Sans", Font.SANS_SERIF);

    public static final Font TITLE = font(22, true);
    public static final Font SUBTITLE = font(13, false);
    public static final Font CARD_TITLE = font(15, true);
    public static final Font BODY = font(13, false);
    public static final Font BODY_BOLD = font(13, true);
    public static final Font SMALL = font(12, false);
    public static final Font SMALL_BOLD = font(12, true);
    public static final Font STAT = font(28, true);
    public static final Font MONO = new Font(MONOSPACE, Font.PLAIN, 12);
    public static final Font MONO_SMALL = new Font(MONOSPACE, Font.PLAIN, 11);
    public static final Font GLYPH = new Font(SYMBOLS, Font.PLAIN, 16);

    private Theme() {
    }

    public static Font font(int size, boolean bold) {
        return new Font(SANS, bold ? Font.BOLD : Font.PLAIN, size);
    }

    /** Sets the Nimbus look-and-feel with our colours. Call once, before any window is created. */
    public static void install() {
        UIManager.put("nimbusBase", PRIMARY);
        UIManager.put("nimbusBlueGrey", CONTROL_BORDER);
        UIManager.put("control", APP_BG);
        UIManager.put("nimbusFocus", FOCUS);
        UIManager.put("nimbusSelectionBackground", PRIMARY);
        UIManager.put("nimbusSelectedText", Color.WHITE);
        UIManager.put("text", INK);
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                }
            }
        } catch (Exception e) {
            // Nimbus is missing: Swing keeps its cross-platform look and the app still works.
            System.err.println("Nimbus look-and-feel not available: " + e.getMessage());
        }
        UIManager.getLookAndFeelDefaults().put("defaultFont", BODY);
        flatten("Spinner:Panel:\"Spinner.formattedTextField\"",
                "Enabled", "Disabled", "Focused", "Selected", "Focused+Selected");
        quietScrollBars();
        flatten("ComboBox", "Enabled", "Disabled", "Focused", "MouseOver", "Pressed",
                "Focused+MouseOver", "Focused+Pressed", "Enabled+Selected", "Disabled+Pressed");
        UIManager.put("Table.alternateRowColor", new Color(0xFAFCFC));
        UIManager.put("Table.showGrid", false);
        UIManager.put("ScrollPane.border", BorderFactory.createEmptyBorder());
    }

    /**
     * Nimbus draws every part of a component with a "painter". Here the background painter
     * of one component is replaced by plain white, so RoundedBorder is its only outline.
     */
    private static void flatten(String component, String... states) {
        Painter<JComponent> plainWhite = (g, c, width, height) -> {
            g.setColor(SURFACE);
            g.fillRect(0, 0, width, height);
        };
        for (String state : states) {
            UIManager.getLookAndFeelDefaults().put(component + "[" + state + "].backgroundPainter", plainWhite);
        }
    }

    /** Scroll bars: a white track and a slim grey thumb instead of the glossy Nimbus ones. */
    private static void quietScrollBars() {
        Painter<JComponent> greyThumb = (g, c, width, height) -> {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(CONTROL_BORDER);
            g.fillRoundRect(2, 4, width - 4, height - 8, 8, 8);
        };
        flatten("ScrollBar:ScrollBarTrack", "Enabled", "Disabled");
        for (String state : new String[] {"Enabled", "MouseOver", "Pressed"}) {
            UIManager.getLookAndFeelDefaults().put(
                    "ScrollBar:ScrollBarThumb[" + state + "].backgroundPainter", greyThumb);
        }
    }

    /** A copy of the graphics with smooth shapes and text. The caller must dispose() it. */
    public static Graphics2D smooth(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        return g2;
    }

    /** Returns the first font family of the list that is installed on this computer. */
    private static String firstInstalled(String... families) {
        String[] names = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        Set<String> installed = new HashSet<>(Arrays.asList(names));
        for (String family : families) {
            if (installed.contains(family)) {
                return family;
            }
        }
        return families[families.length - 1];
    }
}
