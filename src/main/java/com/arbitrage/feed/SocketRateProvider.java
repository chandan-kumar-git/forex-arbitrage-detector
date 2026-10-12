package com.arbitrage.feed;

import com.arbitrage.model.FeedSource;
import com.arbitrage.model.RateSnapshot;
import com.arbitrage.validation.SettingsValidator;
import com.arbitrage.validation.ValidationException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The socket client: connects to the Rate Feed Server and keeps the newest snapshot.
 * A background thread ("feed-reader") reads the lines, so no other thread ever waits for the network.
 */
public class SocketRateProvider implements RateProvider {

    private static final int CONNECT_TIMEOUT_MS = 3000;
    private static final int READ_TIMEOUT_MS = 5000;

    // Shared between the reader thread, the scheduler thread and the UI thread.
    private final AtomicReference<RateSnapshot> latest = new AtomicReference<>();
    private final AtomicLong messages = new AtomicLong();
    private final AtomicLong badMessages = new AtomicLong();
    private final AtomicLong lastMessageMillis = new AtomicLong();
    private final FeedLog log = new FeedLog();
    private volatile FeedStatus status = FeedStatus.DISCONNECTED;
    private volatile Socket socket;
    private volatile PrintWriter out;
    private volatile String address = "";

    public synchronized void connect(String host, int port, String clientName) throws FeedException {
        try {
            SettingsValidator.validateHost(host);
            SettingsValidator.validatePort(port);
        } catch (ValidationException e) {
            throw new FeedException(e.getMessage(), e);
        }
        disconnect();                             // close an older connection first
        status = FeedStatus.CONNECTING;
        address = host.trim() + ":" + port;
        Socket newSocket = new Socket();
        try {
            newSocket.connect(new InetSocketAddress(host.trim(), port), CONNECT_TIMEOUT_MS);
            newSocket.setSoTimeout(READ_TIMEOUT_MS);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(newSocket.getInputStream(), StandardCharsets.UTF_8));
            out = new PrintWriter(new OutputStreamWriter(newSocket.getOutputStream(), StandardCharsets.UTF_8), true);
            send(FeedProtocol.hello(clientName));
            String reply = in.readLine();
            log.add("← " + reply);
            if (reply == null || !reply.startsWith(FeedProtocol.WELCOME)) {
                throw new IOException("the server did not answer with WELCOME");
            }
            socket = newSocket;
            status = FeedStatus.CONNECTED;
            Thread reader = new Thread(() -> readLoop(newSocket, in), "feed-reader");
            reader.setDaemon(true);               // a daemon thread never keeps the program alive
            reader.start();
        } catch (IOException e) {
            status = FeedStatus.FAILED;
            close(newSocket);
            log.add("connect failed: " + e.getMessage());
            throw new FeedException("Cannot connect to " + address
                    + ". Check that the Rate Feed Server is started and that the host and port are right.", e);
        }
    }

    /** Says BYE and closes the socket. Closing also wakes up the reader thread. */
    public synchronized void disconnect() {
        Socket current = socket;
        if (current != null) {
            send(FeedProtocol.BYE);
            socket = null;
            close(current);
        }
        status = FeedStatus.DISCONNECTED;
    }

    /** The newest snapshot, or null if nothing new arrived since the last call. */
    @Override
    public RateSnapshot fetchLatest() {
        return latest.getAndSet(null);
    }

    @Override
    public FeedSource source() {
        return FeedSource.SOCKET;
    }

    /** Runs on the feed-reader thread until the connection ends. */
    private void readLoop(Socket mySocket, BufferedReader in) {
        boolean pingSent = false;
        try {
            while (true) {
                String line;
                try {
                    line = in.readLine();         // waits here for the next line
                } catch (SocketTimeoutException e) {
                    if (pingSent) {
                        break;                    // second silence in a row: give up
                    }
                    send(FeedProtocol.PING);      // 5 s of silence: ask if the server is alive
                    pingSent = true;
                    continue;
                }
                if (line == null || line.equals(FeedProtocol.BYE)) {
                    break;                        // null = the server closed the connection
                }
                pingSent = false;
                handleLine(line);
            }
        } catch (IOException e) {
            log.add("connection lost: " + e.getMessage());
        }
        close(mySocket);
        if (socket == mySocket) {                 // still the current connection, not an old one
            socket = null;
            status = FeedStatus.DISCONNECTED;
            log.add("disconnected");
        }
    }

    private void handleLine(String line) {
        if (!line.startsWith(FeedProtocol.SNAPSHOT)) {
            log.add("← " + line);                 // PONG, ERROR or something unknown
            if (!line.equals(FeedProtocol.PONG)) {
                badMessages.incrementAndGet();
            }
            return;
        }
        try {
            RateSnapshot snapshot = FeedProtocol.decode(line);
            latest.set(snapshot);
            messages.incrementAndGet();
            lastMessageMillis.set(System.currentTimeMillis());
            log.add("← SNAPSHOT " + snapshot.timestamp() + " (" + snapshot.size() + " currencies)");
        } catch (FeedException e) {
            badMessages.incrementAndGet();
            log.add("✗ " + e.getMessage());
        }
    }

    private void send(String line) {
        PrintWriter writer = out;
        if (writer != null) {
            writer.println(line);
            log.add("→ " + line);
        }
    }

    private void close(Socket toClose) {
        try {
            toClose.close();
        } catch (IOException e) {
            log.add("close problem: " + e.getMessage());
        }
    }

    public FeedStatus status() {
        return status;
    }

    public String address() {
        return address;
    }

    public long messages() {
        return messages.get();
    }

    public long badMessages() {
        return badMessages.get();
    }

    public long lastMessageMillis() {
        return lastMessageMillis.get();
    }

    /** The last 200 log lines, newest last. */
    public List<String> recentLog() {
        return log.lines();
    }
}
