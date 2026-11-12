<%-- View for LoginServlet. EL ${...} reads request attributes; <c:out> escapes text against XSS. --%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Log in · Arbitrage Portal</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/portal-assets/portal.css">
</head>
<body class="auth">
  <div class="auth-card">
    <span class="brand-mark"><svg width="18" height="18" viewBox="0 0 32 32" aria-hidden="true"><path d="M16 6 L26 23 L6 23 Z" fill="none" stroke="#fff" stroke-width="3" stroke-linejoin="round"/></svg></span>
    <h1>Log in to the portal</h1>
    <p class="muted">Watch the Java engine live, browse the MySQL history and set profit alerts.</p>

    <c:if test="${not empty flash}"><div class="notice good" role="status"><c:out value="${flash}"/></div></c:if>
    <c:if test="${not empty error}"><div class="notice bad" role="alert"><c:out value="${error}"/></div></c:if>

    <form method="post" action="${pageContext.request.contextPath}/portal/login">
      <input type="hidden" name="next" value="<c:out value='${next}'/>">
      <label class="field"><span>Username</span>
        <input class="input" name="username" autocomplete="username" required value="<c:out value='${rememberedUser}'/>">
      </label>
      <label class="field"><span>Password</span>
        <input class="input" name="password" type="password" autocomplete="current-password" required>
      </label>
      <label class="check"><input type="checkbox" name="remember" ${not empty rememberedUser ? 'checked' : ''}> Remember my username on this computer</label>
      <button class="btn primary" type="submit">Log in</button>
    </form>
    <p class="hint">New here? <a href="${pageContext.request.contextPath}/portal/register">Create an account</a>.
      For the viva: <code>demo</code> / <code>demo1234</code>.</p>
  </div>
</body>
</html>
