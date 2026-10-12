package com.arbitrage.feed;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A short log of network messages with the time in front of each line.
 * It keeps only the newest 200 lines. Used by the socket client and by the feed server.
 */
public class FeedLog {

    private static final int MAX_LINES = 200;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    // Network threads add lines while the UI thread reads them, so the list must be thread-safe.
    private final CopyOnWriteArrayList<String> lines = new CopyOnWriteArrayList<>();

    public void add(String text) {
        lines.add(LocalTime.now().format(TIME) + "  " + text);
        while (lines.size() > MAX_LINES) {
            lines.remove(0);
        }
    }

    /** A copy of the lines, oldest first. */
    public List<String> lines() {
        return List.copyOf(lines);
    }
}
