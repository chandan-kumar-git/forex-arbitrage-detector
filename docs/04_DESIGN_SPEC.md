# Design Specification (Swing)

Goal: a calm, premium "trading desk" look. It should be light, spacious and precise, with one strong
colour (deep teal) and a red that means only one thing: arbitrage. Build everything with JDK classes only:
the **Nimbus** look-and-feel tuned by `Theme`, plus a few small custom-painted components.

## 1. Design tokens (`ui/theme/Theme.java`)

### Colours

| Token | Hex | Use |
|---|---|---|
| `APP_BG` | `#F4F6F8` | window background behind cards |
| `SURFACE` | `#FFFFFF` | cards, tables, dialogs |
| `BORDER` | `#E3E8EB` | card borders, dividers |
| `INK` | `#1E2933` | main text |
| `INK_2` | `#56626B` | secondary text, captions |
| `MUTE` | `#86929A` | hints, axis-like text |
| `PRIMARY` | `#1D5B76` | primary buttons, selection, node borders |
| `PRIMARY_HOVER` | `#174B61` | hover state |
| `PRIMARY_TINT` | `#E6F0F4` | selected rows, info chips |
| `SIDEBAR_BG` | `#143F52` | sidebar |
| `SIDEBAR_TEXT` | `#CFE2EA` | sidebar items |
| `SIDEBAR_ACTIVE` | `#1D5B76` | active nav background |
| `ACCENT` | `#E2B04A` | 3 px active-nav bar, small highlights only |
| `GOOD` / `GOOD_TINT` | `#1E7A55` / `#E3F2EA` | Live, Connected, positive profit |
| `WARN` / `WARN_TINT` | `#A86F00` / `#FBF1DC` | Paused, Connecting |
| `BAD` / `BAD_TINT` | `#B23A2E` / `#FBE8E5` | errors, DB offline, Disconnected |
| `CYCLE` | `#C0392B` | **only** the profitable-cycle edges and nodes |
| `EDGE` | `#D3DADF` | graph edges |
| `NODE_FILL` | `#F0F6F8` | graph nodes |

Rule: red (`CYCLE`) is reserved for arbitrage. Errors use `BAD`, which is a slightly different red, and always come with text.

### Typography

`Theme.font(size, bold)` picks the first installed family from `["Inter", "Segoe UI", "SF Pro Text",
"Helvetica Neue", "Arial", Font.SANS_SERIF]`. For monospace use `["JetBrains Mono", "Consolas", "Menlo", Font.MONOSPACED]`.

| Role | Size | Weight | Example |
|---|---|---|---|
| `TITLE` | 22 | bold | screen title "Dashboard" |
| `SUBTITLE` | 13 | plain, `INK_2` | line under the title |
| `CARD_TITLE` | 15 | bold | "Live network" |
| `BODY` | 13 | plain | labels, table cells |
| `SMALL` | 12 | plain | captions, chips |
| `STAT` | 28 | bold | stat card numbers |
| `MONO` | 12 | plain | protocol log, rates, codes |

Text antialiasing: `System.setProperty("awt.useSystemAAFontSettings", "on")` and `swing.aatext=true`
before any UI is created. In custom painting, enable `KEY_ANTIALIASING` and `KEY_TEXT_ANTIALIASING`.

### Spacing, radius, size

- Spacing scale: 4, 8, 12, 16, 24, 32 (`Theme.S1..S6`). Card padding 20. Gap between cards 16. Screen padding 24.
- Radius: cards 12, buttons and fields 8, chips fully rounded.
- Sidebar width 220. Top bar height 56. Table row height 34. Button height 36. Field height 34.

### Nimbus tuning (`Theme.install()`)

Set the Nimbus look-and-feel, then set these keys **before** creating any component:
`nimbusBase=PRIMARY`, `nimbusBlueGrey=#C9D3D9`, `control=APP_BG`, `nimbusFocus=#5E93AA`,
`nimbusSelectionBackground=PRIMARY`, `nimbusSelectedText=white`, `text=INK`, `defaultFont=BODY`,
`Table.alternateRowColor=#FAFCFC`, `Table.showGrid=false`, `ScrollPane.border=empty`.
If Nimbus is missing, fall back to the cross-platform look-and-feel; the app must still work.

## 2. Components (each small and custom-painted)

| Class | What it is | Notes |
|---|---|---|
| `Card` | `JPanel` with a white rounded rect, a 1 px `BORDER` stroke and an optional title row (title left, action area right) | `paintComponent` draws `RoundRectangle2D`; `setOpaque(false)` |
| `AppButton` | `JButton` with variants `PRIMARY` (teal fill, white text), `SECONDARY` (white fill, border, ink text), `DANGER` (white fill, red text and border) | Paints its own rounded background; hover via a `MouseAdapter` flag; disabled = 45% alpha; hand cursor |
| `NavItem` | Sidebar row: 16 px glyph character + label | Active: `SIDEBAR_ACTIVE` background + 3 px `ACCENT` bar on the left |
| `StatusChip` | Rounded pill with a coloured dot + text; kinds `GOOD`, `WARN`, `BAD`, `INFO` | `setStatus(kind, text)` |
| `StatCard` | `Card` with a caption (`SMALL`, `INK_2`), a big value (`STAT`) and a hint line | `setValue(String)`, `setHint(String)` |
| `FormRow` | Label above the field, red `SMALL` error text below (hidden when empty) | `showError(String)`, `clearError()` |
| `Tables.style(JTable)` | Row height 34, header bold `SMALL` `INK_2` on `SURFACE` with a bottom border, no vertical grid, selection `PRIMARY_TINT` + `INK` text | Also `Tables.profitRenderer()` (green bold, right-aligned, "%.3f%%") and `Tables.monoRenderer()` |
| `BackgroundTask` | `run(Callable<T> work, Consumer<T> onSuccess, Consumer<Exception> onError)` using `SwingWorker` | The success and error callbacks run on the EDT |

Glyphs for nav items: use simple Unicode that every font has (`◉` Dashboard, `☰` History,
`¤` Currencies, `⇄` Feed, `⚙` Settings). No image files are needed.

## 3. Main window

```
┌──────────┬───────────────────────────────────────────────────────────────────────────┐
│ SIDEBAR  │ TOP BAR: [● Live] [⇄ Local simulator] [● DB connected]   toast…  [Pause]  │
│ 220px    ├───────────────────────────────────────────────────────────────────────────┤
│ Forex    │                                                                           │
│ Arbitrage│   CONTENT (CardLayout)  – screen padding 24                               │
│          │                                                                           │
│ ◉ Dashb. │                                                                           │
│ ☰ History│                                                                           │
│ ¤ Curren.│                                                                           │
│ ⇄ Feed   │                                                                           │
│ ⚙ Setting│                                                                           │
│          │                                                                           │
│ v1.0 ·   │                                                                           │
│ CAP-5    │                                                                           │
└──────────┴───────────────────────────────────────────────────────────────────────────┘
```

- `MainWindow extends JFrame`: `BorderLayout`; Sidebar `WEST`; a panel with TopBar `NORTH` + content `CENTER`.
  Title "Forex Arbitrage Detector". Size 1280×800, minimum 1100×700, centred, `EXIT_ON_CLOSE`.
- Content uses `CardLayout`. On navigation, call `screen.onShow()` (polymorphism through `BaseScreen`).
- The sidebar header has a 2-line wordmark, "Forex" (`SIDEBAR_TEXT`, 13) above "Arbitrage" (white, 18 bold), and a footer caption with the version.
- Top bar: white, bottom border. Left: 3 `StatusChip`s (feed state, source, DB). Middle: toast label
  (hidden by default; `GOOD_TINT` pill with "New arbitrage: … +0.347%", shown for 3 s via a one-shot Swing Timer).
  Right: Pause/Resume `AppButton` (`SECONDARY`).
- One `javax.swing.Timer(1000)` in `MainWindow` refreshes the top bar and calls `refresh()` on the visible screen only.

## 4. Screens

Every screen extends `BaseScreen`: a title + subtitle at the top, content below, `APP_BG` background, padding 24.

### 4.1 Dashboard

```
 Dashboard
 Live view of the currency network and detected arbitrage
 ┌ Ticks ────────┐ ┌ Opportunities ┐ ┌ Best profit ──┐ ┌ Avg tick time ┐
 │ 1,284         │ │ 37            │ │ 0.742%        │ │ 3.4 µs        │
 │ since start   │ │ this session  │ │ INR→EUR→GBP   │ │ last 60 ticks │
 └───────────────┘ └───────────────┘ └───────────────┘ └───────────────┘
 ┌ Live network ─────────────────────────┐ ┌ Recent opportunities ──────────┐
 │                                       │ │ Path              Profit  Time │
 │           (NetworkPanel)              │ │ INR→EUR→GBP→INR   0.347% 14:05 │
 │                                       │ │ …                              │
 │  ─ pair   ━ profitable cycle          │ │                                │
 └───────────────────────────────────────┘ └────────────────────────────────┘
```

- Stat row: `GridLayout(1, 4, 16, 0)`. Body: a 60/40 split (`GridBagLayout` with weights 0.6/0.4).
- **NetworkPanel** (custom painting, in this order): background → all edges `EDGE` 1.2 px →
  cycle edges `CYCLE` 3 px with filled arrowheads and a small rate label (`MONO` 11, white rounded
  background) at the midpoint → nodes (radius 28, `NODE_FILL`, 2 px `PRIMARY` border; cycle nodes get a
  3 px `CYCLE` border and a soft 6 px outer ring at 25% alpha) → code text centred (`BODY` bold) → legend.
  Node positions are on a circle starting at the top (angle −90°), with radius `min(w, h)/2 − 60`. It redraws on resize.
- Empty state (fewer than 3 currencies, or no snapshot yet): centred `INK_2` text plus a smaller hint.
- Recent table: in-memory `OpportunityStore`, last 10 rows, columns Path / Profit / Time, profit renderer.
  Empty state: "No arbitrage yet – it usually appears within a few seconds."

### 4.2 History (CAP-5)

```
 History
 Every opportunity saved in MySQL
 ┌ Filters ──────────────────────────────────────────────────────────────────────────┐
 │ Period [Today ▾]   Min profit % [0.10 ⇅]   Currency [INR    ]   [Apply] [Reset]   │
 └───────────────────────────────────────────────────────────────────────────────────┘
 ┌ 128 opportunities · avg 0.41% · best 0.74%           [Delete selected] [Clear all] ┐
 │ #   Detected at          Path                    Len  Profit %   Fee %   Source    │
 │ 412 2026-10-12 14:05:11  INR → EUR → GBP → INR   3    0.347      0.050   LOCAL     │
 │ …   (double-click a row for details)                                               │
 └───────────────────────────────────────────────────────────────────────────────────┘
```

- `JTable` with `TableRowSorter` (sort by clicking headers). Load with `BackgroundTask`; while
  loading, show "Loading…" in the summary line and disable the buttons.
- `OpportunityDialog` (modal, 520×360): header with path + profit, a legs table (Leg, From, To, Rate), and a Close button.
- When the DB is offline, replace the table with a Card showing "Database offline" + the error text + a **Retry** button.

### 4.3 Currencies

```
 Currencies
 Add, edit or deactivate the currencies used by the simulator
 ┌ Currency ────────────────┐ ┌ All currencies ──────────────────────────────────┐
 │ Code        [INR      ]  │ │ Code  Name            USD price      Active      │
 │ Name        [Indian R…]  │ │ USD   US Dollar       1.00000000     ✓           │
 │ USD price   [0.012    ]  │ │ EUR   Euro            1.08000000     ✓           │
 │ [✓] Active               │ │ …                                                │
 │ [Add] [Update]           │ │                                                  │
 │ [Delete]       [Clear]   │ │                                                  │
 └──────────────────────────┘ └──────────────────────────────────────────────────┘
```

- Left card 340 px wide; right card fills the rest.
- Selecting a row fills the form, makes the code field read-only and enables Update/Delete. Clear resets everything.
- Validation errors show under the field (`FormRow`). Success shows a toast ("EUR updated").
- Inactive rows are drawn in `MUTE` colour.

### 4.4 Feed

```
 Feed
 Where rates come from
 ┌ Source ──────────────────────────────┐ ┌ Status ───────────────────────────────┐
 │ (•) Local simulator                  │ │ [● Connected]  localhost:5050         │
 │ ( ) Socket feed                      │ │ Messages 1,204 · Last 0.4 s ago       │
 │     Host [localhost]  Port [5050 ⇅]  │ │ Bad messages 0                        │
 │     [Connect]  [Disconnect]          │ │                                       │
 └──────────────────────────────────────┘ └───────────────────────────────────────┘
 ┌ Protocol log ─────────────────────────────────────────────────────────────────────┐
 │ 14:05:01  → HELLO desk-1                                                          │
 │ 14:05:01  ← WELCOME RateFeedServer 1000                                           │
 │ 14:05:02  ← SNAPSHOT 1739… (5 currencies)                                         │
 └───────────────────────────────────────────────────────────────────────────────────┘
```

- The log is a `JTextArea` (`MONO`, non-editable) in a scroll pane. It keeps the last 200 lines and auto-scrolls.
- Connect runs in `BackgroundTask`; the button shows "Connecting…" and is disabled until done.
- On disconnect detected by the reader thread, the next UI refresh shows the chip in red, and a dialog asks "Switch to Local simulator?".

### 4.5 Settings

Two cards side by side. **Market**: Fee %, Spread %, Inject chance % (each a `JSpinner` with a
`FormRow` and a one-line help text), plus Save (PRIMARY) and Reset defaults (SECONDARY).
**Database**: the JDBC URL (`MONO`, read-only), the user, the status chip, and a **Test connection** button with a result line.

## 5. Rate Feed Server window (`ServerWindow`)

```
┌ Rate Feed Server ─────────────────────────────────────────────┐
│ Port [5050]  [Start server]           [● Running · 2 clients] │
├───────────────────────────────┬───────────────────────────────┤
│ Connected clients             │ Log                           │
│ desk-1   /127.0.0.1:53122     │ 14:05:01 client desk-1 joined │
│ desk-2   /192.168.1.7:50311   │ 14:05:02 broadcast #1 to 2    │
├───────────────────────────────┴───────────────────────────────┤
│ Snapshots sent 1,203 · Interval 1000 ms · Inject chance 30 %  │
└───────────────────────────────────────────────────────────────┘
```

Same `Theme`, size 760×480. The Start button toggles to "Stop server" (`DANGER`). A Swing Timer
refreshes the client list and counters every second.

## 6. Quality bar (check visually before finishing a milestone)

- Nothing clips at 1100×700. Long paths ellipsize in tables, never wrap.
- One primary button per card at most. Destructive actions are `DANGER` and always confirm.
- Every empty, loading and error state has text that tells the user what to do next.
- Numbers: profit `%.3f%%`, prices `%.8f`, counts with a thousands separator, times `HH:mm:ss`.
- Hover and focus are visible on buttons, nav items and fields. Tab order follows the visual order.
- Take a screenshot of every screen at the end of CAP-3 and CAP-5 and save them in `docs/screenshots/`.
