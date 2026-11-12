package com.arbitrage.cap6;

import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.ErrorPageRegistrar;
import org.springframework.boot.web.server.ErrorPageRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * The same as error-page entries in web.xml: errors are forwarded to ErrorServlet,
 * which shows error.jsp (or returns JSON for /api calls).
 */
@Component
public class ErrorPagesConfig implements ErrorPageRegistrar {

    @Override
    public void registerErrorPages(ErrorPageRegistry registry) {
        registry.addErrorPages(
                new ErrorPage(HttpStatus.NOT_FOUND, "/portal/error"),
                new ErrorPage(HttpStatus.FORBIDDEN, "/portal/error"),
                new ErrorPage(HttpStatus.BAD_REQUEST, "/portal/error"),
                new ErrorPage(HttpStatus.SERVICE_UNAVAILABLE, "/portal/error"),
                new ErrorPage(HttpStatus.INTERNAL_SERVER_ERROR, "/portal/error"),
                new ErrorPage(Throwable.class, "/portal/error"));
    }
}
