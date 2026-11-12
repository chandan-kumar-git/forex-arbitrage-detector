package com.arbitrage.cap6.servlet;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * The error page for the portal (404, 403, 500, ...). The container forwards here and puts the
 * details in the standard jakarta.servlet.error.* request attributes.
 */
@WebServlet(name = "ErrorServlet", urlPatterns = "/portal/error")
public class ErrorServlet extends BaseServlet {

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Object code = req.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object message = req.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Object uri = req.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        int status = code instanceof Integer i ? i : 500;
        String uriText = uri == null ? "" : uri.toString();

        // a failed /api call should get JSON, not an HTML page
        if (uriText.startsWith(req.getContextPath() + "/api")) {
            resp.setStatus(status);
            resp.setContentType("application/json;charset=UTF-8");
            resp.getWriter().write("{\"status\":" + status + ",\"path\":\"" + uriText.replace("\"", "") + "\"}");
            return;
        }
        req.setAttribute("status", status);
        req.setAttribute("title", switch (status) {
            case 400 -> "That request was not right";
            case 403 -> "Not allowed";
            case 404 -> "Page not found";
            case 503 -> "Service not available";
            default -> "Something went wrong";
        });
        req.setAttribute("message", message == null || message.toString().isBlank() ? "" : message.toString());
        req.setAttribute("uri", uriText);
        resp.setStatus(status);
        render(req, resp, "error");
    }
}
