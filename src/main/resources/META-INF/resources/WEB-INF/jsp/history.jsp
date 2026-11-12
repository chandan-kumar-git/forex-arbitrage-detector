<%-- View for HistoryServlet: a GET form for the filters, a table built with <c:forEach>, and paging links built with <c:url>. --%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="pageTitle" value="History"/>
<%@ include file="/WEB-INF/jsp/common/header.jspf" %>

<div class="topline"><h1>History</h1></div>
<p class="lead">Every loop the engine saved in MySQL. This page reads them with the CAP-5 JDBC DAO (<code>OpportunityDao.search</code>, a PreparedStatement).</p>

<section class="card">
  <form class="card-body form-row" method="get" action="${pageContext.request.contextPath}/portal/history" style="border-bottom:1px solid var(--line)">
    <label class="field"><span>Period</span>
      <select class="input" name="period">
        <c:forEach items="${['hour','today','week','all']}" var="p">
          <option value="${p}" ${period == p ? 'selected' : ''}>${p == 'hour' ? 'Last hour' : p == 'today' ? 'Today' : p == 'week' ? 'Last 7 days' : 'All'}</option>
        </c:forEach>
      </select>
    </label>
    <label class="field"><span>Min profit %</span><input class="input" name="min" inputmode="decimal" value="${min}"></label>
    <label class="field"><span>Currency</span><input class="input" name="code" maxlength="3" placeholder="e.g. INR" value="<c:out value='${code}'/>"></label>
    <button class="btn primary" type="submit">Apply</button>
    <a class="btn quiet" href="${pageContext.request.contextPath}/portal/history">Reset</a>
  </form>

  <c:choose>
    <c:when test="${not empty dbError}"><div class="empty"><c:out value="${dbError}"/></div></c:when>
    <c:otherwise>
      <div class="card-body" style="padding-bottom:0">
        <strong>${count}</strong> loops ·
        average <span class="gain"><fmt:formatNumber value="${average}" maxFractionDigits="3" minFractionDigits="3"/>%</span> ·
        best <span class="gain"><fmt:formatNumber value="${best}" maxFractionDigits="3" minFractionDigits="3"/>%</span>
      </div>
      <div class="table-wrap">
        <table class="data">
          <thead><tr><th>#</th><th>Found</th><th>Path</th><th class="r">Profit</th><th>Source</th><th></th></tr></thead>
          <tbody>
            <c:forEach items="${rows}" var="o">
              <tr>
                <td class="muted">${o.id}</td>
                <td class="muted">${o.detectedAt}</td>
                <td><a href="${pageContext.request.contextPath}/portal/opportunity?id=${o.id}"><c:out value="${o.pathText}"/></a></td>
                <td class="r gain"><fmt:formatNumber value="${o.profitPercent}" maxFractionDigits="3" minFractionDigits="3"/>%</td>
                <td>${o.source == 'SOCKET' ? 'Socket' : 'Simulator'}</td>
                <td class="r">
                  <form class="inline" method="post" action="${pageContext.request.contextPath}/portal/history" onsubmit="return confirm('Delete opportunity #${o.id}?')">
                    <input type="hidden" name="csrf" value="${sessionScope.csrf}">
                    <input type="hidden" name="id" value="${o.id}">
                    <input type="hidden" name="back" value="/portal/history?period=${period}&min=${min}&code=${code}&page=${pageNo}">
                    <button class="btn sm quiet danger" type="submit">Delete</button>
                  </form>
                </td>
              </tr>
            </c:forEach>
            <c:if test="${empty rows}"><tr><td colspan="6" class="empty">No loops match these filters.</td></tr></c:if>
          </tbody>
        </table>
      </div>
      <div class="pager">
        <span class="muted">Page ${pageNo} of ${pages}</span>
        <span>
          <c:if test="${pageNo > 1}">
            <c:url var="prev" value="/portal/history"><c:param name="period" value="${period}"/><c:param name="min" value="${min}"/><c:param name="code" value="${code}"/><c:param name="page" value="${pageNo - 1}"/></c:url>
            <a class="btn sm" href="${prev}">Previous</a>
          </c:if>
          <c:if test="${pageNo < pages}">
            <c:url var="next" value="/portal/history"><c:param name="period" value="${period}"/><c:param name="min" value="${min}"/><c:param name="code" value="${code}"/><c:param name="page" value="${pageNo + 1}"/></c:url>
            <a class="btn sm" href="${next}">Next</a>
          </c:if>
        </span>
      </div>
    </c:otherwise>
  </c:choose>
</section>

<%@ include file="/WEB-INF/jsp/common/footer.jspf" %>
