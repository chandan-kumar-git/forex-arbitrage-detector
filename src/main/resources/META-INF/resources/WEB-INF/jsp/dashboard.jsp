<%-- View for DashboardServlet. First paint comes from request attributes (server side);
     a small script then refreshes the numbers every 2 s from the REST endpoint /api/state. --%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="pageTitle" value="Dashboard"/>
<%@ include file="/WEB-INF/jsp/common/header.jspf" %>

<div class="topline">
  <h1>Hello, <c:out value="${sessionScope.user.displayName}"/></h1>
  <span class="spacer"></span>
  <span class="chip ${paused ? 'warn' : 'live'}" id="status"><i></i>${paused ? 'Paused' : 'Live'}</span>
  <span class="chip info">${source == 'SOCKET' ? 'Socket feed' : 'Local simulator'}</span>
  <span class="chip ${dbOnline ? 'live' : 'off'}"><i></i>MySQL ${dbOnline ? 'connected' : 'offline'}</span>
</div>
<p class="lead">This page is a JSP rendered by <code>DashboardServlet</code>. The engine below is the same one that drives the Swing desktop app.</p>

<div class="grid grid-4" style="margin-bottom:20px">
  <div class="card stat"><div class="k">Ticks analysed</div><div class="v" id="ticks"><fmt:formatNumber value="${ticks}"/></div><div class="s">one market snapshot per second</div></div>
  <div class="card stat"><div class="k">Loops found</div><div class="v" id="found">${found}</div><div class="s">since the app started</div></div>
  <div class="card stat"><div class="k">Best loop</div><div class="v gain" id="best"><fmt:formatNumber value="${bestProfit}" maxFractionDigits="3" minFractionDigits="3"/>%</div><div class="s" id="bestPath"><c:out value="${bestPath}"/></div></div>
  <div class="card stat"><div class="k">Open sessions</div><div class="v">${activeSessions}</div><div class="s">counted by an HttpSessionListener</div></div>
</div>

<div class="grid grid-2">
  <section class="card">
    <div class="card-head"><h2>Latest loop</h2><span class="spacer"></span><a class="btn sm quiet" href="${pageContext.request.contextPath}/portal/history">All history</a></div>
    <div class="card-body" id="latest">
      <c:choose>
        <c:when test="${not empty latest}">
          <div class="hero-number"><fmt:formatNumber value="${latest.profitPercent}" maxFractionDigits="3" minFractionDigits="3"/>%</div>
          <p class="muted" style="margin:0 0 14px">profit after fees, found <c:out value="${latest.detectedAt}"/></p>
          <div class="metro" aria-label="<c:out value='${latest.pathText}'/>">
            <c:forTokens items="${latest.pathText}" delims=" →" var="code" varStatus="s">
              <c:if test="${not s.first}"><span class="rail"></span></c:if>
              <span class="stop"><b></b><c:out value="${code}"/></span>
            </c:forTokens>
          </div>
        </c:when>
        <c:otherwise><div class="empty">No loop yet. The simulator usually finds one within a few seconds.</div></c:otherwise>
      </c:choose>
    </div>
  </section>

  <section class="card">
    <div class="card-head"><h2>Your alerts fired</h2><span class="spacer"></span><a class="btn sm quiet" href="${pageContext.request.contextPath}/portal/alerts">Manage</a></div>
    <c:choose>
      <c:when test="${empty hits}"><div class="empty">Nothing yet. <a href="${pageContext.request.contextPath}/portal/alerts">Add an alert</a> and it fires here when a loop matches.</div></c:when>
      <c:otherwise>
        <table class="data"><tbody>
          <c:forEach items="${hits}" var="h" end="5">
            <tr><td class="muted">${h.at}</td><td><c:out value="${h.pathText}"/></td><td class="r gain"><fmt:formatNumber value="${h.profitPercent}" maxFractionDigits="3" minFractionDigits="3"/>%</td></tr>
          </c:forEach>
        </tbody></table>
      </c:otherwise>
    </c:choose>
  </section>
</div>

<section class="card" style="margin-top:20px">
  <div class="card-head"><h2>Recent loops</h2><span class="spacer"></span><span class="muted" style="font-size:14px">kept in memory by the engine</span></div>
  <div class="table-wrap">
    <table class="data">
      <thead><tr><th>Found</th><th>Path</th><th class="r">Legs</th><th class="r">Profit</th></tr></thead>
      <tbody id="recent">
        <c:forEach items="${recent}" var="o">
          <tr><td class="muted">${o.detectedAt}</td><td><c:out value="${o.pathText}"/></td><td class="r">${o.legCount}</td>
            <td class="r gain"><fmt:formatNumber value="${o.profitPercent}" maxFractionDigits="3" minFractionDigits="3"/>%</td></tr>
        </c:forEach>
        <c:if test="${empty recent}"><tr><td colspan="4" class="empty">Waiting for the first loop…</td></tr></c:if>
      </tbody>
    </table>
  </div>
</section>

<script>
  // Live refresh: ask the REST endpoint for the engine state and update the numbers in place.
  (function () {
    const $ = (id) => document.getElementById(id);
    let lastFound = ${found};
    async function refresh() {
      try {
        const s = await (await fetch('${pageContext.request.contextPath}/api/state', { cache: 'no-store' })).json();
        $('ticks').textContent = s.stats.ticks.toLocaleString('en-US');
        $('found').textContent = s.stats.found;
        $('best').textContent = s.stats.bestProfit.toFixed(3) + '%';
        $('bestPath').textContent = s.stats.bestPath;
        $('status').className = 'chip ' + (s.paused ? 'warn' : 'live');
        $('status').lastChild.textContent = s.paused ? 'Paused' : 'Live';
        if (s.stats.found !== lastFound) {
          lastFound = s.stats.found;
          $('found').parentElement.classList.remove('flashy');
          void $('found').offsetWidth;          // restart the highlight animation
          $('found').parentElement.classList.add('flashy');
          const rows = s.recent.slice(0, 8).map((o) =>
            '<tr><td class="muted">' + new Date(o.detectedAt).toLocaleTimeString() + '</td><td>' + o.path.join(' → ') +
            '</td><td class="r">' + (o.path.length - 1) + '</td><td class="r gain">' + o.profitPercent.toFixed(3) + '%</td></tr>');
          $('recent').innerHTML = rows.join('');
        }
      } catch (e) { /* server restarting: try again on the next tick */ }
    }
    setInterval(refresh, 2000);
  })();
</script>

<%@ include file="/WEB-INF/jsp/common/footer.jspf" %>
