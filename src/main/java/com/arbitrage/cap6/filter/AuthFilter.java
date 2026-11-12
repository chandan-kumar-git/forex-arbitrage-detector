package com.arbitrage.cap6.filter;

import com.arbitrage.cap6.servlet.BaseServlet;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * Runs before every /portal page. Without a logged-in user in the session it redirects
 * to the login page (and remembers where the user wanted to go). One check, every page.
 */
@WebFilter(filterName = "AuthFilter", urlPatterns = "/portal/*")
public class AuthFilter implements Filter {

    /** Pages that must work without logging in. */
    static final Set<String> PUBLIC = Set.of("/portal/login", "/portal/register", "/portal/error");

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        String path = req.getRequestURI().substring(req.getContextPath().length());

        // pages behind the login must not be kept by the browser cache (Back after logout)
        resp.setHeader("Cache-Control", "no-store");

        if (PUBLIC.contains(path)) {
            chain.doFilter(request, response);
            return;
        }
        HttpSession session = req.getSession(false);
        if (session != null && session.getAttribute(BaseServlet.USER) != null) {
            chain.doFilter(request, response);    // logged in: go on to the servlet
            return;
        }
        String next = path + (req.getQueryString() == null ? "" : "?" + req.getQueryString());
        resp.sendRedirect(req.getContextPath() + "/portal/login?next=" + URLEncoder.encode(next, StandardCharsets.UTF_8));
    }
}
