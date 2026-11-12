package com.arbitrage.cap6.servlet;

import com.arbitrage.cap6.view.LegView;
import com.arbitrage.cap6.view.OpportunityView;
import com.arbitrage.cap7.dto.Dtos.OpportunityDto;
import com.arbitrage.cap7.service.NotFoundException;
import com.arbitrage.cap7.service.OpportunityQueryService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.CannotCreateTransactionException;

import java.io.IOException;

/**
 * One opportunity with its legs. This page reads through the CAP-7 JPA service
 * (one JOIN FETCH query), to show that servlets and Spring services work together.
 */
@WebServlet(name = "OpportunityServlet", urlPatterns = "/portal/opportunity")
public class OpportunityServlet extends BaseServlet {

    private OpportunityQueryService service;

    @Override
    public void init() {
        service = bean(OpportunityQueryService.class);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        long id;
        try {
            id = Long.parseLong(param(req, "id"));
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or wrong opportunity id");
            return;
        }
        try {
            OpportunityDto o = service.get(id);
            OpportunityView view = new OpportunityView(o.id(), o.pathText(), o.legs(), o.profitPercent(), o.fee(),
                    o.source(), OpportunityView.format(o.detectedAt()),
                    o.legDetails().stream().map(l -> new LegView(l.legNo(), l.from(), l.to(), l.rate())).toList());
            req.setAttribute("o", view);
            render(req, resp, "opportunity");
        } catch (NotFoundException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, e.getMessage());
        } catch (CannotCreateTransactionException | DataAccessException e) {
            resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "The database is not available. Is MySQL running?");
        }
    }
}
