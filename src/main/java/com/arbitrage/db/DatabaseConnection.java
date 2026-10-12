package com.arbitrage.db;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Opens JDBC connections to MySQL with DriverManager and remembers if the database is reachable.
 * There is no connection pool on purpose: each DAO method opens one connection and closes it.
 */
@Component
public class DatabaseConnection {

    private final String url;
    private final String username;
    private final String password;

    // volatile: several threads (scheduler, SwingWorker, EDT) read and write these flags
    private volatile boolean online = false;
    private volatile String lastError = "The database has not been contacted yet.";

    public DatabaseConnection(@Value("${db.url}") String url,
                              @Value("${db.username}") String username,
                              @Value("${db.password}") String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    /** Opens and closes one connection; returns the time taken in ms. Throws DatabaseException. */
    public long test() {
        long start = System.currentTimeMillis();
        try (Connection connection = getConnection()) {
            online = true;
            lastError = "";
            return System.currentTimeMillis() - start;
        } catch (SQLException e) {
            DatabaseException failure = failure("Cannot connect to MySQL", e);
            markOffline(failure.getMessage());
            throw failure;
        }
    }

    /**
     * Turns an SQLException into a DatabaseException with a message in plain words.
     * SQLState 08xxx means "connection problem", so the database is marked offline.
     */
    public DatabaseException failure(String action, SQLException e) {
        String state = e.getSQLState() == null ? "" : e.getSQLState();
        String reason;
        if (state.startsWith("08")) {
            reason = "MySQL is not reachable. Is the MySQL server running?";
            markOffline(action + ": " + reason);
        } else if (state.startsWith("28")) {
            reason = "MySQL refused the username or password. Check db.password in application.properties.";
        } else {
            reason = e.getMessage().lines().findFirst().orElse("unknown database error");
        }
        return new DatabaseException(action + ": " + reason, e);
    }

    public void markOffline(String error) {
        online = false;
        lastError = error;
    }

    public boolean isOnline() {
        return online;
    }

    public String lastError() {
        return lastError;
    }

    public String url() {
        return url;
    }

    public String username() {
        return username;
    }
}
