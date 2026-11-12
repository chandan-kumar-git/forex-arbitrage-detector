package com.arbitrage.cap6.servlet;

import com.arbitrage.cap6.auth.PasswordHasher;
import com.arbitrage.cap6.auth.User;
import com.arbitrage.cap6.auth.UserDao;
import com.arbitrage.db.DatabaseConnection;
import com.arbitrage.db.DatabaseException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * GET shows the login form; POST checks the password and starts a session.
 * Demonstrates: HttpSession, session fixation protection, cookies ("remember my username").
 */
@WebServlet(name = "LoginServlet", urlPatterns = "/portal/login")
public class LoginServlet extends BaseServlet {

    static final String REMEMBER_COOKIE = "arbRememberUser";
    private static final int THIRTY_DAYS = 30 * 24 * 60 * 60;

    private UserDao users;
    private DatabaseConnection database;

    /** Servlet lifecycle: init() runs once, before the first request. */
    @Override
    public void init() {
        users = bean(UserDao.class);
        database = bean(DatabaseConnection.class);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (currentUser(req) != null) {
            redirect(req, resp, "/portal", null);
            return;
        }
        req.setAttribute("rememberedUser", rememberedUsername(req));
        req.setAttribute("next", param(req, "next"));
        render(req, resp, "login");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String username = param(req, "username").toLowerCase();
        String password = req.getParameter("password") == null ? "" : req.getParameter("password");
        String next = param(req, "next");
        req.setAttribute("rememberedUser", username);
        req.setAttribute("next", next);
        try {
            Optional<UserDao.StoredUser> found = users.findByUsername(username);
            if (found.isEmpty() || !PasswordHasher.matches(password, found.get().passwordHash())) {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                req.setAttribute("error", "Wrong username or password.");
                render(req, resp, "login");
                return;
            }
            User user = found.get().user();
            startSession(req, user);
            users.updateLastLogin(user.getId(), LocalDateTime.now());
            rememberUsername(resp, req.getParameter("remember") != null ? username : null);
            // only follow "next" inside the portal, so the login page cannot send users to other sites
            String target = next.startsWith("/portal") && !next.startsWith("//") ? next : "/portal";
            redirect(req, resp, target, "Welcome back, " + user.getDisplayName() + ".");
        } catch (DatabaseException e) {
            resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            req.setAttribute("error", database.isOnline() ? e.getMessage()
                    : "MySQL is not running, so accounts cannot be checked. Start MySQL and try again.");
            render(req, resp, "login");
        }
    }

    /** A new session id after login: an id seen before login (session fixation) becomes useless. */
    private void startSession(HttpServletRequest req, User user) {
        HttpSession old = req.getSession(false);
        if (old != null) {
            old.invalidate();
        }
        HttpSession session = req.getSession(true);
        session.setAttribute(USER, user);
        csrfToken(session);
    }

    /** The cookie keeps only the username, never the password. */
    private void rememberUsername(HttpServletResponse resp, String username) {
        Cookie cookie = new Cookie(REMEMBER_COOKIE, username == null ? "" : username);
        cookie.setHttpOnly(true);
        cookie.setPath("/portal");
        cookie.setMaxAge(username == null ? 0 : THIRTY_DAYS);   // max age 0 deletes the cookie
        resp.addCookie(cookie);
    }

    private static String rememberedUsername(HttpServletRequest req) {
        if (req.getCookies() == null) {
            return "";
        }
        for (Cookie cookie : req.getCookies()) {
            if (REMEMBER_COOKIE.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return "";
    }
}
