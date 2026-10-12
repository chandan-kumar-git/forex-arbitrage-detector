# Technical Specification

## 1. Stack

| Item | Choice |
|---|---|
| Language | Java 17 |
| Build | Maven, `spring-boot-starter-parent` 3.3.x |
| Framework | Spring Boot core only: DI, `@Component`, `@Scheduled`, `@Value`, SLF4J logging |
| UI | Java Swing (Nimbus look-and-feel tuned by `Theme`) |
| Database | MySQL 8, JDBC via `DriverManager` + `PreparedStatement` (driver: `mysql-connector-j`) |
| Networking | `java.net.ServerSocket` / `Socket`, line-based text protocol |
| Tests | JUnit 5 (from `spring-boot-starter-test`) |

## 2. Architecture (layers)

```
 ┌──────────────────────────── Arbitrage Desktop (one JVM) ──────────────────────────┐
 │  UI (Swing, EDT)        MainWindow ─ Sidebar ─ TopBar ─ 5 screens                  │
 │        │ reads every second (Swing Timer)    │ CRUD via BackgroundTask             │
 │        ▼                                     ▼                                     │
 │  Engine (scheduler thread)              Database (JDBC DAOs)                       │
 │  TickScheduler → RateFeedManager → GraphBuilder → BellmanFordDetector              │
 │               → ProfitCalculator → OpportunityStore (+ OpportunityDao.insert)      │
 │        ▲                                                                           │
 │  Feed: RateProvider ◄── SimulatedRateProvider | SocketRateProvider ──┐             │
 └──────────────────────────────────────────────────────────────────────┼─────────────┘
                                                            TCP :5050   │ text lines
 ┌─────────── Rate Feed Server (separate JVM, plain Java + Swing) ──────▼─────────────┐
 │  FeedServer (ServerSocket) → ClientHandler per client ← broadcaster every 1 s      │
 │  uses SimulatedRateProvider + FeedProtocol (shared code)      ServerWindow (Swing) │
 └────────────────────────────────────────────────────────────────────────────────────┘
```

**Rule of the architecture:** background threads **write** to thread-safe stores; the UI **reads**
them on a Swing Timer every second. Background code never calls Swing; the UI never runs SQL or
socket calls on the EDT.

## 3. Package and file layout

```
arbitrage/
├── pom.xml
├── README.md
├── docs/ (the spec files + DECISIONS.md + USER_MANUAL.md)
├── sql/schema.sql, sql/seed.sql
└── src/main/java/com/arbitrage/
    ├── ArbitrageApplication.java          main(): Spring start, then Swing on EDT
    ├── config/
    │   └── AppSettings.java               fee, spread, injectChance (volatile, defaults)
    ├── model/
    │   ├── Currency.java                  record (code, name, usdPrice, active) + defaults()
    │   ├── RateSnapshot.java              record (codes, rates[][], timestamp) – deep copy
    │   ├── Edge.java                      record (from, to, weight)
    │   ├── Opportunity.java               record (id, path, profitPercent, fee, source, detectedAt)
    │   ├── OpportunityLeg.java            record (legNo, fromCode, toCode, rate)
    │   └── FeedSource.java                enum LOCAL, SOCKET
    ├── validation/
    │   ├── ValidationException.java       checked; has field name + message
    │   ├── CurrencyValidator.java
    │   └── SettingsValidator.java
    ├── engine/
    │   ├── GraphBuilder.java
    │   ├── BellmanFordDetector.java
    │   ├── ProfitCalculator.java
    │   ├── TriangleValidator.java
    │   ├── OpportunityStore.java          in-memory recent list (CopyOnWriteArrayList, max 100)
    │   ├── TickStats.java                 counters + rolling tick time (ArrayDeque, 60)
    │   └── ArbitrageService.java          orchestrates one detection
    ├── feed/
    │   ├── RateProvider.java              interface
    │   ├── SimulatedRateProvider.java     plain class (no Spring), reused by server
    │   ├── SocketRateProvider.java        client: connect, reader thread, latest snapshot
    │   ├── FeedProtocol.java              static encode/decode of protocol lines
    │   ├── FeedException.java             checked
    │   ├── RateFeedManager.java           holds current mode + providers; switchTo(...)
    │   ├── RateStore.java                 AtomicReference<RateSnapshot>
    │   └── TickScheduler.java             @Scheduled every 1000 ms
    ├── db/                                (CAP-5)
    │   ├── DatabaseConnection.java        DriverManager.getConnection, online flag
    │   ├── DatabaseInitializer.java       CREATE TABLE IF NOT EXISTS + seed
    │   ├── DatabaseException.java         unchecked wrapper with friendly message
    │   ├── CurrencyDao.java
    │   ├── OpportunityDao.java
    │   └── SettingsDao.java
    ├── service/
    │   ├── StartupService.java            runs once after Spring start (DB, settings, currencies)
    │   └── CurrencyService.java           validation + list in memory + DAO (CRUD entry point)
    ├── server/                            (CAP-5, no Spring)
    │   ├── FeedServerApp.java             main(): opens ServerWindow on EDT
    │   ├── FeedServer.java                ServerSocket, accept thread, broadcaster
    │   ├── ClientHandler.java             one thread per client
    │   └── ServerWindow.java              Swing window for the server
    └── ui/
        ├── UiServices.java                @Component bundle of beans the UI needs
        ├── MainWindow.java                JFrame: Sidebar WEST, TopBar NORTH, CardLayout CENTER
        ├── Sidebar.java
        ├── TopBar.java
        ├── theme/
        │   ├── Theme.java                 colours, fonts, spacing, Nimbus setup
        │   ├── Card.java                  rounded white panel with title
        │   ├── AppButton.java             custom-painted button (PRIMARY, SECONDARY, DANGER)
        │   ├── NavItem.java               sidebar entry
        │   ├── StatusChip.java            pill label (GOOD, WARN, BAD, INFO)
        │   ├── StatCard.java              big number + caption
        │   ├── Tables.java                static styleTable(JTable) + cell renderers
        │   ├── FormRow.java               label + field + inline error label
        │   └── BackgroundTask.java        tiny SwingWorker wrapper
        └── screens/
            ├── BaseScreen.java            abstract JPanel: title, subtitle, onShow()
            ├── DashboardScreen.java
            ├── NetworkPanel.java          custom painting of the graph
            ├── HistoryScreen.java         (CAP-5)
            ├── OpportunityDialog.java     legs of one opportunity (CAP-5)
            ├── CurrenciesScreen.java
            ├── FeedScreen.java
            └── SettingsScreen.java
src/main/resources/application.properties
src/test/java/com/arbitrage/...  (tests listed in §10)
```

## 4. Model

All model types are **records** (immutable). `RateSnapshot` makes a deep copy of the matrix in its
compact constructor and exposes `size()`, `rate(i, j)` and `code(i)`. Never expose the raw array to callers.

```java
public record Currency(String code, String name, double usdPrice, boolean active) {
    /** Built-in list used when the database is offline and by the feed server. */
    public static List<Currency> defaults() {
        return List.of(
            new Currency("USD", "US Dollar", 1.0, true),
            new Currency("EUR", "Euro", 1.08, true),
            new Currency("GBP", "British Pound", 1.27, true),
            new Currency("INR", "Indian Rupee", 0.012, true),
            new Currency("JPY", "Japanese Yen", 0.0067, true));
    }
}
```

`Opportunity.path()` is a `List<String>` like `[INR, EUR, GBP, INR]`; add `pathText()` that joins with `" → "`.
`id` is `0` until the opportunity is saved.

## 5. Engine (same algorithm as the verified reference implementation)

- **GraphBuilder.build(snapshot, fee)** → `List<Edge>` of n·(n−1) edges, weight `-Math.log(rate * (1 - fee))`.
- **BellmanFordDetector.findCycle(edges, n)** → `List<Integer>` cycle like `[3, 1, 2, 3]`, or `List.of()`. Use exactly this logic:

```java
public List<Integer> findCycle(List<Edge> edges, int n) {
    double[] dist = new double[n];            // all 0.0 = virtual source
    int[] parent = new int[n];
    Arrays.fill(parent, -1);
    int lastUpdated = -1;
    for (int round = 1; round <= n; round++) { // round n is the check round
        lastUpdated = -1;
        for (Edge e : edges) {
            double candidate = dist[e.from()] + e.weight();
            if (candidate < dist[e.to()] - EPS) {   // EPS = 1e-12
                dist[e.to()] = candidate;
                parent[e.to()] = e.from();
                lastUpdated = e.to();
            }
        }
        if (lastUpdated == -1) {
            return List.of();                 // nothing changed: no negative cycle
        }
    }
    int v = lastUpdated;
    for (int i = 0; i < n; i++) {
        v = parent[v];                        // walk n steps back: now inside the cycle
    }
    List<Integer> cycle = new ArrayList<>();
    int current = v;
    do {
        cycle.add(current);
        current = parent[current];
    } while (current != v);
    cycle.add(v);
    Collections.reverse(cycle);
    return cycle;
}
```

- **ProfitCalculator.profitPercent(cycle, snapshot, fee)** → `(Π rate·(1−fee) − 1) × 100`.
- **ProfitCalculator.legs(cycle, snapshot)** → `List<OpportunityLeg>`.
- **TriangleValidator.findProfitableTriangles(snapshot, fee)** → `List<List<Integer>>`. This is a brute-force cross-check; it logs a warning if it disagrees with Bellman-Ford.
- **ArbitrageService.detect(snapshot, source)**: build → find → if empty, validator check → profit → if > 0, create `Opportunity`, add to `OpportunityStore`, update `TickStats`, and (CAP-5) save it with `OpportunityDao.insertWithLegs` inside try/catch. On `DatabaseException`, mark the DB offline and keep going.
- **OpportunityStore**: `CopyOnWriteArrayList<Opportunity>`, newest first, max 100; `latest()`, `all()` (returns `List.copyOf`).
- **TickStats**: `AtomicLong ticks, found`; best profit; `ArrayDeque<Long>` of the last 60 tick durations (synchronized methods, since it is tiny); `averageMicros()`.

## 6. Feed

```java
public interface RateProvider {
    /** Latest snapshot, or null if none is available yet. */
    RateSnapshot fetchLatest();
    FeedSource source();
}
```

- **SimulatedRateProvider** (plain class): constructor `(List<Currency> currencies, AppSettings settings)`.
  It keeps a `double[]` of USD prices, does a random walk (volatility 0.0005), builds the matrix with
  spread, and injects a +0.6…+1.2% error into one random pair with probability `injectChance` (the
  reverse edge is divided by the same factor). It has a `synchronized setCurrencies(List<Currency>)` that
  uses only the active ones.
- **SocketRateProvider**: see `06_NETWORKING_SPEC.md`. `fetchLatest()` returns the last snapshot
  received; it returns `null` if it has not changed since the previous call, so a stale snapshot is not analysed twice.
- **RateFeedManager** (@Component): owns one `SimulatedRateProvider` and one `SocketRateProvider`,
  plus a `volatile FeedSource mode`. It has `current()`, `useLocal()`, `connectSocket(host, port)` (throws `FeedException`) and `disconnect()`.
- **RateStore**: `AtomicReference<RateSnapshot>` with `update` and `getLatest`.
- **TickScheduler** (@Component, `@Scheduled(fixedRate = 1000)`): if not ready or paused, return.
  Otherwise call `snapshot = manager.current().fetchLatest()`; if null, return; then `rateStore.update`
  and `service.detect`, timing the whole thing with `System.nanoTime()`. Catch `RuntimeException`
  and log it, so one failure never stops the schedule. `volatile boolean paused, ready`.

## 7. Spring wiring and startup

- `@SpringBootApplication @EnableScheduling ArbitrageApplication`. `main()` does:
  1. `System.setProperty` for text antialiasing; `Theme.install()` (Nimbus + colours + fonts).
  2. `ctx = new SpringApplicationBuilder(ArbitrageApplication.class).headless(false).run(args)`.
  3. `ctx.getBean(StartupService.class).start()`. This tests the DB, creates tables, seeds,
     loads settings and currencies into the simulator, then sets `TickScheduler.ready = true`.
     If the DB fails, it uses `Currency.defaults()` and `AppSettings` defaults, and marks the DB offline.
  4. `SwingUtilities.invokeLater(() -> new MainWindow(ctx.getBean(UiServices.class)).setVisible(true))`.
- `UiServices` is a @Component that just holds references (has-a): `AppSettings, RateFeedManager,
  RateStore, OpportunityStore, TickStats, TickScheduler, CurrencyService, OpportunityDao, SettingsDao,
  DatabaseConnection`. The UI gets everything from it, which avoids long constructor lists.
- In CAP-3 (before the db package exists), `CurrencyService` keeps the list only in memory.
  In CAP-5 it also calls `CurrencyDao`. Screen code does not change between CAP-3 and CAP-5.

## 8. Threads

| Thread | Created by | Does | Touches |
|---|---|---|---|
| `main` | JVM | Spring start, StartupService, invokeLater | then ends |
| `scheduling-1` | Spring | tick: fetch → detect → save | stores (write), DAO insert |
| `AWT-EventQueue-0` (EDT) | Swing | all UI, Swing Timers (1 s refresh) | stores (read) |
| `SwingWorker-*` | BackgroundTask | DB queries for screens | DAOs |
| `feed-reader` | SocketRateProvider | reads lines from server | latest snapshot (write) |
| Server: `feed-acceptor`, `client-N`, `feed-broadcaster` | FeedServer | accept, read, broadcast | client list |

Thread-safety tools: immutable records, `AtomicReference`, `AtomicLong`, `CopyOnWriteArrayList`,
`volatile` flags, and `synchronized` only on tiny methods (`TickStats`, `setCurrencies`).

## 9. Configuration — `application.properties`

```properties
spring.main.banner-mode=off
db.url=jdbc:mysql://localhost:3306/arbitrage_db?createDatabaseIfNotExist=true&connectTimeout=3000&socketTimeout=5000
db.username=root
db.password=CHANGE_ME
feed.default-host=localhost
feed.default-port=5050
logging.level.com.arbitrage=INFO
```

Fee, spread and inject chance are **not** in this file. They live in `AppSettings` (defaults
0.0005 / 0.001 / 0.30) and, from CAP-5, in the `app_setting` table.

## 10. Exceptions and validation

| Exception | Type | Thrown by | Shown as |
|---|---|---|---|
| `ValidationException(field, message)` | checked | validators | red inline text under the field |
| `DatabaseException(userMessage, cause)` | unchecked | DAOs (wraps `SQLException`) | dialog with the friendly message; DB chip turns red |
| `FeedException(message, cause)` | checked | `SocketRateProvider.connect`, `FeedProtocol.decode` | Feed screen status + dialog |

Validation rules (constants in the validator classes):
- Currency code `^[A-Z]{3}$`, unique; name 2–40 characters; usdPrice > 0 and ≤ 1,000,000; USD is always active and cannot be deleted.
- Fee 0–0.05, spread 0–0.02, injectChance 0–1. Host not blank; port 1024–65535.
- At least 3 active currencies are needed to detect (otherwise detection is skipped and the dashboard shows the empty state).

## 11. Tests (no MySQL needed)

| Test class | Checks |
|---|---|
| `BellmanFordDetectorTest` | known triangle found; fair market → empty; brute force agrees |
| `ProfitCalculatorTest` | INR→EUR→GBP→INR case gives 0.3472% (±0.0005) |
| `FeedProtocolTest` | encode → decode round trip; malformed line → `FeedException` |
| `CurrencyValidatorTest` | good input passes; bad code, empty name, price ≤ 0, duplicate, delete USD fail |
| `SettingsValidatorTest` | ranges enforced |
| `SimulatedRateProviderTest` | diagonal is 1; `r[i][j]*r[j][i] < 1` (no 2-cycle arbitrage) |

The reference case for the profit test (USD 1, EUR 1.08, GBP 1.27, INR 0.012; spread 0.001; EUR→GBP × 1.008, GBP→EUR ÷ 1.008; fee 0.0005)
gives the cycle INR→EUR→GBP→INR with **0.3472% profit**.
