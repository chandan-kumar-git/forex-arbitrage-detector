package com.arbitrage.server;

import com.arbitrage.feed.FeedProtocol;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Talks to one connected client on its own thread.
 * readLine() blocks, so every client needs a thread; otherwise one slow client would stop the others.
 */
public class ClientHandler implements Runnable {

    private static final int INTERVAL_MS = 1000;

    private final Socket socket;
    private final FeedServer server;
    private volatile PrintWriter out;
    private volatile String name = "unknown";

    public ClientHandler(Socket socket, FeedServer server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

            String first = in.readLine();         // the first line must be "HELLO <name>"
            if (first == null || !first.startsWith(FeedProtocol.HELLO + " ")) {
                send(FeedProtocol.ERROR + " expected HELLO");
                return;
            }
            name = first.substring(FeedProtocol.HELLO.length()).trim();
            send(FeedProtocol.welcome(FeedProtocol.SERVER_NAME, INTERVAL_MS));
            server.register(this);

            String line = in.readLine();
            while (line != null && !line.equals(FeedProtocol.BYE)) {
                if (line.equals(FeedProtocol.PING)) {
                    send(FeedProtocol.PONG);
                } else {
                    server.log("unknown message from " + name + ": " + line);
                }
                line = in.readLine();
            }
        } catch (IOException e) {
            server.log("client " + name + " lost: " + e.getMessage());
        } finally {
            server.unregister(this);
            close();
        }
    }

    /** synchronized: the broadcaster thread and the PONG reply may write at the same time. */
    public synchronized void send(String line) {
        if (out != null) {
            out.println(line);
        }
    }

    /** True when a write failed, which means the client is gone. */
    public boolean hasError() {
        return out != null && out.checkError();
    }

    public void close() {
        try {
            socket.close();
        } catch (IOException e) {
            server.log("close problem: " + e.getMessage());
        }
    }

    public ClientInfo info() {
        return new ClientInfo(name, String.valueOf(socket.getRemoteSocketAddress()));
    }
}
