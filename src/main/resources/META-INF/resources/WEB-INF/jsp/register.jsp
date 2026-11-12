<%-- View for RegisterServlet: each field shows its own server-side error from the "errors" map. --%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Create an account · Arbitrage Portal</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/portal-assets/portal.css">
</head>
<body class="auth">
  <div class="auth-card">
    <span class="brand-mark"><svg width="18" height="18" viewBox="0 0 32 32" aria-hidden="true"><path d="M16 6 L26 23 L6 23 Z" fill="none" stroke="#fff" stroke-width="3" stroke-linejoin="round"/></svg></span>
    <h1>Create an account</h1>
    <p class="muted">Your password is stored only as a PBKDF2 hash.</p>

    <form method="post" action="${pageContext.request.contextPath}/portal/register" novalidate>
      <label class="field"><span>Username</span>
        <input class="input ${not empty errors.username ? 'bad' : ''}" name="username" autocomplete="username" value="<c:out value='${username}'/>">
        <c:choose>
          <c:when test="${not empty errors.username}"><span class="err"><c:out value="${errors.username}"/></span></c:when>
          <c:otherwise><small>3 to 40: lowercase letters, digits or _</small></c:otherwise>
        </c:choose>
      </label>
      <label class="field"><span>Your name</span>
        <input class="input ${not empty errors.displayName ? 'bad' : ''}" name="displayName" value="<c:out value='${displayName}'/>">
        <c:if test="${not empty errors.displayName}"><span class="err"><c:out value="${errors.displayName}"/></span></c:if>
      </label>
      <label class="field"><span>Password</span>
        <input class="input ${not empty errors.password ? 'bad' : ''}" name="password" type="password" autocomplete="new-password">
        <c:choose>
          <c:when test="${not empty errors.password}"><span class="err"><c:out value="${errors.password}"/></span></c:when>
          <c:otherwise><small>At least 8 characters</small></c:otherwise>
        </c:choose>
      </label>
      <label class="field"><span>Repeat the password</span>
        <input class="input ${not empty errors.confirm ? 'bad' : ''}" name="confirm" type="password" autocomplete="new-password">
        <c:if test="${not empty errors.confirm}"><span class="err"><c:out value="${errors.confirm}"/></span></c:if>
      </label>
      <button class="btn primary" type="submit">Create account</button>
    </form>
    <p class="hint">Already have one? <a href="${pageContext.request.contextPath}/portal/login">Log in</a>.</p>
  </div>
</body>
</html>
