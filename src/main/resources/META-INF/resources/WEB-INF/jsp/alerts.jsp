<%-- View for AlertServlet: the user's alerts (JPA entity ProfitAlert) and the ones that fired. --%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="pageTitle" value="Alerts"/>
<%@ include file="/WEB-INF/jsp/common/header.jspf" %>

<div class="topline"><h1>Profit alerts</h1></div>
<p class="lead">Tell the engine what you care about. Every 2 seconds <code>AlertService</code> checks each new loop against your alerts and counts the matches in MySQL.</p>

<c:if test="${not empty dbError}"><div class="notice bad" role="alert"><c:out value="${dbError}"/></div></c:if>

<div class="grid grid-2">
  <section class="card">
    <div class="card-head"><h2>Your alerts</h2></div>
    <table class="data">
      <thead><tr><th>When a loop pays</th><th>Through</th><th class="r">Fired</th><th>Last match</th><th></th></tr></thead>
      <tbody>
        <c:forEach items="${alerts}" var="a">
          <tr style="${a.active ? '' : 'opacity:.55'}">
            <td><strong>≥ <fmt:formatNumber value="${a.minProfit}" maxFractionDigits="3"/>%</strong></td>
            <td>${a.currencyCode}</td>
            <td class="r"><strong>${a.triggerCount}</strong></td>
            <td class="muted" style="font-size:14px"><c:out value="${a.lastPath}"/><br>${a.lastTriggeredAt}</td>
            <td class="r" style="white-space:nowrap">
              <form class="inline" method="post" action="${pageContext.request.contextPath}/portal/alerts">
                <input type="hidden" name="csrf" value="${sessionScope.csrf}"><input type="hidden" name="id" value="${a.id}">
                <button class="btn sm quiet" name="action" value="${a.active ? 'off' : 'on'}">${a.active ? 'Pause' : 'Resume'}</button>
                <button class="btn sm quiet danger" name="action" value="delete">Delete</button>
              </form>
            </td>
          </tr>
        </c:forEach>
        <c:if test="${empty alerts and empty dbError}"><tr><td colspan="5" class="empty">No alerts yet. Add one on the right.</td></tr></c:if>
      </tbody>
    </table>
  </section>

  <div class="grid">
    <section class="card">
      <div class="card-head"><h2>New alert</h2></div>
      <form class="card-body" method="post" action="${pageContext.request.contextPath}/portal/alerts" style="display:grid;gap:14px">
        <input type="hidden" name="csrf" value="${sessionScope.csrf}">
        <input type="hidden" name="action" value="add">
        <label class="field"><span>Minimum profit %</span><input class="input" name="minProfit" inputmode="decimal" value="0.5"><small>For example 0.5 means half a percent</small></label>
        <label class="field"><span>Only loops through (optional)</span><input class="input" name="currencyCode" maxlength="3" placeholder="INR"></label>
        <button class="btn primary" type="submit">Add alert</button>
      </form>
    </section>
    <section class="card">
      <div class="card-head"><h2>Fired recently</h2></div>
      <c:choose>
        <c:when test="${empty hits}"><div class="empty">Nothing has matched yet.</div></c:when>
        <c:otherwise>
          <table class="data"><tbody>
            <c:forEach items="${hits}" var="h" end="7">
              <tr><td class="muted">${h.at}</td><td><c:out value="${h.pathText}"/></td><td class="r gain"><fmt:formatNumber value="${h.profitPercent}" maxFractionDigits="3" minFractionDigits="3"/>%</td></tr>
            </c:forEach>
          </tbody></table>
        </c:otherwise>
      </c:choose>
    </section>
  </div>
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jspf" %>
