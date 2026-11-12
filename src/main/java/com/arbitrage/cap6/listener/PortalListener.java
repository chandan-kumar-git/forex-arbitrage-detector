package com.arbitrage.cap6.listener;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Listens to the web application's lifecycle: when it starts (ServletContext) and when
 * sessions are created or destroyed. The dashboard shows the live count of open sessions.
 */
@WebListener
public class PortalListener implements ServletContextListener, HttpSessionListener {

    public static final String ACTIVE_SESSIONS = "activeSessions";
    public static final String STARTED_AT = "portalStartedAt";

    private static final Logger log = LoggerFactory.getLogger(PortalListener.class);
    private final AtomicInteger active = new AtomicInteger();   // sessions start and end on many threads

    @Override
    public void contextInitialized(ServletContextEvent event) {
        event.getServletContext().setAttribute(ACTIVE_SESSIONS, active);
        event.getServletContext().setAttribute(STARTED_AT, new java.util.Date());
        log.info("Portal ready on {}", event.getServletContext().getServerInfo());
    }

    @Override
    public void sessionCreated(HttpSessionEvent event) {
        active.incrementAndGet();
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        active.decrementAndGet();                 // on logout (invalidate) or after 30 minutes idle
    }
}
