# Product Specification

## 1. One-line summary

A Java Swing desktop app that receives forex rates every second (from a local simulator or a
socket feed server), detects profitable currency cycles with Bellman-Ford, highlights them on a live
network graph, and stores them in MySQL. A second small Swing program, the Rate Feed Server,
broadcasts rates to any number of connected apps.

## 2. Programs

| Program | Main class | Purpose |
|---|---|---|
| **Arbitrage Desktop** | `com.arbitrage.ArbitrageApplication` | The main app (Spring Boot + Swing) |
| **Rate Feed Server** | `com.arbitrage.server.FeedServerApp` | Plain Java + Swing; broadcasts rates over TCP |

## 3. Screens of Arbitrage Desktop

The left sidebar switches screens (CardLayout). The top bar is always visible.

| # | Screen | Purpose | Milestone |
|---|---|---|---|
| 1 | **Dashboard** | Stat cards, live network graph, recent opportunities | CAP-3 |
| 2 | **History** | Saved opportunities from MySQL: filter, view legs, delete | CAP-5 |
| 3 | **Currencies** | CRUD form + table for currencies (code, name, USD price, active) | CAP-5 (in-memory list in CAP-3) |
| 4 | **Feed** | Choose Local simulator or Socket feed; connect/disconnect; protocol log | CAP-5 (Local only in CAP-3) |
| 5 | **Settings** | Fee, spread, inject chance; save to DB; test DB connection | CAP-3 (in-memory), CAP-5 (saved) |

## 4. User stories with acceptance criteria

### Detection and dashboard (CAP-3)

**US-1 Live detection.** As an analyst, I want rates analysed every second so I can see arbitrage the moment it appears.
- A tick runs every 1000 ms while not paused.
- When a profitable cycle (profit after fees > 0) is found within ~1 s, it appears at the top of "Recent opportunities".
- The cycle's edges turn red on the network graph and stay red for 1.5 s.
- A green toast in the top bar says `New arbitrage: INR → EUR → GBP → INR  +0.347%` for 3 s.

**US-2 Pause and resume.** As an analyst, I want to pause the feed to study a moment.
- The Pause button changes to Resume. The tick counter stops, and the UI stays responsive.
- The status chip shows `Paused` (amber) or `Live` (green).

**US-3 Stats at a glance.** As an analyst, I want headline numbers.
- Four stat cards: **Ticks**, **Opportunities** (this session), **Best profit** (this session, %), **Avg tick time** (µs, rolling last 60 ticks).

**US-4 Readable network.** As an analyst, I want to see all currencies and the rates on the cycle.
- Nodes sit evenly on a circle; all edges are faint grey.
- Cycle edges are red with arrowheads, and each shows its rate as a small label.
- A legend at the bottom of the card says: grey = exchange pair, red = profitable cycle.
- With fewer than 3 active currencies, the card shows the empty state "Add at least 3 active currencies".

### Currencies (CAP-3 in memory, CAP-5 in MySQL)

**US-5 Manage currencies.** As an analyst, I want to add, edit, deactivate and delete currencies.
- The form has Code (3 uppercase letters), Name (2–40 characters), USD price (> 0), and Active (checkbox).
- **Add** rejects a duplicate code; the inline red message says "Code already exists".
- Clicking a table row fills the form; **Update** saves changes; **Clear** empties the form.
- **Delete** asks for confirmation. USD can never be deleted or deactivated (it is the base currency).
- A currency that appears in saved history cannot be deleted. The message says "Used in history – deactivate it instead".
- Changes take effect in the simulator from the next tick.

### History (CAP-5)

**US-6 Browse history.** As an analyst, I want every detected opportunity saved and searchable.
- Each opportunity and its legs are saved in one transaction.
- Filters: period (Last hour / Today / Last 7 days / All), minimum profit %, and a currency code contains-search. Results are sorted newest first, max 500 rows.
- The table can be sorted by clicking column headers.
- Double-clicking a row opens "Opportunity details" with the legs table (from, to, rate).
- **Delete selected** (with confirm) and **Clear all** (with typed confirmation `DELETE`).
- A summary line reads: `128 opportunities · avg 0.41% · best 0.74%`.

### Feed / networking (CAP-5)

**US-7 Connect to a feed server.** As an analyst, I want to receive rates from the Rate Feed Server.
- Mode radio buttons: **Local simulator** or **Socket feed**.
- For socket mode, enter Host (default `localhost`) and Port (default `5050`), then click **Connect**. The status goes Connecting → Connected (green).
- The log shows the protocol lines with time (`12:01:03  ← SNAPSHOT 1739… 5 currencies`).
- Counters show messages received, last message age, and bad messages.
- If the server stops, the status turns **Disconnected** (red), ticks pause automatically, and a dialog offers "Switch to Local simulator?".
- A wrong host or port gives a clear error within 3 s (connect timeout), not a frozen window.

**US-8 Run the feed server.** As a feed operator, I want a small window to run the server.
- Port field (default 5050), Start/Stop button, broadcast interval 1000 ms.
- A list shows connected clients (name + address); there are counters for clients and snapshots sent, plus a log area.
- Multiple desktop apps can connect at the same time; each receives the same snapshots.

### Settings (CAP-3 in memory, CAP-5 saved)

**US-9 Tune the market.** As an analyst, I want to change fee, spread and inject chance.
- Fee 0–5% (default 0.05%), spread 0–2% (default 0.1%), inject chance 0–100% (default 30%).
- **Save** validates and applies to the next tick; in CAP-5 it is also saved to MySQL and loaded at startup.
- **Reset defaults** restores the defaults.
- The "Database" card shows the JDBC URL and a **Test connection** button, which reports "Connected in 42 ms" or the error message.

### Robustness (CAP-5)

**US-10 Database offline.** If MySQL is down at startup, the app still opens. The top bar shows a red `DB offline` chip, History and Currencies show an empty state with "Retry", detection keeps working in memory, and no stack traces appear in dialogs.

## 5. Non-functional requirements

- Starts in under 5 s; a tick under 5 ms for 10 currencies.
- No UI freeze over 100 ms. All DB and network work happens off the EDT.
- Minimum window 1100×700, default 1280×800. Text never clips at that size.
- All user input is validated before use; all SQL uses PreparedStatement.
- Code is readable by a second-year student; see the simplicity rules in `docs/03_TECHNICAL_SPEC.md`.
