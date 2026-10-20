# Architecture (short version)

This page is the reference for the code structure, database and networking design.
This page is the summary to explain the project from.

## 1. Layers

```
 ┌──────────────────────────── Arbitrage Desktop (one JVM) ───────────────────────────┐
 │  UI (Swing, EDT)       MainWindow ─ Sidebar ─ TopBar ─ 5 screens                    │
 │        │ reads every second (Swing Timer)     │ DB / network via BackgroundTask     │
 │        ▼                                      ▼                                     │
 │  Engine (scheduler thread)               Database (JDBC DAOs)                       │
 │  TickScheduler → RateFeedManager → GraphBuilder → BellmanFordDetector               │
 │               → ProfitCalculator → OpportunityStore + OpportunityDao.insertWithLegs │
 │        ▲                                                                            │
 │  Feed: RateProvider ◄── SimulatedRateProvider | SocketRateProvider ──┐              │
 └──────────────────────────────────────────────────────────────────────┼──────────────┘
                                                             TCP :5050  │ text lines
 ┌─────────── Rate Feed Server (separate JVM, plain Java + Swing) ──────▼──────────────┐
 │  FeedServer (ServerSocket) → ClientHandler per client ← broadcaster every 1 s       │
 │  uses SimulatedRateProvider + FeedProtocol (shared code)       ServerWindow (Swing) │
 └─────────────────────────────────────────────────────────────────────────────────────┘
```

**The one rule:** background threads *write* to thread-safe stores; the UI *reads* them on a
Swing Timer. Background code never touches Swing; the UI never runs SQL or socket calls on the EDT.

## 2. Packages

| Package | Classes | Job |
|---|---|---|
| `model` | `Currency`, `RateSnapshot`, `Edge`, `Opportunity`, `OpportunityLeg`, `FeedSource` | Immutable data (records) |
| `config` | `AppSettings` | Fee, spread, inject chance |
| `validation` | `CurrencyValidator`, `SettingsValidator`, `ValidationException` | Check all user input |
| `engine` | `GraphBuilder`, `BellmanFordDetector`, `ProfitCalculator`, `TriangleValidator`, `OpportunityStore`, `TickStats`, `ArbitrageService` | The algorithm |
| `feed` | `RateProvider`, `SimulatedRateProvider`, `SocketRateProvider`, `FeedProtocol`, `FeedLog`, `FeedStatus`, `FeedException`, `RateFeedManager`, `RateStore`, `TickScheduler` | Where rates come from |
| `db` | `DatabaseConnection`, `DatabaseInitializer`, `CurrencyDao`, `OpportunityDao`, `SettingsDao`, `DatabaseException` | JDBC |
| `service` | `StartupService`, `CurrencyService` | Startup and currency CRUD |
| `server` | `FeedServerApp`, `FeedServer`, `ClientHandler`, `ClientInfo`, `ServerWindow` | The second program |
| `ui` | `MainWindow`, `Sidebar`, `TopBar`, `UiServices` | Window frame |
| `ui.theme` | `Theme`, `Card`, `AppButton`, `NavItem`, `StatusChip`, `StatCard`, `FormRow`, `EmptyState`, `RoundedBorder`, `Tables`, `Formats`, `Ui`, `BackgroundTask` | Design system |
| `ui.screens` | `BaseScreen`, `DashboardScreen`, `NetworkPanel`, `HistoryScreen`, `HistoryFilters`, `HistoryTable`, `OpportunityDialog`, `CurrenciesScreen`, `CurrencyForm`, `FeedScreen`, `FeedStatusCard`, `SettingsScreen`, `DatabaseCard`, `OfflineCard` | The five screens |

## 3. One tick, step by step

1. Spring calls `TickScheduler.tick()` every 1000 ms on thread `scheduling-1`.
2. `RateFeedManager.current()` returns the simulator or the socket client. Both are a `RateProvider`,
   so the scheduler does not care which one it is (**polymorphism**).
3. `fetchLatest()` gives a `RateSnapshot` (or `null` when the socket has nothing new).
4. `GraphBuilder` makes one `Edge` per currency pair with weight `−log(rate × (1 − fee))`.
5. `BellmanFordDetector.findCycle` looks for a **negative cycle** = a loop whose rates multiply to more than 1.
6. `ProfitCalculator` turns the cycle into a percent and into legs.
7. `ArbitrageService` saves it with `OpportunityDao.insertWithLegs` (one transaction) and adds it to `OpportunityStore`.
8. One second later the Swing Timer in `MainWindow` reads the stores and repaints.

**Why −log?** Profit means `r1 × r2 × r3 > 1`. Taking the log gives `log r1 + log r2 + log r3 > 0`,
and with a minus sign `(−log r1) + (−log r2) + (−log r3) < 0`: a negative cycle, which is exactly
what Bellman-Ford can detect.

## 4. Threads

| Thread | Created by | Does | Touches |
|---|---|---|---|
| `main` | JVM | `Theme.install()`, Spring start, `StartupService.start()`, `invokeLater` | then ends |
| `scheduling-1` | Spring `@Scheduled` | tick: fetch → detect → save | stores (write), `OpportunityDao` |
| `AWT-EventQueue-0` (EDT) | Swing | all UI, Swing Timers | stores (read only) |
| `SwingWorker-*` | `BackgroundTask` | DB queries and socket connect for the screens | DAOs, `RateFeedManager` |
| `feed-reader` (daemon) | `SocketRateProvider.connect` | reads lines from the server | latest snapshot, counters, log |
| `feed-acceptor` | `FeedServer.start` | `accept()` loop | starts `client-N` threads |
| `client-N` | `FeedServer.acceptLoop` | one `ClientHandler`: HELLO, PING, BYE | client list |
| `feed-broadcaster` | `FeedServer.start` (`ScheduledExecutorService`) | sends one snapshot per second to all | client list |

Thread-safety tools used: immutable records, `AtomicReference`, `AtomicLong`, `CopyOnWriteArrayList`,
`volatile` flags, and `synchronized` on a few small methods.

## 5. Database (MySQL 8, database `arbitrage_db`)

```
┌──────────────────────┐        ┌────────────────────────────┐        ┌──────────────────────────┐
│ currency             │        │ opportunity                │        │ opportunity_leg          │
├──────────────────────┤        ├────────────────────────────┤        ├──────────────────────────┤
│ PK code CHAR(3)      │◄──┐    │ PK id BIGINT AUTO_INCREMENT│◄───────┤ PK,FK opportunity_id     │
│    name VARCHAR(40)  │   │    │    path_text VARCHAR(120)  │  1   N │ PK    leg_no TINYINT     │
│    usd_price DECIMAL │   ├────┤    path_length TINYINT     │        │ FK    from_code CHAR(3)  │
│    active BOOLEAN    │   │    │    profit_percent DECIMAL  │        │ FK    to_code CHAR(3)    │
│    created_at        │   │    │    fee DECIMAL             │        │       rate DECIMAL       │
│    updated_at        │   │    │    source VARCHAR(10)      │        └──────────────────────────┘
└──────────────────────┘   │    │    detected_at DATETIME(3) │         from_code, to_code → currency
                           │    └────────────────────────────┘
                           └── referenced by opportunity_leg (ON DELETE RESTRICT)

┌──────────────────────────┐
│ app_setting              │   key / value: fee, spread, inject_chance
├──────────────────────────┤
│ PK setting_key VARCHAR   │
│    setting_value VARCHAR │
│    updated_at            │
└──────────────────────────┘
```

- `opportunity` 1 → N `opportunity_leg`, `ON DELETE CASCADE`: deleting an opportunity deletes its legs.
- `opportunity_leg` → `currency`, `ON DELETE RESTRICT`: a currency used in history cannot be deleted.
- `CHECK` constraints: price > 0, source is `LOCAL` or `SOCKET`. Index on `detected_at`.

JDBC style, the same in every DAO method:

```java
String sql = "SELECT 1 FROM currency WHERE code = ?";
try (Connection connection = database.getConnection();            // DriverManager.getConnection
     PreparedStatement statement = connection.prepareStatement(sql)) {
    statement.setString(1, code);                                  // value sent separately from the SQL
    try (ResultSet rs = statement.executeQuery()) {
        return rs.next();
    }
} catch (SQLException e) {
    throw database.failure("Could not check the currency", e);     // friendly DatabaseException
}
```

| Class | Methods |
|---|---|
| `CurrencyDao` | `findAll`, `exists`, `insert`, `update`, `delete` |
| `OpportunityDao` | `insertWithLegs` (transaction + generated key + batch), `search`, `summary`, `findLegs`, `delete`, `deleteAll` |
| `SettingsDao` | `loadAll`, `saveAll` (transaction + batch) |
| `DatabaseInitializer` | `createTables` (DDL with `Statement`), `seedIfEmpty` |

## 6. Socket protocol (UTF-8 text, one message per line)

| Direction | Message | Example | Meaning |
|---|---|---|---|
| C → S | `HELLO <clientName>` | `HELLO desk-1` | first line after connecting |
| S → C | `WELCOME <serverName> <intervalMs>` | `WELCOME RateFeedServer 1000` | handshake OK |
| S → C | `SNAPSHOT <epochMillis> <codes> <rates>` | see below | one full rate matrix |
| C → S | `PING` | `PING` | sent after 5 s of silence |
| S → C | `PONG` | `PONG` | reply to PING |
| both | `BYE` | `BYE` | polite close |
| S → C | `ERROR <text>` | `ERROR expected HELLO` | protocol error, then close |

```
SNAPSHOT 1760261111000 USD,EUR,GBP 1.00000000,0.92500000,0.78661000,1.07892000,1.00000000,0.85634000,1.26873000,1.16543000,1.00000000
```

`FeedProtocol.encode` writes this line and `FeedProtocol.decode` checks it: exactly 4 parts, the word
`SNAPSHOT`, a numeric timestamp, 3–20 codes of 3 uppercase letters, exactly n × n positive rates, and 1 on
the diagonal. Anything else throws `FeedException("Bad snapshot: …")` and is counted as a bad message.

## 7. Error handling

| Exception | Type | Thrown by | Shown as |
|---|---|---|---|
| `ValidationException(field, message)` | checked | validators | red text under the field |
| `DatabaseException(userMessage, cause)` | unchecked | DAOs (wrap `SQLException`) | dialog with the friendly message; DB chip turns red on connection errors |
| `FeedException(message, cause)` | checked | `SocketRateProvider.connect`, `FeedProtocol.decode` | dialog on the Feed screen; bad-message counter |

If MySQL is down at startup, `StartupService` catches the `DatabaseException`, loads
`Currency.defaults()` and keeps the default settings. Detection runs in memory until **Retry** works.
