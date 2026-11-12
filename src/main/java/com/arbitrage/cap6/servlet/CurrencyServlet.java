package com.arbitrage.cap6.servlet;

import com.arbitrage.cap6.view.CurrencyView;
import com.arbitrage.db.DatabaseException;
import com.arbitrage.service.CurrencyService;
import com.arbitrage.validation.ValidationException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/**
 * Currencies page. GET lists them; POST handles add, toggle and delete, all through the same
 * CurrencyService (and validators) the Swing app uses, then redirects back with a message.
 */
@WebServlet(name = "CurrencyServlet", urlPatterns = "/portal/currencies")
public class CurrencyServlet extends BaseServlet {

    private CurrencyService currencies;

    @Override
    public void init() {
        currencies = bean(CurrencyService.class);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        List<CurrencyView> list = currencies.all().stream().map(CurrencyView::new).toList();
        req.setAttribute("currencies", list);
        render(req, resp, "currencies");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!validCsrf(req)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "The form expired. Reload the page and try again.");
            return;
        }
        String action = param(req, "action");
        String code = param(req, "code");
        try {
            String message = switch (action) {
                case "add" -> {
                    currencies.add(code, param(req, "name"), param(req, "price"), true);
                    yield code + " added.";
                }
                case "toggle" -> {
                    var current = currencies.all().stream().filter(c -> c.code().equals(code)).findFirst()
                            .orElseThrow(() -> new ValidationException("code", "Unknown currency"));
                    currencies.update(code, current.name(), String.valueOf(current.usdPrice()), !current.active());
                    yield code + (current.active() ? " deactivated." : " activated.");
                }
                case "delete" -> {
                    currencies.delete(code);
                    yield code + " deleted.";
                }
                default -> "Unknown action.";
            };
            redirect(req, resp, "/portal/currencies", message);
        } catch (ValidationException | DatabaseException e) {
            // show the form again with what the user typed and the reason in plain words
            req.setAttribute("error", e.getMessage());
            req.setAttribute("formCode", code);
            req.setAttribute("formName", param(req, "name"));
            req.setAttribute("formPrice", param(req, "price"));
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            doGet(req, resp);
        }
    }
}
