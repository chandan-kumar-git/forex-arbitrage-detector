<%-- View for OpportunityServlet: one loop and its legs (loaded through JPA with JOIN FETCH). --%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="pageTitle" value="Opportunity #${o.id}"/>
<%@ include file="/WEB-INF/jsp/common/header.jspf" %>

<div class="topline">
  <h1>Opportunity #${o.id}</h1>
  <span class="spacer"></span>
  <a class="btn" href="${pageContext.request.contextPath}/portal/history">Back to history</a>
</div>

<div class="grid grid-2">
  <section class="card">
    <div class="card-body">
      <div class="hero-number"><fmt:formatNumber value="${o.profitPercent}" maxFractionDigits="3" minFractionDigits="3"/>%</div>
      <p class="muted" style="margin:0 0 18px">profit after a fee of <fmt:formatNumber value="${o.fee * 100}" maxFractionDigits="2" minFractionDigits="2"/>% per conversion · found ${o.detectedAt} · ${o.source == 'SOCKET' ? 'socket feed' : 'local simulator'}</p>
      <div class="metro" aria-label="<c:out value='${o.pathText}'/>">
        <c:forEach items="${o.legs}" var="l" varStatus="s">
          <c:if test="${s.first}"><span class="stop"><b></b>${l.fromCode}</span></c:if>
          <span class="rail"></span>
          <span class="stop"><b></b>${l.toCode}</span>
        </c:forEach>
      </div>
    </div>
  </section>
  <section class="card">
    <div class="card-head"><h2>The trades, one by one</h2></div>
    <table class="data">
      <thead><tr><th>Step</th><th>Convert</th><th class="r">Rate</th><th class="r">1,000 becomes</th></tr></thead>
      <tbody>
        <c:set var="money" value="${1000}"/>
        <c:forEach items="${o.legs}" var="l">
          <c:set var="money" value="${money * l.rate * (1 - o.fee)}"/>
          <tr><td>${l.legNo}</td><td><strong>${l.fromCode} → ${l.toCode}</strong></td>
            <td class="r"><fmt:formatNumber value="${l.rate}" maxFractionDigits="6"/></td>
            <td class="r"><fmt:formatNumber value="${money}" maxFractionDigits="2" minFractionDigits="2"/> ${l.toCode}</td></tr>
        </c:forEach>
      </tbody>
    </table>
    <div class="card-body muted" style="font-size:14px">Start with 1,000 <c:out value="${o.legs[0].fromCode}"/>, follow the trades, and you end with more than you started: that is the arbitrage.</div>
  </section>
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jspf" %>
