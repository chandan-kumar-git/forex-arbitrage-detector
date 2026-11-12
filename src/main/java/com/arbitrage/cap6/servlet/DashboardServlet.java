package com.arbitrage.cap6.servlet;

import com.arbitrage.cap6.auth.User;
import com.arbitrage.cap6.listener.PortalListener;
import com.arbitrage.cap6.view.OpportunityView;
import com.arbitrage.cap7.service.AlertService;
import com.arbitrage.db.DatabaseConnection;
import com.arbitrage.engine.OpportunityStore;
import com.arbitrage.engine.TickStats;
import com.arbitrage.feed.RateFeedManager;
import com.arbitrage.feed.RateStore;
import com.arbitrage.feed.TickScheduler;
import com.arbitrage.model.Opportunity;
import com.arbitrage.model.RateSnapshot;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/**
 * The portal home: the live engine's numbers, the newest loops and this user's fired alerts.
 * Controller only: it collects data into request attributes and forwards to dashboard.jsp.
 */
@WebServlet(name = "DashboardServlet", urlPatterns = {"/portal", "/portal/"})
public class DashboardServlet extends BaseServlet {

    private TickStats stats;
    private OpportunityStore store;
    private RateStore rates;
    private TickScheduler scheduler;
    private RateFeedManager feed;
    private DatabaseConnection database;
    private AlertService alerts;

    @Override
    public void init() {
        stats = bean(TickStats.class);
        store = bean(OpportunityStore.class);
        rates = bean(RateStore.class);
        scheduler = bean(TickScheduler.class);
        feed = bean(RateFeedManager.class);
        database = bean(DatabaseConnection.class);
        alerts = bean(AlertService.class);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = currentUser(req);
        List<OpportunityView> recent = store.all().stream().limit(8).map(OpportunityView::of).toList();
        Opportunity latest = store.latest();
        RateSnapshot snapshot = rates.getLatest();

        req.setAttribute("ticks", stats.ticks());
        req.setAttribute("found", stats.found());
        req.setAttribute("bestProfit", stats.bestProfit());
        req.setAttribute("bestPath", stats.bestPath());
        req.setAttribute("avgMicros", stats.averageMicros());
        req.setAttribute("paused", scheduler.isPaused());
        req.setAttribute("source", feed.mode().name());
        req.setAttribute("dbOnline", database.isOnline());
        req.setAttribute("currencyCount", snapshot == null ? 0 : snapshot.size());
        req.setAttribute("latest", latest == null ? null : OpportunityView.of(latest));
        req.setAttribute("recent", recent);
        req.setAttribute("hits", com.arbitrage.cap6.view.HitView.of(alerts.recentHits(user.getUsername())));
        req.setAttribute("activeSessions", getServletContext().getAttribute(PortalListener.ACTIVE_SESSIONS));
        render(req, resp, "dashboard");
    }
}
