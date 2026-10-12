# Decisions

Choices made while building that the spec did not fix, or where the build differs from the spec.
Each one has the reason, so it can be explained in the viva.

## 1. Choices the spec left open

| # | Decision | Why |
|---|---|---|
| 1 | Project root is the folder that already held `CLAUDE.md`, `docs/` and `sql/` | The spec pack says to unzip into the project folder; nothing had to be copied |
| 2 | `spring-boot-starter-parent` **3.3.13** | Last release of the 3.3.x line the spec asks for |
| 3 | `<start-class>` in `pom.xml` points to `ArbitrageApplication` | The project has two `main` methods; `mvn spring-boot:run` needs to know which one |
| 4 | A Maven wrapper (`mvnw.cmd`, `.mvn/`) is included | Maven was not installed on the development laptop; the wrapper lets anyone build |
| 5 | `RateSnapshot.timestamp` is a `long` (epoch milliseconds) | The protocol sends epoch milliseconds, so no conversion is needed |
| 6 | Rate formula: `rate[i][j] = price[i] / price[j] × (1 − spread)` | This is the formula that reproduces the spec's 0.3472 % reference case |
| 7 | `Opportunity.withId(id)` | Records cannot be changed; after the INSERT a copy with the generated id is stored |
| 8 | Validators are `final` classes with `static` methods | They have no state, so no object is needed; simplest to call and to test |
| 9 | The code field is **not** upper-cased automatically (`aud` is rejected) | The rule "3 uppercase letters" stays visible; the History search box does upper-case |
| 10 | Socket client name is `desk-<process id>` | Two desktops on one laptop need different names without asking the user |
| 11 | Tick time is shown in µs, or in ms once it reaches 1000 µs | With MySQL a tick takes a few ms (one connection per insert); "13,320.0 µs" is hard to read |
| 12 | While paused, the network graph is frozen | US-2 says pause is for studying a moment; the red cycle stays visible |
| 13 | Dashboard "Recent" table writes the path without spaces (`INR→EUR→GBP→INR`) | As in the wireframe; it fits the 40 % card at 1100×700. History uses the long form |
| 14 | Closing the main window uses `DO_NOTHING_ON_CLOSE` + `WindowAdapter` + `System.exit(0)` | The build plan asks to confirm on close while connected; `EXIT_ON_CLOSE` cannot ask first |
| 15 | Test connection does a full reconnect when the database was offline | A plain test would mark the DB online although the tables were never created |
| 16 | Settings **Save** applies the values first, then saves to MySQL in the background | The market reacts at once; a DB problem only produces a message |
| 17 | The server logs every broadcast while at least one client is connected | Matches the wireframe and makes the demo visibly "alive" |
| 18 | Setting values are stored as plain decimals with 6 places (`0.000500`) | `String.valueOf(0.0005)` would store `5.0E-4`, which the spec forbids |
| 19 | A currency that MySQL already has (added by another desktop) is reported as "Code already exists" | Uses `CurrencyDao.exists`; avoids a raw duplicate-key error |
| 20 | Unknown-currency message names the whole path, not one code | A batch insert does not say which leg failed; the path is enough to act on |

## 2. Classes added beyond the spec's file list

All are small and exist to keep other classes short.

| Class | Job |
|---|---|
| `ui/theme/Formats` | One place for number and time formats (`%.3f%%`, `%.8f`, `HH:mm:ss`) |
| `ui/theme/EmptyState` | Centred "nothing to show" message with an optional button |
| `ui/theme/RoundedBorder` | Rounded outline (radius 8) for text fields, spinners, combo box |
| `ui/theme/Ui` | `row(...)` (FlowLayout button row) and `styleField(...)`; keeps `Theme` to tokens |
| `ui/screens/CurrencyForm` | The form half of the Currencies screen |
| `ui/screens/HistoryFilters`, `HistoryTable` | The filter card and the table of the History screen |
| `ui/screens/OfflineCard` | "Database offline + Retry", shared by History and Currencies |
| `ui/screens/DatabaseCard`, `FeedStatusCard` | One card each, split out of Settings and Feed |
| `feed/FeedStatus` | Enum from the networking spec (`DISCONNECTED, CONNECTING, CONNECTED, FAILED`) |
| `feed/FeedLog` | The "last 200 lines with time" log, shared by socket client and server |
| `server/ClientInfo` | Record (name, address) from the networking spec |

## 3. Where the build differs from the spec (on purpose)

| Spec says | Build does | Why |
|---|---|---|
| Graph radius `min(w, h)/2 − 60` | `min(w, h)/2 − 44` (node radius + 16) | With −60 the nodes overlapped at the minimum window size |
| Rate labels drawn with the arrows | Labels drawn after the nodes and moved 14 px off the line | Otherwise a node or the arrow hides the number in a small window |
| `FormRow` error text "hidden when empty" | The line is always there (help text in grey, error in red) | The form does not jump when an error appears |
| `CurrencyDao.findActive()`, `SettingsDao.save(key, value)` | Not built | Nothing calls them (`findAll` and `saveAll` are used); the rule is "no dead code" |
| `EXIT_ON_CLOSE` on the main window | See decision 14 | Needed for the confirm-on-close rule |
| Stock Nimbus fields | Text field, spinner and combo box get a flat white background and `RoundedBorder`; scroll bars get a slim grey thumb | Design spec asks for radius-8 fields; done with Nimbus "painter" keys in `Theme` |
| Classes under 150 lines | 10 of 72 classes have 152–194 lines (`SocketRateProvider`, `NetworkPanel`, `FeedScreen`, `CurrenciesScreen`, `HistoryScreen`, `OpportunityDao`, `ServerWindow`, `SettingsScreen`, `Theme`, `DashboardScreen`) | "Aim", not a limit; each still has one job. Splitting further would scatter one idea over many files |

## 4. Collections used, and where (rubric: appropriate use of Collections)

| Collection | Where | Why this one |
|---|---|---|
| `ArrayList<Edge>` | `GraphBuilder.build` | Ordered list that is only appended to and looped over |
| `ArrayList<Integer>` + `Collections.reverse` | `BellmanFordDetector.findCycle` | The cycle is collected backwards, then reversed |
| `List.of(...)` | `Currency.defaults()`, empty results | Small fixed lists that must never change |
| `List.copyOf(...)` | `RateSnapshot`, `Opportunity`, `OpportunityStore.all()`, `CurrencyService.all()` | Hand out a copy so callers cannot change shared data |
| `ArrayList<Currency>` | `CurrencyService.currencies` | The in-memory currency list, in display order |
| `HashMap<String, Integer>` | `CurrencyService.indexByCode` | Code → position, so update and delete find the row in one step |
| `HashSet<String>` | `CurrencyService.codes()` → `CurrencyValidator.checkNotDuplicate` | "Is this code taken?" is one fast lookup |
| `ArrayDeque<Long>` | `TickStats.durations` | Rolling window of the last 60 tick times: add at the end, remove at the front |
| `CopyOnWriteArrayList<Opportunity>` | `OpportunityStore.recent` | Scheduler thread writes, UI thread reads, no locks needed |
| `CopyOnWriteArrayList<String>` | `FeedLog.lines` | Network thread writes, UI thread reads |
| `CopyOnWriteArrayList<ClientHandler>` | `FeedServer.clients` | Broadcaster loops while clients join and leave |
| `HashMap<String, String>` | `SettingsDao.loadAll`, `SettingsScreen.save` | Key / value settings |
| `HashMap<String, FormRow>` | `CurrencyForm.rowsByField`, `SettingsScreen.rowsByField` | Validator field name → the row that shows its error |
| `LinkedHashMap<String, BaseScreen>` | `MainWindow.screens` | Screen name → screen, kept in sidebar order |
| `LinkedHashMap<String, NavItem>` | `Sidebar.items` | Screen name → its nav row |
| `AtomicReference<RateSnapshot>` | `RateStore`, `SocketRateProvider.latest` | One shared value written by one thread and read by another |
| `AtomicLong` / `AtomicInteger` | `TickStats`, `SocketRateProvider`, `FeedServer` | Thread-safe counters |
| `TableRowSorter` | `HistoryTable` | Sort the History table by clicking a column header |
| `DefaultListModel<String>` | `ServerWindow.clientModel` | The model behind the connected-clients `JList` |

## 5. Layout managers used, and where (rubric: GUI design and layout)

| Layout | Where |
|---|---|
| `BorderLayout` | `MainWindow` (sidebar WEST, rest CENTER), `Card`, `BaseScreen`, `FormRow`, `Sidebar`, most screen bodies |
| `CardLayout` | `MainWindow.content` (one screen at a time); `DashboardScreen` (table / empty state); `HistoryScreen` and `CurrenciesScreen` (data / offline) |
| `GridLayout` | `DashboardScreen.buildStatRow` (1×4 stat cards); `SettingsScreen` and `FeedScreen` (two cards side by side); `ServerWindow.buildMiddle` |
| `GridBagLayout` | `DashboardScreen.buildLowerRow` (60 / 40 split by weights); `CurrencyForm` (the form); `EmptyState` (centring) |
| `FlowLayout` | `Ui.row` — every button row (`CurrencyForm`, `SettingsScreen`, `FeedScreen`, `HistoryFilters`, `OpportunityDialog`, `ServerWindow`) and `Card` title actions |
| `BoxLayout` | `Sidebar.navList` (Y axis), `TopBar` (X axis) |

## 6. Event listeners used, and where (rubric: event handling)

| Listener | Where | What it does |
|---|---|---|
| `ActionListener` (lambdas) | All `AppButton`s; `NavItem`s in `Sidebar.addItem`; radio buttons in `FeedScreen` | Button and navigation clicks |
| `ListSelectionListener` | `CurrenciesScreen.buildTable` | Selecting a table row fills the form |
| `MouseAdapter` | `AppButton`, `NavItem` (hover on enter / exit); `HistoryScreen` (double-click opens details) | Hover effect and double-click |
| `ChangeListener` | `SettingsScreen.buildMarketCard` | Spinner changed → "Unsaved changes" |
| `WindowAdapter` | `MainWindow` (`windowClosing`) | Confirm on close while the socket feed is connected |
| Swing `Timer` | `MainWindow` (1 s refresh), `TopBar.toastTimer` (3 s, one shot), `ServerWindow` (1 s refresh) | Periodic UI refresh on the EDT; hide the toast |
| `SwingWorker` (through `BackgroundTask`) | Every database and network call started from the UI | Work off the EDT, result back on the EDT |

## 7. Threading rule, and how it was checked

- Background threads only **write** to thread-safe stores (`RateStore`, `OpportunityStore`, `TickStats`,
  `SocketRateProvider` counters). They never call Swing.
- The UI only **reads** those stores, on Swing Timers.
- Every DAO call and every socket call that starts in the UI is inside a `BackgroundTask.run(...)` lambda
  (checked by searching the `ui` package for `history()`, `settingsDao()`, `connectDatabase`, `connectSocket`,
  `useLocal`, `currencies().add/update/delete`, `start()`, `stop()`).
- `Theme.install()` runs on the `main` thread before the first component exists; the windows are created
  inside `SwingUtilities.invokeLater`.

## 8. How it was tested

- `mvn test`: 6 test classes, 31 tests (no MySQL needed).
- Live MySQL 8.4 (a temporary portable copy, not installed on the laptop): tables and seed created at start;
  opportunities saved with 3 legs each in one transaction; currency add / duplicate / update / deactivate /
  delete; foreign-key protection on a used currency; settings saved and loaded after restart; history
  filter, sort, details, delete; MySQL stopped while running → offline state → Retry after restart.
- Two JVMs for the socket demo: server window + desktop + a second client; same snapshots on both
  clients; server stop → dialog; wrong port → error in under 2 s; port in use → message in the server window.
- Screenshots in `docs/screenshots/` were taken from the real windows at 1280×800 and 1100×700.
