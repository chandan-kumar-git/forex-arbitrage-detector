package com.arbitrage.cap6.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/** POST only (with the CSRF token): ends the session and goes back to the login page. */
@WebServlet(name = "LogoutServlet", urlPatterns = "/portal/logout")
public class LogoutServlet extends BaseServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!validCsrf(req)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "The form expired. Reload the page and try again.");
            return;
        }
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();                 // removes the user and every other session attribute
        }
        redirect(req, resp, "/portal/login", "You are logged out.");
    }
}
