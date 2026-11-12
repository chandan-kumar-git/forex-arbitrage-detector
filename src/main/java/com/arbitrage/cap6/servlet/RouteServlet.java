package com.arbitrage.cap6.servlet;

import com.arbitrage.cap7.service.ConflictException;
import com.arbitrage.cap7.service.NotFoundException;
import com.arbitrage.cap7.service.RouteService;
import com.arbitrage.service.CurrencyService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/** "Send money" page: pick two currencies and an amount; shows the cheapest route and the saving. */
@WebServlet(name = "RouteServlet", urlPatterns = "/portal/route")
public class RouteServlet extends BaseServlet {

    private RouteService routes;
    private CurrencyService currencies;

    @Override
    public void init() {
        routes = bean(RouteService.class);
        currencies = bean(CurrencyService.class);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        List<String> codes = currencies.all().stream().filter(c -> c.active()).map(c -> c.code()).toList();
        String from = param(req, "from").isEmpty() ? "INR" : param(req, "from").toUpperCase();
        String to = param(req, "to").isEmpty() ? "JPY" : param(req, "to").toUpperCase();
        String amountText = param(req, "amount").isEmpty() ? "50000" : param(req, "amount");
        req.setAttribute("codes", codes);
        req.setAttribute("from", from);
        req.setAttribute("to", to);
        req.setAttribute("amount", amountText);
        if (req.getParameter("from") != null) {
            try {
                double amount = Double.parseDouble(amountText);
                if (amount <= 0) {
                    throw new NumberFormatException();
                }
                RouteService.Route route = routes.plan(from, to, amount);
                // EL reads JavaBean getters or Map keys, so the record is passed as a Map
                java.util.Map<String, Object> view = new java.util.LinkedHashMap<>();
                view.put("received", route.received());
                view.put("directReceived", route.directReceived());
                view.put("saving", route.saving());
                view.put("hops", route.path().size() - 1);
                req.setAttribute("route", view);
                req.setAttribute("pathText", String.join(" → ", route.path()));
                req.setAttribute("better", route.saving() > route.directReceived() * 1e-9);
            } catch (NumberFormatException e) {
                req.setAttribute("error", "Amount must be a number above 0, like 50000.");
            } catch (NotFoundException | ConflictException e) {
                req.setAttribute("error", e.getMessage());
            }
        }
        render(req, resp, "route");
    }
}
