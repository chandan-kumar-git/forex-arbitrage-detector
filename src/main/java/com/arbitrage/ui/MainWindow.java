package com.arbitrage.ui;

import com.arbitrage.feed.FeedStatus;
import com.arbitrage.ui.screens.BaseScreen;
import com.arbitrage.ui.screens.CurrenciesScreen;
import com.arbitrage.ui.screens.DashboardScreen;
import com.arbitrage.ui.screens.FeedScreen;
import com.arbitrage.ui.screens.HistoryScreen;
import com.arbitrage.ui.screens.SettingsScreen;
import com.arbitrage.ui.theme.BackgroundTask;
import com.arbitrage.ui.theme.Theme;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The main window: sidebar on the left, top bar above, and the screens in the middle.
 * A CardLayout shows one screen at a time. One timer refreshes the visible screen every second.
 */
public class MainWindow extends JFrame {

    private static final int REFRESH_MILLIS = 1000;

    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final Map<String, BaseScreen> screens = new LinkedHashMap<>();   // screen name -> screen
    private final Sidebar sidebar = new Sidebar(this::showScreen);
    private final UiServices services;
    private final TopBar topBar;
    private BaseScreen visibleScreen;
    private boolean feedLostDialogShown = false;

    public MainWindow(UiServices services) {
        super("Forex Arbitrage Detector");
        this.services = services;
        topBar = new TopBar(services);
        addScreen("◉", new DashboardScreen(services, topBar));
        addScreen("☰", new HistoryScreen(services, topBar));
        addScreen("¤", new CurrenciesScreen(services, topBar));
        addScreen("⇄", new FeedScreen(services, topBar));
        addScreen("⚙", new SettingsScreen(services, topBar));

        JPanel right = new JPanel(new BorderLayout());
        right.add(topBar, BorderLayout.NORTH);
        right.add(content, BorderLayout.CENTER);
        setLayout(new BorderLayout());
        add(sidebar, BorderLayout.WEST);
        add(right, BorderLayout.CENTER);
        content.setBackground(Theme.APP_BG);

        setSize(1280, 800);
        setMinimumSize(new Dimension(1100, 700));
        setLocationRelativeTo(null);              // centre of the screen
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeApp();
            }
        });

        showScreen("Dashboard");
        // A Swing Timer fires on the EDT, so refresh() may touch components safely.
        new Timer(REFRESH_MILLIS, e -> refresh()).start();
    }

    /** Switches to the screen with this name and lets it load its data. */
    public void showScreen(String name) {
        visibleScreen = screens.get(name);
        cards.show(content, name);
        sidebar.setActive(name);
        visibleScreen.onShow();                   // each screen decides what "show" means for it
    }

    private void addScreen(String glyph, BaseScreen screen) {
        screens.put(screen.title(), screen);
        content.add(screen, screen.title());
        sidebar.addItem(glyph, screen.title());
    }

    private void refresh() {
        topBar.refresh();
        visibleScreen.refresh();
        checkFeedLost();
    }

    /** When the feed server goes away, ask once if the app should switch back to the simulator. */
    private void checkFeedLost() {
        if (!services.feed().isFeedLost()) {
            feedLostDialogShown = false;
            return;
        }
        if (feedLostDialogShown) {
            return;
        }
        feedLostDialogShown = true;
        int answer = JOptionPane.showConfirmDialog(this,
                "The connection to the feed server was lost.\nSwitch to Local simulator?",
                "Feed disconnected", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer == JOptionPane.YES_OPTION) {
            BackgroundTask.run(() -> {
                services.feed().useLocal();
                return true;
            }, done -> refresh(), error -> refresh());
        }
    }

    /** Asks before closing while the socket feed is connected, then says BYE and exits. */
    private void closeApp() {
        if (services.feed().socket().status() == FeedStatus.CONNECTED) {
            int answer = JOptionPane.showConfirmDialog(this,
                    "The socket feed is still connected. Disconnect and exit?",
                    "Exit", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (answer != JOptionPane.YES_OPTION) {
                return;
            }
        }
        BackgroundTask.run(() -> {
            services.feed().disconnect();
            return true;
        }, done -> System.exit(0), error -> System.exit(0));   // exit also stops the scheduler thread
    }
}
