# Rubric Mapping (what to show the faculty)

Use this table during the demo. Each rubric line has the feature that earns it, the **real file to open**
(all paths start at `src/main/java/com/arbitrage/`), and what to say.

## CAP-1 — Quiz + Project Proposal (20)

| Criterion | Marks | Evidence | Open this |
|---|---|---|---|
| Conceptual understanding (OOP, Collections) | 5 | Records, an interface with two implementations, the collections table | `model/Currency.java`, `model/RateSnapshot.java`, `feed/RateProvider.java`, `docs/DECISIONS.md` §4 |
| Problem identification | 5 | Proposal §1: inconsistent rates → profitable loops that vanish quickly | `docs/01_PROJECT_PROPOSAL.md` |
| Project planning | 5 | Proposal §6 milestones + build plan | `docs/01_PROJECT_PROPOSAL.md`, `docs/07_BUILD_PLAN.md` |
| Presentation & justification | 5 | Why Bellman-Ford (finds negative cycles, works for loops longer than 3), why −log(rate) | `engine/BellmanFordDetector.java`, `engine/GraphBuilder.java`, `docs/ARCHITECTURE.md` §3 |

Tests to run for CAP-1: `BellmanFordDetectorTest`, `ProfitCalculatorTest` (the 0.3472 % reference case),
`SimulatedRateProviderTest`, `CurrencyValidatorTest`, `SettingsValidatorTest` in `src/test/java/com/arbitrage/`.

## CAP-3 — Phase I: Collections & Swing GUI (30)

| Criterion | Marks | Evidence | Open this |
|---|---|---|---|
| OOP design & code structure | 5 | Packages by layer; abstract `BaseScreen` with `onShow()` overridden by five screens (polymorphism); `RateProvider` interface; `StatCard extends Card` (inheritance); `UiServices` (has-a) | `ui/screens/BaseScreen.java`, `ui/MainWindow.java` (`showScreen`), `feed/RateFeedManager.java` (`current()`), `ui/theme/StatCard.java` |
| Appropriate use of Collections | 5 | `ArrayList` edges, `HashMap` code→index, `HashSet` duplicate check, `ArrayDeque` rolling tick times, `CopyOnWriteArrayList` store, `LinkedHashMap` screens, `TableRowSorter` | `engine/GraphBuilder.java`, `service/CurrencyService.java`, `engine/TickStats.java`, `engine/OpportunityStore.java`, `ui/MainWindow.java`, `ui/screens/HistoryTable.java`; full list in `docs/DECISIONS.md` §4 |
| GUI design & layout | 5 | Sidebar + `CardLayout`, `GridLayout` stat row, `GridBagLayout` 60/40 split and form, `BoxLayout`, `FlowLayout` button rows, custom-painted `Card` / `AppButton` / `StatusChip`, the network graph | `ui/MainWindow.java`, `ui/screens/DashboardScreen.java`, `ui/screens/CurrencyForm.java`, `ui/theme/Card.java`, `ui/theme/AppButton.java`, `ui/screens/NetworkPanel.java`, `ui/theme/Theme.java`; list in `docs/DECISIONS.md` §5 |
| Event handling & functionality | 5 | Nav clicks, Pause / Resume, table selection → form, spinners, toast timer, 1 s refresh timer, confirm on close | `ui/Sidebar.java`, `ui/TopBar.java`, `ui/screens/CurrenciesScreen.java`, `ui/screens/SettingsScreen.java`, `ui/MainWindow.java`; list in `docs/DECISIONS.md` §6 |
| Code quality & exception handling | 5 | `ValidationException` shown under the field, scheduler try/catch so one bad tick never stops the app, small classes with Javadoc | `validation/CurrencyValidator.java`, `validation/ValidationException.java`, `ui/theme/FormRow.java`, `feed/TickScheduler.java` |
| Demonstration & documentation | 5 | Live demo + user manual Part 1 + screenshots | `docs/USER_MANUAL.md`, `docs/screenshots/cap3-*.png` |

## CAP-5 — Phase II: JDBC & Networking (30)

| Criterion | Marks | Evidence | Open this |
|---|---|---|---|
| Database design | 5 | 4 tables, PK / FK, 1-to-N legs, CASCADE vs RESTRICT, CHECK constraints, index | `sql/schema.sql`, `docs/ARCHITECTURE.md` §5, `db/DatabaseInitializer.java` |
| JDBC connectivity & CRUD | 7 | `DriverManager.getConnection`; full CRUD with `PreparedStatement`; transaction + generated keys + batch | `db/DatabaseConnection.java`, `db/CurrencyDao.java`, `db/OpportunityDao.java` (`insertWithLegs`), `db/SettingsDao.java` |
| Networking (socket programming) | 7 | `ServerSocket`, one thread per client, broadcaster; `Socket` client with a reader thread; text protocol | `server/FeedServer.java`, `server/ClientHandler.java`, `feed/SocketRateProvider.java`, `feed/FeedProtocol.java` |
| Exception handling & validation | 4 | Friendly `DatabaseException` messages, FK violation message, DB-offline mode, 3 s connect timeout, bad-message counter, port-in-use message | `db/DatabaseConnection.java` (`failure`), `db/CurrencyDao.java` (`delete`), `service/StartupService.java`, `ui/screens/OfflineCard.java`, `feed/SocketRateProvider.java`, `server/ServerWindow.java` (`showProblem`) |
| User interface & integration | 3 | History, Currencies, Feed and Settings on top of DB + network; the same engine for both feed modes | `ui/screens/HistoryScreen.java`, `ui/screens/FeedScreen.java`, `ui/screens/DatabaseCard.java`, `feed/TickScheduler.java` |
| Demonstration & documentation | 4 | Two-program demo script, README, user manual, architecture, screenshots | `README.md`, `docs/USER_MANUAL.md`, `docs/ARCHITECTURE.md`, `docs/screenshots/cap5-*.png` |

Test to run for CAP-5: `src/test/java/com/arbitrage/feed/FeedProtocolTest.java`.

## Demo order (about 8 minutes)

1. `mvn test` → 31 tests green. Open `ProfitCalculatorTest` and point at 0.3472 %.
2. Start Arbitrage Desktop → Dashboard: wait for a red cycle, press Pause, explain the graph.
3. Currencies: add `AUD / Australian Dollar / 0.66` → it appears in the graph. Show a validation error.
4. History: filter, sort, double-click for the legs. Show the same rows in MySQL
   (`SELECT * FROM opportunity ORDER BY id DESC LIMIT 5;`).
5. Start Rate Feed Server → Start → Feed screen → Socket feed → Connect → show the protocol log.
6. Stop the server → the desktop offers to switch to Local.
7. Stop MySQL → `DB offline`, detection continues → start MySQL → Retry.

## Likely viva questions (short answers)

- **Statement vs PreparedStatement?** PreparedStatement uses `?` placeholders. Values are sent separately, so user input cannot change the SQL (prevents SQL injection), and it is faster to reuse. Only the `CREATE TABLE` statements in `DatabaseInitializer` use a plain `Statement`, because they contain no user values.
- **executeQuery vs executeUpdate?** `executeQuery` is for SELECT and returns a `ResultSet`. `executeUpdate` is for INSERT, UPDATE, DELETE and DDL, and returns the row count.
- **Why a transaction in insertWithLegs?** An opportunity and its legs must be saved together or not at all. `setAutoCommit(false)`, `commit` on success, `rollback` on error.
- **Why does the server use one thread per client?** `readLine()` blocks. A separate thread lets each client be served without waiting for the others.
- **How do the desktop and server agree on data?** A text protocol: `HELLO` / `WELCOME` / `SNAPSHOT` lines, encoded and decoded by `FeedProtocol`, which is unit-tested.
- **Why no DB or network calls on the EDT?** The EDT draws the whole UI. A slow query would freeze the window, so `BackgroundTask` (a `SwingWorker`) runs them in the background and brings the result back to the EDT.
- **What happens if MySQL is down?** `StartupService` catches the error, uses default currencies and settings, the DB chip turns red, and detection continues in memory. Retry reconnects.
- **Where is polymorphism?** `RateFeedManager.current()` returns a `RateProvider`; the scheduler calls `fetchLatest()` without knowing if it is the simulator or the socket client. Also `BaseScreen.onShow()`, called by `MainWindow.showScreen` for whichever screen is shown.
- **Why records?** Model data never changes after it is created, so it is safe to share between threads without locks.
- **Why `volatile` and `CopyOnWriteArrayList`?** One thread writes and another reads. `volatile` makes the new value visible to the other thread; `CopyOnWriteArrayList` lets the UI loop over the list while the scheduler adds to it.
