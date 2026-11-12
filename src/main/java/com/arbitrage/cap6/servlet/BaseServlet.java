package com.arbitrage.cap6.servlet;

import com.arbitrage.cap6.auth.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.context.support.WebApplicationContextUtils;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Shared helpers for the portal servlets (the Controllers of MVC):
 * get a Spring bean, forward to a JSP view, read the logged-in user, flash messages and CSRF tokens.
 */
public abstract class BaseServlet extends HttpServlet {

    public static final String USER = "user";
    public static final String CSRF = "csrf";
    private static final String FLASH = "flash";
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * The servlet container (Tomcat) creates servlets, not Spring, so the Spring services
     * are looked up from the ServletContext, where Spring Boot stores its application context.
     */
    protected <T> T bean(Class<T> type) {
        return WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext()).getBean(type);
    }

    /** MVC: hand the request to the JSP view. The browser URL does not change (server-side forward). */
    protected void render(HttpServletRequest req, HttpServletResponse resp, String view)
            throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session != null && session.getAttribute(FLASH) != null) {
            req.setAttribute("flash", session.getAttribute(FLASH));
            session.removeAttribute(FLASH);       // a flash message is shown once
        }
        req.setAttribute("page", view);
        req.getRequestDispatcher("/WEB-INF/jsp/" + view + ".jsp").forward(req, resp);
    }

    /** Post / Redirect / Get: after a form POST, redirect so refreshing the page does not repeat it. */
    protected void redirect(HttpServletRequest req, HttpServletResponse resp, String path, String flash)
            throws IOException {
        if (flash != null) {
            req.getSession().setAttribute(FLASH, flash);
        }
        resp.sendRedirect(req.getContextPath() + path);
    }

    protected User currentUser(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        return session == null ? null : (User) session.getAttribute(USER);
    }

    /** One random token per session; every form sends it back, so other sites cannot post for the user. */
    public static String csrfToken(HttpSession session) {
        String token = (String) session.getAttribute(CSRF);
        if (token == null) {
            byte[] bytes = new byte[16];
            RANDOM.nextBytes(bytes);
            token = HexFormat.of().formatHex(bytes);
            session.setAttribute(CSRF, token);
        }
        return token;
    }

    protected boolean validCsrf(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        String expected = session == null ? null : (String) session.getAttribute(CSRF);
        return expected != null && expected.equals(req.getParameter(CSRF));
    }

    /** Reads a request parameter, trimmed; never null. */
    protected static String param(HttpServletRequest req, String name) {
        String value = req.getParameter(name);
        return value == null ? "" : value.trim();
    }
}
