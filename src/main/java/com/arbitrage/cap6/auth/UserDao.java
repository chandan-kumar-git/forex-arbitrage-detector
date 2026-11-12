package com.arbitrage.cap6.auth;

import com.arbitrage.db.DatabaseConnection;
import com.arbitrage.db.DatabaseException;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * JDBC access to the app_user table, written the same way as the CAP-5 DAOs:
 * DriverManager connection, PreparedStatement with ? placeholders, try-with-resources.
 */
@Component
public class UserDao {

    /** A user found by name, with the stored hash for the password check. */
    public record StoredUser(User user, String passwordHash) {
    }

    public static final String DEMO_USERNAME = "demo";
    public static final String DEMO_PASSWORD = "demo1234";

    private final DatabaseConnection database;

    public UserDao(DatabaseConnection database) {
        this.database = database;
    }

    public Optional<StoredUser> findByUsername(String username) {
        String sql = "SELECT id, username, password_hash, display_name, last_login_at FROM app_user WHERE username = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                Timestamp last = rs.getTimestamp("last_login_at");
                User user = new User(rs.getLong("id"), rs.getString("username"), rs.getString("display_name"),
                        last == null ? null : last.toLocalDateTime());
                return Optional.of(new StoredUser(user, rs.getString("password_hash")));
            }
        } catch (SQLException e) {
            throw database.failure("Could not look up the user", e);
        }
    }

    /** Inserts a new user. Throws DatabaseException "Username already taken" on a duplicate. */
    public void insert(String username, String displayName, String passwordHash) {
        String sql = "INSERT INTO app_user (username, password_hash, display_name) VALUES (?, ?, ?)";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            statement.setString(2, passwordHash);
            statement.setString(3, displayName);
            statement.executeUpdate();
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new DatabaseException("Username already taken", e);   // the UNIQUE key said no
        } catch (SQLException e) {
            throw database.failure("Could not create the user", e);
        }
    }

    public void updateLastLogin(long id, LocalDateTime when) {
        String sql = "UPDATE app_user SET last_login_at = ? WHERE id = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, Timestamp.valueOf(when));
            statement.setLong(2, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw database.failure("Could not save the login time", e);
        }
    }

    public long count() {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM app_user");
             ResultSet rs = statement.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        } catch (SQLException e) {
            throw database.failure("Could not count the users", e);
        }
    }

    /** Creates the demo account (demo / demo1234) when there are no users yet, for the viva. */
    public void seedDemoUserIfEmpty() {
        if (count() == 0) {
            insert(DEMO_USERNAME, "Demo User", PasswordHasher.hash(DEMO_PASSWORD));
        }
    }
}
