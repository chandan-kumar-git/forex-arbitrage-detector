<%-- View for ErrorServlet (404, 403, 500, ...). Works with or without a logged-in user. --%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>${status} · Arbitrage Portal</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/portal-assets/portal.css">
</head>
<body class="auth">
  <div class="auth-card">
    <div class="hero-number" style="color:var(--blue)">${status}</div>
    <h1><c:out value="${title}"/></h1>
    <c:if test="${not empty message}"><p class="muted"><c:out value="${message}"/></p></c:if>
    <c:if test="${not empty uri}"><p class="muted" style="font-size:14px">Address: <code><c:out value="${uri}"/></code></p></c:if>
    <p style="margin-top:22px"><a class="btn primary" href="${pageContext.request.contextPath}/portal">Go to the portal</a></p>
  </div>
</body>
</html>
