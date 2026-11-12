package com.arbitrage.cap6.servlet;

import com.arbitrage.cap6.auth.PasswordHasher;
import com.arbitrage.cap6.auth.UserDao;
import com.arbitrage.db.DatabaseException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/** GET shows the sign-up form; POST validates it on the server and creates the account. */
@WebServlet(name = "RegisterServlet", urlPatterns = "/portal/register")
public class RegisterServlet extends BaseServlet {

    private UserDao users;

    @Override
    public void init() {
        users = bean(UserDao.class);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        render(req, resp, "register");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String username = param(req, "username").toLowerCase();
        String displayName = param(req, "displayName");
        String password = req.getParameter("password") == null ? "" : req.getParameter("password");
        String confirm = req.getParameter("confirm") == null ? "" : req.getParameter("confirm");

        Map<String, String> errors = validate(username, displayName, password, confirm);
        if (errors.isEmpty()) {
            try {
                users.insert(username, displayName, PasswordHasher.hash(password));
                redirect(req, resp, "/portal/login", "Account created. You can log in now.");
                return;
            } catch (DatabaseException e) {
                errors.put("username", e.getMessage());
            }
        }
        req.setAttribute("errors", errors);       // the JSP shows each message under its field
        req.setAttribute("username", username);
        req.setAttribute("displayName", displayName);
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        render(req, resp, "register");
    }

    /** Server-side validation: never trust what the browser checked. */
    static Map<String, String> validate(String username, String displayName, String password, String confirm) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (!username.matches("[a-z0-9_]{3,40}")) {
            errors.put("username", "3 to 40 characters: lowercase letters, digits or _");
        }
        if (displayName.length() < 2 || displayName.length() > 60) {
            errors.put("displayName", "2 to 60 characters");
        }
        if (password.length() < 8) {
            errors.put("password", "At least 8 characters");
        }
        if (!password.equals(confirm)) {
            errors.put("confirm", "The two passwords are different");
        }
        return errors;
    }
}
