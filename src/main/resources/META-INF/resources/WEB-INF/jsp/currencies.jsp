<%-- View for CurrencyServlet: add / activate / delete, each a small POST form with the CSRF token. --%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="pageTitle" value="Currencies"/>
<%@ include file="/WEB-INF/jsp/common/header.jspf" %>

<div class="topline"><h1>Currencies</h1></div>
<p class="lead">The stations of the market. Changes go through the same <code>CurrencyService</code> and validators as the Swing app, and the market uses them from the next tick.</p>

<c:if test="${not empty error}"><div class="notice bad" role="alert"><c:out value="${error}"/></div></c:if>

<div class="grid grid-2">
  <section class="card">
    <div class="card-head"><h2>In the market</h2></div>
    <table class="data">
      <thead><tr><th>Code</th><th>Name</th><th class="r">USD price</th><th>Status</th><th></th></tr></thead>
      <tbody>
        <c:forEach items="${currencies}" var="cur">
          <tr style="${cur.active ? '' : 'opacity:.55'}">
            <td><strong>${cur.code}</strong></td>
            <td><c:out value="${cur.name}"/></td>
            <td class="r"><fmt:formatNumber value="${cur.usdPrice}" maxFractionDigits="6"/></td>
            <td><span class="chip ${cur.active ? 'live' : ''}"><i></i>${cur.active ? 'Active' : 'Off'}</span></td>
            <td class="r" style="white-space:nowrap">
              <c:if test="${not cur.base}">
                <form class="inline" method="post" action="${pageContext.request.contextPath}/portal/currencies">
                  <input type="hidden" name="csrf" value="${sessionScope.csrf}"><input type="hidden" name="code" value="${cur.code}">
                  <button class="btn sm quiet" name="action" value="toggle">${cur.active ? 'Deactivate' : 'Activate'}</button>
                </form>
                <form class="inline" method="post" action="${pageContext.request.contextPath}/portal/currencies" onsubmit="return confirm('Delete ${cur.code}?')">
                  <input type="hidden" name="csrf" value="${sessionScope.csrf}"><input type="hidden" name="code" value="${cur.code}">
                  <button class="btn sm quiet danger" name="action" value="delete">Delete</button>
                </form>
              </c:if>
              <c:if test="${cur.base}"><span class="muted" style="font-size:13px">base currency</span></c:if>
            </td>
          </tr>
        </c:forEach>
      </tbody>
    </table>
  </section>

  <section class="card">
    <div class="card-head"><h2>Add a currency</h2></div>
    <form class="card-body" method="post" action="${pageContext.request.contextPath}/portal/currencies" style="display:grid;gap:14px">
      <input type="hidden" name="csrf" value="${sessionScope.csrf}">
      <input type="hidden" name="action" value="add">
      <label class="field"><span>Code</span><input class="input" name="code" maxlength="3" placeholder="AUD" value="<c:out value='${formCode}'/>"><small>3 uppercase letters</small></label>
      <label class="field"><span>Name</span><input class="input" name="name" placeholder="Australian Dollar" value="<c:out value='${formName}'/>"></label>
      <label class="field"><span>USD price</span><input class="input" name="price" inputmode="decimal" placeholder="0.66" value="<c:out value='${formPrice}'/>"><small>Price of one unit in US dollars</small></label>
      <button class="btn primary" type="submit">Add currency</button>
    </form>
  </section>
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jspf" %>
