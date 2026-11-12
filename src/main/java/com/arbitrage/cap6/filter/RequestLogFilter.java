package com.arbitrage.cap6.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Logs every portal and API request with its status and time taken. Code before chain.doFilter()
 * runs on the way in, code after it runs on the way out: the filter "wraps" the servlet.
 */
@WebFilter(filterName = "RequestLogFilter", urlPatterns = {"/portal/*", "/api/v1/*"})
public class RequestLogFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(RequestLogFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        long start = System.nanoTime();
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        try {
            chain.doFilter(request, response);
        } finally {
            long micros = (System.nanoTime() - start) / 1000;
            log.info("{} {} -> {} in {} ms", req.getMethod(), req.getRequestURI(), resp.getStatus(), micros / 1000.0);
        }
    }
}
