package com.arbitrage.cap6;

import com.arbitrage.cap6.auth.PasswordHasher;
import com.arbitrage.cap6.auth.User;
import com.arbitrage.cap6.filter.AuthFilter;
import com.arbitrage.cap6.servlet.BaseServlet;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** CAP-6: password hashing and the login filter, tested without starting a server. */
class Cap6Test {

    @Test
    void passwordHashesAreSaltedAndVerifiable() {
        String first = PasswordHasher.hash("demo1234");
        String second = PasswordHasher.hash("demo1234");
        assertNotEquals(first, second);           // a new random salt each time
        assertTrue(PasswordHasher.matches("demo1234", first));
        assertFalse(PasswordHasher.matches("wrong-password", first));
        assertFalse(PasswordHasher.matches("demo1234", "not-a-hash"));
    }

    @Test
    void filterSendsVisitorsWithoutSessionToLogin() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/portal/history");
        req.setQueryString("period=today");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        new AuthFilter().doFilter(req, resp, chain);

        assertNull(chain.getRequest());           // the servlet was never reached
        assertEquals("/portal/login?next=%2Fportal%2Fhistory%3Fperiod%3Dtoday", resp.getRedirectedUrl());
    }

    @Test
    void filterLetsLoggedInUsersAndPublicPagesThrough() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/portal/history");
        req.getSession().setAttribute(BaseServlet.USER, new User(1, "demo", "Demo User", null));
        MockFilterChain chain = new MockFilterChain();
        new AuthFilter().doFilter(req, new MockHttpServletResponse(), chain);
        assertNotNull(chain.getRequest());

        MockFilterChain loginChain = new MockFilterChain();
        new AuthFilter().doFilter(new MockHttpServletRequest("GET", "/portal/login"), new MockHttpServletResponse(), loginChain);
        assertNotNull(loginChain.getRequest());
    }
}
