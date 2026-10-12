# Networking Specification (CAP-5)

A real forex desk receives prices from a **feed server**. We copy that idea in miniature:

- **Rate Feed Server** (`com.arbitrage.server`): a plain Java program with a Swing window. It runs a
  `SimulatedRateProvider` and **broadcasts** a snapshot to every connected client once a second.
- **Arbitrage Desktop** is a **client**: `SocketRateProvider` connects, reads snapshots in a background
  thread, and hands the latest one to the normal tick pipeline. Because it implements `RateProvider`,
  the engine and UI do not change at all. This is the main OOP payoff for the viva.

```
 Desktop A ─┐                                   ┌──────────── Rate Feed Server ────────────┐
 Desktop B ─┼──── TCP 5050, text lines ────────►│ ServerSocket.accept() ─► ClientHandler×N │
 Desktop C ─┘                                   │ broadcaster (1 s) ─► send to all clients │
                                                └──────────────────────────────────────────┘
```

## 1. Protocol (UTF-8 text, one message per line, fields separated by one space)

| Direction | Message | Example | Meaning |
|---|---|---|---|
| C → S | `HELLO <clientName>` | `HELLO desk-1` | first line after connecting |
| S → C | `WELCOME <serverName> <intervalMs>` | `WELCOME RateFeedServer 1000` | handshake OK |
| S → C | `SNAPSHOT <epochMillis> <codes> <rates>` | see below | one full rate matrix |
| C → S | `PING` | `PING` | optional liveness check |
| S → C | `PONG` | `PONG` | reply to PING |
| C → S / S → C | `BYE` | `BYE` | polite close |
| S → C | `ERROR <text>` | `ERROR expected HELLO` | protocol error, then close |

`SNAPSHOT` format:
- `<codes>` = comma-separated currency codes, e.g. `USD,EUR,GBP,INR,JPY`
- `<rates>` = n×n numbers, row-major, comma-separated, each with `%.8f` and `Locale.ROOT`

Example with 3 currencies (3 × 3 = 9 rates):

```
SNAPSHOT 1760261111000 USD,EUR,GBP 1.00000000,0.92500000,0.78661000,1.07892000,1.00000000,0.85634000,1.26873000,1.16543000,1.00000000
```

The codes field and the rates field never contain spaces, so `line.split(" ")` always gives exactly 4 parts.

## 2. FeedProtocol (shared, static, unit-tested)

```java
public final class FeedProtocol {
    public static final int DEFAULT_PORT = 5050;
    public static String hello(String name) { ... }
    public static String welcome(String server, int intervalMs) { ... }
    public static String encode(RateSnapshot s) { ... }            // "SNAPSHOT ..."
    public static RateSnapshot decode(String line) throws FeedException { ... }
}
```

`decode` rules: exactly 4 space-separated parts; parts[0] equals `SNAPSHOT`; the timestamp is parsed as a
long; there are 3–20 codes, each matching `^[A-Z]{3}$`; there are exactly n×n rates, each finite and > 0; the diagonal is
within 1e-9 of 1. Any violation → `FeedException("Bad snapshot: <reason>")`.

## 3. Client — `SocketRateProvider implements RateProvider`

```java
public void connect(String host, int port, String clientName) throws FeedException
public void disconnect()
public RateSnapshot fetchLatest()          // null if nothing new since the last call
public FeedStatus status()                 // DISCONNECTED, CONNECTING, CONNECTED, FAILED
public long messages(), badMessages(), lastMessageMillis()
public List<String> recentLog()            // last 200 lines, newest last (copy)
```

Steps in `connect`:
1. Validate host/port (`SettingsValidator`). Set status `CONNECTING`.
2. `socket = new Socket(); socket.connect(new InetSocketAddress(host, port), 3000);` and `socket.setSoTimeout(5000)`.
3. Create `BufferedReader(InputStreamReader(socket.getInputStream(), UTF_8))` and `PrintWriter(OutputStreamWriter(..., UTF_8), true)`.
4. Send `HELLO`; read one line; it must start with `WELCOME`, otherwise throw `FeedException`.
5. Start the **daemon** thread `feed-reader`: loop `readLine()`. For `SNAPSHOT`, `decode` → `latest.set(...)`, `messages++`.
   Bad lines → `badMessages++`, logged, ignored. On `BYE`, `null` (end of stream) or `IOException`, set status `DISCONNECTED`, close and exit the loop.
   `SocketTimeoutException` (no data for 5 s) → send `PING` once; on a second timeout, treat as disconnected.
6. Status `CONNECTED`. Every sent/received line is added to `recentLog` with `HH:mm:ss` and `→`/`←`.

`disconnect()` sends `BYE`, closes the socket (this unblocks the reader) and sets `DISCONNECTED`.
Fields shared across threads: `volatile` status, `AtomicReference<RateSnapshot> latest`, `AtomicLong` counters, a
`CopyOnWriteArrayList<String>` log trimmed to 200 lines.

`RateFeedManager.connectSocket(host, port)` = `socket.connect(...)`, then `mode = SOCKET`. `useLocal()` = `socket.disconnect()`, then `mode = LOCAL`.
When the UI sees `mode == SOCKET && status == DISCONNECTED`, it shows the switch-to-local dialog once.

Saving opportunities in SOCKET mode works the same way (`source = SOCKET`). If a leg uses a code that
is not in the local `currency` table, the FK insert fails. `OpportunityDao` reports
`DatabaseException("Unknown currency <code> – add it on the Currencies screen")`; the service logs it once and the opportunity stays in memory.

## 4. Server — `FeedServer`

```java
public FeedServer(int port, SimulatedRateProvider simulator)
public void start() throws IOException      // binds ServerSocket, starts threads
public void stop()                          // BYE to all, close everything
public boolean isRunning()
public List<ClientInfo> clients()           // name + address (copy)
public long snapshotsSent()
public List<String> recentLog()
```

- `start()`: `serverSocket = new ServerSocket(port)`; thread `feed-acceptor` loops `accept()` and creates a
  `ClientHandler` per socket, each run on its own thread `client-N`. A `ScheduledExecutorService`
  (`feed-broadcaster`) runs every 1000 ms: `line = FeedProtocol.encode(simulator.fetchLatest())`, then
  `handler.send(line)` for every client. Clients whose `PrintWriter.checkError()` is true are removed.
- `ClientHandler implements Runnable`: reads the first line; if it is not `HELLO`, it sends `ERROR` and closes.
  Otherwise it stores the name, sends `WELCOME`, adds itself to `clients` (`CopyOnWriteArrayList`) and
  then loops reading `PING` → `PONG`, `BYE` → close, and logs anything else. In `finally`, it removes itself and closes the socket.
  `send(String)` is `synchronized` (the broadcaster and the PONG reply can write at the same time).
- `stop()`: shut down the executor, send `BYE` to all, close the client sockets, then close the `ServerSocket`
  (this makes `accept()` throw, which ends the acceptor thread cleanly).
- `BindException` (port in use) → the window shows "Port 5050 is already in use".

The server **does not use Spring or MySQL**. It uses `Currency.defaults()` and `new AppSettings()` so it
can run anywhere. `FeedServerApp.main` calls `Theme.install()` and opens `ServerWindow` on the EDT.

## 5. Demo script (for the viva)

1. Start **Rate Feed Server** → Start → "Running · 0 clients".
2. Start **Arbitrage Desktop** → Feed → Socket feed → `localhost:5050` → Connect → the log shows HELLO/WELCOME, then snapshots.
3. The server window shows `desk-1`. Start a second desktop instance (or on a second laptop on the same
   Wi-Fi, use the server laptop's IP) → 2 clients receive the same snapshots.
4. Stop the server → the desktop shows Disconnected and offers to switch to Local.
5. Show `FeedProtocolTest` passing and explain the line format.

## 6. Tests

- `FeedProtocolTest`: round trip; a wrong keyword; a wrong number of rates; a negative rate; a bad code → `FeedException`.
- Optional `FeedServerIT` (not run by default): start the server on a random free port, connect a client,
  receive one snapshot within 2 s, then stop.
