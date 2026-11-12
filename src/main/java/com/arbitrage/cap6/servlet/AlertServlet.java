package com.arbitrage.cap6.servlet;

import com.arbitrage.cap6.auth.User;
import com.arbitrage.cap7.dto.Dtos.AlertRequest;
import com.arbitrage.cap7.service.AlertService;
import com.arbitrage.cap7.service.NotFoundException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.CannotCreateTransactionException;

import java.io.IOException;

/** The logged-in user's profit alerts: list, add, switch on/off, delete (stored with JPA). */
@WebServlet(name = "AlertServlet", urlPatterns = "/portal/alerts")
public class AlertServlet extends BaseServlet {

    private AlertService alerts;

    @Override
    public void init() {
        alerts = bean(AlertService.class);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = currentUser(req);
        try {
            req.setAttribute("alerts", alerts.list(user.getUsername()).stream().map(AlertBean::new).toList());
        } catch (CannotCreateTransactionException | DataAccessException e) {
            req.setAttribute("dbError", "The database is not available, so alerts cannot be loaded. Is MySQL running?");
        }
        req.setAttribute("hits", com.arbitrage.cap6.view.HitView.of(alerts.recentHits(user.getUsername())));
        render(req, resp, "alerts");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!validCsrf(req)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "The form expired. Reload the page and try again.");
            return;
        }
        User user = currentUser(req);
        String action = param(req, "action");
        String message;
        try {
            message = switch (action) {
                case "add" -> add(req, user);
                case "on", "off" -> {
                    alerts.setActive(Long.parseLong(param(req, "id")), action.equals("on"));
                    yield "Alert switched " + action + ".";
                }
                case "delete" -> {
                    alerts.delete(Long.parseLong(param(req, "id")));
                    yield "Alert deleted.";
                }
                default -> "Unknown action.";
            };
        } catch (NumberFormatException | NotFoundException e) {
            message = "That alert does not exist any more.";
        } catch (CannotCreateTransactionException | DataAccessException e) {
            message = "The database is not available. Is MySQL running?";
        }
        redirect(req, resp, "/portal/alerts", message);
    }

    private String add(HttpServletRequest req, User user) {
        double min;
        try {
            min = Double.parseDouble(param(req, "minProfit"));
        } catch (NumberFormatException e) {
            return "Minimum profit must be a number, like 0.5.";
        }
        String code = param(req, "currencyCode").toUpperCase();
        if (min < 0 || min > 100) {
            return "Minimum profit must be between 0 and 100 %.";
        }
        if (!code.isEmpty() && !code.matches("[A-Z]{3}")) {
            return "Currency must be 3 letters, like INR, or left empty.";
        }
        alerts.create(new AlertRequest(min, code.isEmpty() ? null : code, user.getUsername()), user.getUsername());
        return "Alert added: loops of " + min + " % or more" + (code.isEmpty() ? "." : " through " + code + ".");
    }

    /** An alert as a JavaBean for JSP EL (the REST DTO is a record). */
    public static class AlertBean {
        private final com.arbitrage.cap7.dto.Dtos.AlertDto a;

        AlertBean(com.arbitrage.cap7.dto.Dtos.AlertDto a) {
            this.a = a;
        }

        public long getId() {
            return a.id();
        }

        public double getMinProfit() {
            return a.minProfit();
        }

        public String getCurrencyCode() {
            return a.currencyCode() == null ? "any" : a.currencyCode();
        }

        public boolean isActive() {
            return a.active();
        }

        public int getTriggerCount() {
            return a.triggerCount();
        }

        public String getLastPath() {
            return a.lastPath() == null ? "" : a.lastPath();
        }

        public String getLastTriggeredAt() {
            return com.arbitrage.cap6.view.OpportunityView.format(a.lastTriggeredAt());
        }
    }
}
