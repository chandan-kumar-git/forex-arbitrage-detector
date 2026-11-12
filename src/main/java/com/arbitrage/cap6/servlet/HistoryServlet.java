package com.arbitrage.cap6.servlet;

import com.arbitrage.cap6.view.OpportunityView;
import com.arbitrage.cap7.service.Period;
import com.arbitrage.db.DatabaseException;
import com.arbitrage.db.OpportunityDao;
import com.arbitrage.model.Opportunity;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/**
 * History page. GET: read the filters from the query string, load rows with the CAP-5 JDBC DAO,
 * page them (20 per page) and forward to history.jsp. POST: delete one row (Post/Redirect/Get).
 */
@WebServlet(name = "HistoryServlet", urlPatterns = "/portal/history")
public class HistoryServlet extends BaseServlet {

    private static final int PAGE_SIZE = 20;

    private OpportunityDao dao;

    @Override
    public void init() {
        dao = bean(OpportunityDao.class);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Period period = Period.parse(param(req, "period"));
        double minProfit = parseDouble(param(req, "min"));
        String code = param(req, "code").toUpperCase().replaceAll("[^A-Z]", "");
        int page = Math.max(1, (int) parseDouble(param(req, "page")));

        req.setAttribute("period", period.name().toLowerCase());
        req.setAttribute("min", minProfit);
        req.setAttribute("code", code);
        try {
            String like = "%" + code + "%";       // a LIKE pattern, still passed as a ? parameter
            List<Opportunity> rows = dao.search(period.since(), minProfit, like);
            OpportunityDao.Summary summary = dao.summary(period.since(), minProfit, like);
            int pages = Math.max(1, (rows.size() + PAGE_SIZE - 1) / PAGE_SIZE);
            page = Math.min(page, pages);
            List<OpportunityView> pageRows = rows.subList((page - 1) * PAGE_SIZE, Math.min(rows.size(), page * PAGE_SIZE))
                    .stream().map(OpportunityView::of).toList();
            req.setAttribute("rows", pageRows);
            req.setAttribute("count", summary.count());
            req.setAttribute("average", summary.averageProfit());
            req.setAttribute("best", summary.bestProfit());
            req.setAttribute("pageNo", page);
            req.setAttribute("pages", pages);
        } catch (DatabaseException e) {
            req.setAttribute("dbError", e.getMessage());
        }
        render(req, resp, "history");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!validCsrf(req)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "The form expired. Reload the page and try again.");
            return;
        }
        long id = (long) parseDouble(param(req, "id"));
        String message;
        try {
            message = dao.delete(id) == 1 ? "Opportunity #" + id + " deleted." : "Opportunity #" + id + " was already gone.";
        } catch (DatabaseException e) {
            message = e.getMessage();
        }
        String back = req.getParameter("back");
        redirect(req, resp, back != null && back.startsWith("/portal/history") ? back : "/portal/history", message);
    }

    private static double parseDouble(String text) {
        try {
            return text.isEmpty() ? 0 : Math.max(0, Double.parseDouble(text));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
