<%-- View for RubricServlet: where each rubric point lives, for the viva. --%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Rubric map"/>
<%@ include file="/WEB-INF/jsp/common/header.jspf" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<div class="topline"><h1>Rubric map</h1></div>
<p class="lead">Each course phase, the code that implements it, and a page you can open to see it working.
  Running on <code>${serverInfo}</code>, Servlet API ${servletVersion}. Your session id: <code>${sessionId}</code> (created ${sessionCreated}).</p>

<section class="card">
  <div class="table-wrap">
    <table class="data">
      <thead><tr><th>Phase</th><th>What it shows</th><th>Code (src/main/java/com/arbitrage/…)</th><th>See it</th></tr></thead>
      <tbody>
        <tr><td><strong>CAP-1</strong></td><td>OOP and Collections, Bellman-Ford engine, unit tests</td><td><code>model/</code> <code>engine/</code> <code>feed/</code></td><td>Swing app · <code>mvn test</code></td></tr>
        <tr><td><strong>CAP-3</strong></td><td>Swing GUI: layouts, events, custom painting</td><td><code>ui/</code></td><td>Swing window</td></tr>
        <tr><td><strong>CAP-5</strong></td><td>JDBC CRUD with PreparedStatement, transactions</td><td><code>db/</code></td><td><a href="${ctx}/portal/history">History</a> (JDBC DAO)</td></tr>
        <tr><td><strong>CAP-5</strong></td><td>TCP sockets: feed server and client</td><td><code>server/</code> <code>feed/SocketRateProvider</code></td><td>Rate Feed Server window</td></tr>
        <tr><td><strong>CAP-6</strong></td><td>Servlets: doGet / doPost, init(), forward and redirect</td><td><code>cap6/servlet/</code></td><td><a href="${ctx}/portal">Dashboard</a></td></tr>
        <tr><td><strong>CAP-6</strong></td><td>JSP with EL and JSTL (c:forEach, c:if, c:url, fmt)</td><td><code>resources/META-INF/resources/WEB-INF/jsp/</code></td><td>every portal page</td></tr>
        <tr><td><strong>CAP-6</strong></td><td>MVC: servlet → model → JSP view</td><td><code>cap6/servlet/BaseServlet.render()</code></td><td><a href="${ctx}/portal/history">History</a></td></tr>
        <tr><td><strong>CAP-6</strong></td><td>Sessions, login, logout, cookies, session fixation, CSRF token</td><td><code>cap6/servlet/LoginServlet</code> <code>cap6/auth/</code></td><td><a href="${ctx}/portal/login">Login</a></td></tr>
        <tr><td><strong>CAP-6</strong></td><td>Filters and Listeners</td><td><code>cap6/filter/</code> <code>cap6/listener/</code></td><td>Open sessions count on the dashboard</td></tr>
        <tr><td><strong>CAP-6</strong></td><td>Error pages</td><td><code>cap6/ErrorPagesConfig</code> <code>ErrorServlet</code></td><td><a href="${ctx}/portal/does-not-exist">Try a 404</a></td></tr>
        <tr><td><strong>CAP-7</strong></td><td>Hibernate / JPA entities, @OneToMany, @IdClass</td><td><code>cap7/entity/</code></td><td><a href="${ctx}/portal/history">Open any opportunity</a></td></tr>
        <tr><td><strong>CAP-7</strong></td><td>Spring Data repositories: derived queries, JPQL, paging</td><td><code>cap7/repository/</code></td><td><a href="${ctx}/api/v1/opportunities?size=5">/api/v1/opportunities</a></td></tr>
        <tr><td><strong>CAP-7</strong></td><td>@Transactional services</td><td><code>cap7/service/</code></td><td><a href="${ctx}/portal/alerts">Alerts</a></td></tr>
        <tr><td><strong>CAP-7</strong></td><td>REST API: GET POST PUT PATCH DELETE, status codes, validation, error JSON</td><td><code>cap7/rest/</code></td><td><a href="${ctx}/api/v1/currencies">/api/v1/currencies</a> · <a href="${ctx}/api/v1/opportunities/summary">summary</a> · <a href="${ctx}/api/v1/route?from=INR&to=JPY&amount=50000">route</a></td></tr>
        <tr><td><strong>Real life</strong></td><td>Cheapest money transfer route, profit alerts</td><td><code>cap7/service/RouteService</code> <code>AlertService</code></td><td><a href="${ctx}/portal/route">Send money</a></td></tr>
      </tbody>
    </table>
  </div>
</section>

<%@ include file="/WEB-INF/jsp/common/footer.jspf" %>
