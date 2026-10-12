# Build Plan — CAP-1 to CAP-5

Build the milestones **in order**. At the end of each milestone:
1. run `mvn -q compile` and `mvn test`;
2. run the app and check the milestone checklist;
3. update `README.md` and `docs/DECISIONS.md`;
4. suggest a git commit message `feat: <milestone>` and a tag (`cap-1`, `cap-3`, `cap-5`);
5. print the checklist with ✅/❌ and **STOP. Wait for the student to say `continue`.**

Never start CAP-6/7/8 work (Servlet, JSP, sessions, login, Hibernate, REST).

---

## M0 — Project skeleton (½ day)

- [ ] Maven project `com.arbitrage:arbitrage:1.0.0`, Java 17, parent `spring-boot-starter-parent` 3.3.x.
- [ ] Dependencies exactly as in `CLAUDE.md` (add `mysql-connector-j` now, scope `runtime`; it is unused until M5).
- [ ] Package folders from `03_TECHNICAL_SPEC.md §3` (only create files when a milestone needs them).
- [ ] `application.properties` from §9 with `db.password=CHANGE_ME`. `.gitignore` for `target/`, IDE files.
- [ ] `README.md` stub: what it is, how to run.

**Check:** `mvn -q compile` passes.

---

## M1 — CAP-1: Core engine (OOP + Collections)

Build: `model/*` (Currency, RateSnapshot, Edge, Opportunity, OpportunityLeg, FeedSource), `config/AppSettings`,
`engine/*` (GraphBuilder, BellmanFordDetector, ProfitCalculator, TriangleValidator, OpportunityStore, TickStats, ArbitrageService without the DAO),
`feed/RateProvider`, `feed/SimulatedRateProvider`, `feed/RateStore`, `feed/RateFeedManager` (LOCAL only), `feed/TickScheduler`,
`validation/*`, `service/CurrencyService` (in-memory list only), `service/StartupService` (simple version: loads `Currency.defaults()` into the simulator and sets `TickScheduler.ready = true`; M3 extends it with the database steps), and `ArbitrageApplication` printing each opportunity with the logger (no UI yet).

Tests: `BellmanFordDetectorTest`, `ProfitCalculatorTest` (0.3472% reference case), `SimulatedRateProviderTest`,
`CurrencyValidatorTest`, `SettingsValidatorTest`.

Collections that must be visible in this milestone (write them down in DECISIONS.md):
`ArrayList` (edges), `List.of`/`List.copyOf` (immutable model data), `HashMap<String,Integer>` (code → index in CurrencyService),
`HashSet` (duplicate-code check), `ArrayDeque` (rolling tick times), `CopyOnWriteArrayList` (OpportunityStore).

**Checklist**
- [ ] `mvn test` green (≥ 5 test classes).
- [ ] `mvn spring-boot:run` logs `Arbitrage found: X → Y → Z → X  +0.4xx%` lines every few seconds.
- [ ] No class is over 150 lines; every class has a 1–3 line Javadoc.
- [ ] `docs/01_PROJECT_PROPOSAL.md` copied into the repo docs (it is the CAP-1 deliverable).

---

## (CAP-2 — no code. CAP-2 is the LinkedIn course + reflection report; the student does it.)

---

## M2 — CAP-3: Swing GUI (Collections, Swing components, layouts, events)

Build: `ui/theme/*` (Theme, Card, AppButton, NavItem, StatusChip, StatCard, Tables, FormRow, BackgroundTask),
`ui/UiServices`, `ui/MainWindow`, `ui/Sidebar`, `ui/TopBar`, `ui/screens/BaseScreen`,
`DashboardScreen` + `NetworkPanel`, `CurrenciesScreen` (CRUD on the **in-memory** list via CurrencyService),
`SettingsScreen` (Market card only, in-memory), and `FeedScreen` (Local mode only; the socket controls are visible but disabled with the hint "Available in Phase II").
`ArbitrageApplication` now opens the window on the EDT (`headless(false)`).

Follow `04_DESIGN_SPEC.md` exactly: tokens, components, layouts, states.

Layout managers that must appear (list them in DECISIONS.md): `BorderLayout` (frame, cards), `CardLayout` (screens),
`GridLayout` (stat row), `GridBagLayout` (dashboard body, forms), `FlowLayout` (button rows), `BoxLayout` (sidebar).

Event handling that must appear: `ActionListener` lambdas (buttons, nav), `ListSelectionListener` (currencies table fills the form),
`MouseAdapter` (button hover, double-click), `ChangeListener` (spinners), `WindowAdapter` (confirm on close while connected),
and Swing `Timer` (1 s refresh, toast timeout).

**Checklist**
- [ ] The window opens at 1280×800 and looks like the wireframes; nothing clips at 1100×700.
- [ ] Within ~10 s, an opportunity appears: toast, red cycle with rate labels, row in Recent, stat cards update.
- [ ] Pause/Resume works; the chip changes; the UI stays smooth.
- [ ] Currencies: add AUD → it appears in the graph; validation errors show inline; USD cannot be deleted.
- [ ] Settings: change the fee → it affects the next tick; invalid values are rejected inline.
- [ ] Empty states: deactivate down to 2 currencies → the dashboard shows the empty state; reactivate → it recovers.
- [ ] No Swing call happens off the EDT (search for `invokeLater`/`Timer` usage; add a comment where it matters).
- [ ] Screenshots of all screens are in `docs/screenshots/cap3-*.png`.
- [ ] `docs/USER_MANUAL.md` part 1 (Dashboard, Currencies, Settings).

---

## (CAP-4 — coding exam. Project task: a 30-minute quality pass only.)

- [ ] Every catch block either shows the user a friendly message or logs with context; no empty catch.
- [ ] Every user input goes through a validator.
- [ ] Remove dead code; check names and Javadocs.

---

## M3 — CAP-5 part A: JDBC + MySQL

Build: `db/*` (DatabaseConnection, DatabaseInitializer, DatabaseException, CurrencyDao, OpportunityDao, SettingsDao),
`service/StartupService`, and `sql/schema.sql` + `sql/seed.sql` (copy them from the spec pack).
Wire `CurrencyService` → `CurrencyDao`, `ArbitrageService` → `OpportunityDao.insertWithLegs`, and `SettingsScreen` → `SettingsDao` + the Database card.
Add `HistoryScreen` + `OpportunityDialog`. The top-bar DB chip uses `DatabaseConnection.online`.

**Checklist** (= `05_DATABASE_SPEC.md §6`, all 7 steps)
- [ ] Tables are created automatically; seed data appears once.
- [ ] Each opportunity is saved with its legs in one transaction (`setAutoCommit(false)`, `commit`, `rollback`).
- [ ] History: filters, sorting, summary line, details dialog, delete selected, clear all (typed confirm).
- [ ] Currency CRUD persists across restarts; FK protection message works.
- [ ] Settings persist across restarts; Test connection shows the ms or the error.
- [ ] MySQL stopped → the app starts, the chip is red, History shows offline + Retry, and detection continues.
- [ ] Every SQL statement uses `PreparedStatement` (except the DDL in DatabaseInitializer).

---

## M4 — CAP-5 part B: Socket networking

Build: `feed/FeedProtocol`, `feed/FeedException`, `feed/SocketRateProvider`, `RateFeedManager` (SOCKET mode),
the socket section of `FeedScreen`, `server/*` (FeedServerApp, FeedServer, ClientHandler, ServerWindow), and `FeedProtocolTest`.

**Checklist** (= `06_NETWORKING_SPEC.md §5` demo script)
- [ ] The server starts, shows clients, and broadcasts every second; port-in-use gives a friendly error.
- [ ] The desktop connects; the log shows HELLO/WELCOME/SNAPSHOT; counters move; opportunities are saved with `source = SOCKET`.
- [ ] Two desktops receive the same snapshots at the same time.
- [ ] Server stopped → the desktop shows Disconnected within 5 s and offers to switch to Local.
- [ ] A wrong host/port fails within ~3 s with a clear message; the UI never freezes.
- [ ] `FeedProtocolTest` is green.

---

## M5 — CAP-5 part C: Integration + documentation

- [ ] `README.md`: requirements (JDK 17, Maven, MySQL 8), setup (create the DB user/password, edit properties),
      how to run both programs, a troubleshooting table (DB offline, port in use, HeadlessException).
- [ ] `docs/USER_MANUAL.md`: every screen with a screenshot and steps.
- [ ] `docs/ARCHITECTURE.md`: a short version of the layer diagram, thread table, ER diagram, protocol table.
- [ ] `docs/screenshots/cap5-*.png` for all screens and the server window.
- [ ] `docs/08_RUBRIC_MAPPING.md` updated with the real file names.
- [ ] Final run of every checklist above. **STOP here** — CAP-6 onwards comes later.
