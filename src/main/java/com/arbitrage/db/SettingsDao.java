package com.arbitrage.db;

import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Reads and writes the app_setting table, a simple key / value table
 * that keeps fee, spread and inject chance between runs.
 */
@Component
public class SettingsDao {

    public static final String KEY_FEE = "fee";
    public static final String KEY_SPREAD = "spread";
    public static final String KEY_INJECT_CHANCE = "inject_chance";

    /** Insert the key, or change its value when the key is already there. */
    private static final String UPSERT = "INSERT INTO app_setting (setting_key, setting_value) VALUES (?, ?) "
            + "ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value)";

    private final DatabaseConnection database;

    public SettingsDao(DatabaseConnection database) {
        this.database = database;
    }

    public Map<String, String> loadAll() {
        String sql = "SELECT setting_key, setting_value FROM app_setting";
        Map<String, String> values = new HashMap<>();
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                values.put(rs.getString("setting_key"), rs.getString("setting_value"));
            }
        } catch (SQLException e) {
            throw database.failure("Could not load the settings", e);
        }
        return values;
    }

    /** Saves all values in one transaction, so the settings never end up half saved. */
    public void saveAll(Map<String, String> values) {
        try (Connection connection = database.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(UPSERT)) {
                for (Map.Entry<String, String> entry : values.entrySet()) {
                    statement.setString(1, entry.getKey());
                    statement.setString(2, entry.getValue());
                    statement.addBatch();
                }
                statement.executeBatch();
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw database.failure("Could not save the settings", e);
        }
    }
}
