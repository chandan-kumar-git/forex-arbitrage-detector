package com.arbitrage.cap6.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** "Where is each rubric point?" - a page for the viva that links every CAP to its code and its live page. */
@WebServlet(name = "RubricServlet", urlPatterns = "/portal/rubric")
public class RubricServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setAttribute("serverInfo", getServletContext().getServerInfo());   // e.g. Apache Tomcat/10.1.x
        req.setAttribute("servletVersion", getServletContext().getMajorVersion() + "." + getServletContext().getMinorVersion());
        req.setAttribute("sessionId", req.getSession().getId());
        req.setAttribute("sessionCreated", new java.util.Date(req.getSession().getCreationTime()));
        render(req, resp, "rubric");
    }
}
