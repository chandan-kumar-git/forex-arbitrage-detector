package com.arbitrage;

import com.arbitrage.service.StartupService;
import com.arbitrage.ui.MainWindow;
import com.arbitrage.ui.UiServices;
import com.arbitrage.ui.theme.Theme;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.swing.SwingUtilities;

/**
 * Starting point of the Arbitrage Desktop program.
 * Spring creates all the @Component objects, then the Swing window opens.
 */
@SpringBootApplication
@EnableScheduling
public class ArbitrageApplication {

    public static void main(String[] args) {
        // Smooth text. These two lines must run before any Swing class is used.
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        Theme.install();

        // headless(false): Spring Boot must not switch off the graphical desktop.
        ConfigurableApplicationContext context =
                new SpringApplicationBuilder(ArbitrageApplication.class).headless(false).run(args);
        context.getBean(StartupService.class).start();

        // Swing rule: windows are created and shown on the Event Dispatch Thread (EDT).
        UiServices services = context.getBean(UiServices.class);
        SwingUtilities.invokeLater(() -> new MainWindow(services).setVisible(true));
    }
}
