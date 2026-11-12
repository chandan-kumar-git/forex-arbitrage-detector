<%-- View for RouteServlet: the real-life money-transfer planner. --%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="pageTitle" value="Send money"/>
<%@ include file="/WEB-INF/jsp/common/header.jspf" %>

<div class="topline"><h1>Send money the cheapest way</h1></div>
<p class="lead">Paying a fee abroad or sending money home? Banks quote rare pairs (like INR → JPY) with a wide spread.
  Going through a common currency is often cheaper. The same graph search behind the detector finds the best route.</p>

<section class="card" style="margin-bottom:20px">
  <form class="card-body form-row" method="get" action="${pageContext.request.contextPath}/portal/route">
    <label class="field"><span>I have</span><input class="input" name="amount" inputmode="decimal" value="<c:out value='${amount}'/>"></label>
    <label class="field"><span>in</span>
      <select class="input" name="from"><c:forEach items="${codes}" var="k"><option ${k == from ? 'selected' : ''}>${k}</option></c:forEach></select>
    </label>
    <label class="field"><span>and need</span>
      <select class="input" name="to"><c:forEach items="${codes}" var="k"><option ${k == to ? 'selected' : ''}>${k}</option></c:forEach></select>
    </label>
    <button class="btn primary" type="submit">Find the best route</button>
  </form>
</section>

<c:if test="${not empty error}"><div class="notice bad" role="alert"><c:out value="${error}"/></div></c:if>

<c:if test="${not empty route}">
  <div class="grid grid-2">
    <section class="card">
      <div class="card-head"><h2>Best route</h2><span class="spacer"></span><span class="chip ${better ? 'live' : 'info'}"><i></i>${better ? 'Cheaper than direct' : 'Direct is best'}</span></div>
      <div class="card-body">
        <div class="hero-number"><fmt:formatNumber value="${route.received}" maxFractionDigits="2" minFractionDigits="2"/> <span style="font-size:.5em">${to}</span></div>
        <p class="muted" style="margin:0 0 16px">you receive, after bank spreads and a 0.10% fee per conversion</p>
        <div class="metro">
          <c:forTokens items="${pathText}" delims=" →" var="k" varStatus="s">
            <c:if test="${not s.first}"><span class="rail"></span></c:if>
            <span class="stop"><b></b>${k}</span>
          </c:forTokens>
        </div>
      </div>
    </section>
    <section class="card">
      <div class="card-head"><h2>Compared with changing directly</h2></div>
      <table class="data">
        <tbody>
          <tr><td>Direct ${from} → ${to}</td><td class="r"><fmt:formatNumber value="${route.directReceived}" maxFractionDigits="2" minFractionDigits="2"/> ${to}</td></tr>
          <tr><td>Best route (${route.hops} ${route.hops == 1 ? 'step' : 'steps'})</td><td class="r"><strong><fmt:formatNumber value="${route.received}" maxFractionDigits="2" minFractionDigits="2"/> ${to}</strong></td></tr>
          <tr><td><strong>You save</strong></td><td class="r gain"><fmt:formatNumber value="${route.saving}" maxFractionDigits="2" minFractionDigits="2"/> ${to}</td></tr>
        </tbody>
      </table>
      <div class="card-body muted" style="font-size:14px">Spreads used: 0.05% per major currency (USD, EUR, GBP, JPY, CHF), 0.40% per other currency, plus 0.80% when neither side is USD. Rates are the live simulated market.</div>
    </section>
  </div>
</c:if>

<%@ include file="/WEB-INF/jsp/common/footer.jspf" %>
