# Rubric guide: where every CAP lives

The project is arranged in six parts. Each Java package has a `package-info.java` that says which part it is.

| Part | CAP | Folder (`src/main/java/com/arbitrage/…`) | What to show |
|---|---|---|---|
| 1 | CAP-1 | `model/`, `config/`, `validation/`, `engine/`, `feed/`, `service/` | OOP records, Collections, Bellman-Ford, `mvn test` |
| 2 | CAP-3 | `ui/` | The Swing desktop window |
| 3 | CAP-5 | `db/` | JDBC CRUD with PreparedStatement, transactions |
| 4 | CAP-5 | `server/`, `feed/SocketRateProvider` | Rate Feed Server over TCP sockets |
| 5 | CAP-6 | `cap6/` + `src/main/resources/META-INF/resources/WEB-INF/jsp/` | Servlet + JSP portal at `http://127.0.0.1:8080/portal` |
| 6 | CAP-7 | `cap7/` (+ `api/`) | JPA/Hibernate entities, Spring Data, REST API at `/api/v1` |
| — | CAP-8 | `docs/`, `web/` | Demo, documentation, the React web app (Vercel) |

## CAP-6: Servlet, JSP, MVC

| Rubric idea | File | How it works |
|---|---|---|
| Servlet lifecycle (`init`, `doGet`, `doPost`) | `cap6/servlet/*Servlet.java` | Every servlet looks up its Spring services in `init()`; GET shows a page, POST changes data |
| MVC | `cap6/servlet/BaseServlet.render()` | Servlet (controller) → request attributes (model) → forward to a JSP (view) |
| JSP, EL, JSTL | `WEB-INF/jsp/*.jsp` | `${...}` reads attributes; `<c:forEach>`, `<c:if>`, `<c:choose>`, `<c:url>`, `<fmt:formatNumber>`; `<c:out>` escapes text |
| Includes | `WEB-INF/jsp/common/header.jspf`, `footer.jspf` | `<%@ include %>` shares the layout |
| Session, login, logout | `LoginServlet`, `LogoutServlet`, `cap6/auth/` | User in `HttpSession`; a new session id after login (session fixation); `invalidate()` on logout |
| Cookies | `LoginServlet.rememberUsername()` | "Remember my username" cookie, HttpOnly, 30 days |
| Passwords | `cap6/auth/PasswordHasher` | PBKDF2 with a random salt; only the hash is stored (`app_user` table, JDBC `UserDao`) |
| Filter | `cap6/filter/AuthFilter`, `RequestLogFilter` | Login check before every portal page; request timing log |
| Listener | `cap6/listener/PortalListener` | ServletContext start, live count of open sessions |
| Error pages | `cap6/ErrorPagesConfig`, `ErrorServlet`, `error.jsp` | 404 / 403 / 500 go to a friendly page (JSON for `/api`) |
| Form safety | `BaseServlet.csrfToken()` | Every POST form carries a per-session token |
| PRG pattern | `BaseServlet.redirect()` | After a POST, redirect with a one-time "flash" message |

Portal pages: Dashboard (live, refreshes from `/api/state`), Send money, History (JDBC, filters, paging, delete), Opportunity (JPA, legs), Alerts, Currencies, Rubric map, Login, Register, Error.

## CAP-7: Spring Boot, Hibernate / JPA, REST

| Rubric idea | File |
|---|---|
| Entities and mapping | `cap7/entity/OpportunityEntity` (`@OneToMany`), `OpportunityLegEntity` (`@ManyToOne`, `@IdClass` composite key), `CurrencyEntity`, `ProfitAlert` |
| Repositories | `cap7/repository/*` — derived queries (`findByActiveTrue`), JPQL (`JOIN FETCH`, `GROUP BY`), native SQL, paging (`Pageable`) |
| Services and transactions | `cap7/service/*` — `@Transactional`, read-only queries, dirty checking, `@Scheduled` alert check |
| DTOs and validation | `cap7/dto/Dtos` — records, `@NotBlank`, `@Pattern`, `@Positive`, `@DecimalMax` |
| REST controllers | `cap7/rest/*` — GET, POST (201 + Location), PUT, PATCH, DELETE (204) |
| Error handling | `cap7/rest/ApiExceptionHandler` — 400 with field errors, 404, 409, 503 |
| CORS | `cap7/config/WebConfig` |
| Tests | `src/test/java/com/arbitrage/cap7/Cap7Test` (MockMvc + Mockito), `cap6/Cap6Test` |

### REST endpoints

```
GET    /api/state                              live engine (used by the web desk)
POST   /api/pause?paused=true
GET    /api/v1/opportunities?period=today&minProfit=0.3&code=INR&page=0&size=20
GET    /api/v1/opportunities/{id}
DELETE /api/v1/opportunities/{id}
GET    /api/v1/opportunities/summary | top | pairs | per-day
GET    /api/v1/currencies          POST /api/v1/currencies
GET    /api/v1/currencies/{code}   PUT  /api/v1/currencies/{code}   DELETE /api/v1/currencies/{code}
GET    /api/v1/alerts?owner=demo   POST /api/v1/alerts   PATCH /api/v1/alerts/{id}?active=false   DELETE /api/v1/alerts/{id}
GET    /api/v1/engine/settings     PUT  /api/v1/engine/settings
GET    /api/v1/route?from=INR&to=JPY&amount=50000
```

## Real-life use

- **Send money the cheapest way** (`cap7/service/RouteService`, portal `/portal/route`, web `/desk/send`). Banks quote rare pairs with wide spreads, so INR → USD → JPY often beats INR → JPY directly. The planner runs the same shortest-path idea as Bellman-Ford on −log(rate) and shows the saving.
- **Profit alerts** (`AlertService`, portal Alerts page, web Settings): set a level once and get told when a loop reaches it.

## Demo order (about 10 minutes)

1. `mvn test`: 42 Java tests. `cd web && npm test`: 14 web tests.
2. `run-all.bat`: MySQL, the feed server, the Swing app, the web desk.
3. Swing window: a loop appears; explain Bellman-Ford.
4. Browser `http://127.0.0.1:8080/portal`: log in as `demo` / `demo1234`, the dashboard updates live.
5. History → open an opportunity (JPA, one-to-many legs). Delete one (POST + redirect).
6. Send money: INR 50,000 → JPY, see the route through USD and the saving.
7. Alerts: add "≥ 0.5 %", wait a few seconds, it fires.
8. REST: open `/api/v1/opportunities/summary`, `/api/v1/route?...`; show a 404 and a 400 JSON error.
9. Rubric map page: every CAP and its code.
10. Log out: the Back button cannot reopen the pages (no-store + filter).
