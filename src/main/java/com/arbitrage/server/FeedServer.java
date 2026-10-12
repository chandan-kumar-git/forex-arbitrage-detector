package com.arbitrage.server;

import com.arbitrage.feed.FeedLog;
import com.arbitrage.feed.FeedProtocol;
import com.arbitrage.feed.SimulatedRateProvider;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The Rate Feed Server: accepts clients on a ServerSocket and sends every connected
 * client the same rate snapshot once a second. It uses no Spring and no database.
 */
public class FeedServer {

    private static final int INTERVAL_MS = 1000;

    private final int port;
    private final SimulatedRateProvider simulator;
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private final AtomicLong snapshotsSent = new AtomicLong();
    private final AtomicInteger clientNumber = new AtomicInteger();
    private final FeedLog log = new FeedLog();
    private volatile boolean running = false;
    private ServerSocket serverSocket;
    private ScheduledExecutorService broadcaster;

    public FeedServer(int port, SimulatedRateProvider simulator) {
        this.port = port;
        this.simulator = simulator;
    }

    /** Binds the port and starts the threads. Throws BindException if the port is in use. */
    public synchronized void start() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;
        Thread acceptor = new Thread(this::acceptLoop, "feed-acceptor");
        acceptor.start();
        broadcaster = Executors.newSingleThreadScheduledExecutor(task -> new Thread(task, "feed-broadcaster"));
        broadcaster.scheduleAtFixedRate(this::broadcast, INTERVAL_MS, INTERVAL_MS, TimeUnit.MILLISECONDS);
        log.add("server started on port " + port);
    }

    /** Says BYE to everyone and closes everything. */
    public synchronized void stop() {
        if (!running) {
            return;
        }
        running = false;
        broadcaster.shutdownNow();
        for (ClientHandler client : clients) {
            client.send(FeedProtocol.BYE);
            client.close();
        }
        try {
            serverSocket.close();                 // this makes accept() throw, which ends the acceptor thread
        } catch (IOException e) {
            log.add("close problem: " + e.getMessage());
        }
        log.add("server stopped");
    }

    /** Runs on the feed-acceptor thread: waits for clients and gives each one its own thread. */
    private void acceptLoop() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(socket, this);
                new Thread(handler, "client-" + clientNumber.incrementAndGet()).start();
            } catch (IOException e) {
                if (running) {
                    log.add("accept failed: " + e.getMessage());
                }
            }
        }
    }

    /** Runs on the feed-broadcaster thread every second. */
    private void broadcast() {
        try {
            String line = FeedProtocol.encode(simulator.fetchLatest());
            for (ClientHandler client : clients) {
                client.send(line);
                if (client.hasError()) {          // the write failed: this client is gone
                    client.close();
                }
            }
            if (!clients.isEmpty()) {
                log.add("broadcast #" + snapshotsSent.incrementAndGet() + " to " + clients.size());
            }
        } catch (RuntimeException e) {
            // Without this catch, one failure would silently stop all future broadcasts.
            log.add("broadcast failed: " + e.getMessage());
        }
    }

    void register(ClientHandler client) {
        clients.add(client);
        log.add("client " + client.info().name() + " joined");
    }

    void unregister(ClientHandler client) {
        if (clients.remove(client)) {
            log.add("client " + client.info().name() + " left");
        }
    }

    void log(String text) {
        log.add(text);
    }

    public boolean isRunning() {
        return running;
    }

    /** Name and address of every connected client (a copy). */
    public List<ClientInfo> clients() {
        List<ClientInfo> infos = new ArrayList<>();
        for (ClientHandler client : clients) {
            infos.add(client.info());
        }
        return infos;
    }

    public long snapshotsSent() {
        return snapshotsSent.get();
    }

    public List<String> recentLog() {
        return log.lines();
    }
}
