package com.arbitrage.db;

import com.arbitrage.config.AppSettings;
import com.arbitrage.model.Currency;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Creates the tables if they do not exist yet and inserts the default rows
 * when a table is empty. The CREATE TABLE statements are the same as in sql/schema.sql.
 */
@Component
public class DatabaseInitializer {

    private static final String[] CREATE_TABLES = {
        """
        CREATE TABLE IF NOT EXISTS currency (
            code        CHAR(3)        NOT NULL,
            name        VARCHAR(40)    NOT NULL,
            usd_price   DECIMAL(18, 8) NOT NULL,
            active      BOOLEAN        NOT NULL DEFAULT TRUE,
            created_at  TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
            updated_at  TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
            PRIMARY KEY (code),
            CONSTRAINT chk_currency_price CHECK (usd_price > 0)
        )""",
        """
        CREATE TABLE IF NOT EXISTS opportunity (
            id              BIGINT         NOT NULL AUTO_INCREMENT,
            path_text       VARCHAR(120)   NOT NULL,
            path_length     TINYINT        NOT NULL,
            profit_percent  DECIMAL(10, 6) NOT NULL,
            fee             DECIMAL(8, 6)  NOT NULL,
            source          VARCHAR(10)    NOT NULL,
            detected_at     DATETIME(3)    NOT NULL,
            PRIMARY KEY (id),
            INDEX idx_opportunity_detected (detected_at),
            CONSTRAINT chk_opportunity_source CHECK (source IN ('LOCAL', 'SOCKET'))
        )""",
        """
        CREATE TABLE IF NOT EXISTS opportunity_leg (
            opportunity_id  BIGINT         NOT NULL,
            leg_no          TINYINT        NOT NULL,
            from_code       CHAR(3)        NOT NULL,
            to_code         CHAR(3)        NOT NULL,
            rate            DECIMAL(18, 8) NOT NULL,
            PRIMARY KEY (opportunity_id, leg_no),
            CONSTRAINT fk_leg_opportunity FOREIGN KEY (opportunity_id)
                REFERENCES opportunity (id) ON DELETE CASCADE,
            CONSTRAINT fk_leg_from FOREIGN KEY (from_code)
                REFERENCES currency (code) ON DELETE RESTRICT,
            CONSTRAINT fk_leg_to FOREIGN KEY (to_code)
                REFERENCES currency (code) ON DELETE RESTRICT
        )""",
        """
        CREATE TABLE IF NOT EXISTS app_setting (
            setting_key    VARCHAR(40)  NOT NULL,
            setting_value  VARCHAR(100) NOT NULL,
            updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
            PRIMARY KEY (setting_key)
        )""",
        // CAP-6: users of the Servlet/JSP portal (login and register)
        """
        CREATE TABLE IF NOT EXISTS app_user (
            id             BIGINT       NOT NULL AUTO_INCREMENT,
            username       VARCHAR(40)  NOT NULL,
            password_hash  VARCHAR(200) NOT NULL,
            display_name   VARCHAR(60)  NOT NULL,
            created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
            last_login_at  TIMESTAMP    NULL,
            PRIMARY KEY (id),
            UNIQUE KEY uk_app_user_username (username)
        )""",
        // CAP-7: profit alerts, mapped as a JPA entity
        """
        CREATE TABLE IF NOT EXISTS profit_alert (
            id                 BIGINT         NOT NULL AUTO_INCREMENT,
            owner              VARCHAR(40)    NOT NULL,
            min_profit         DECIMAL(10, 6) NOT NULL,
            currency_code      CHAR(3)        NULL,
            active             BOOLEAN        NOT NULL DEFAULT TRUE,
            trigger_count      INT            NOT NULL DEFAULT 0,
            last_triggered_at  DATETIME(3)    NULL,
            last_path          VARCHAR(120)   NULL,
            created_at         TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
            PRIMARY KEY (id),
            CONSTRAINT chk_alert_profit CHECK (min_profit >= 0)
        )"""
    };

    private final DatabaseConnection database;

    public DatabaseInitializer(DatabaseConnection database) {
        this.database = database;
    }

    /** DDL has no user values, so a plain Statement is used here (and only here). */
    public void createTables() {
        try (Connection connection = database.getConnection();
             Statement statement = connection.createStatement()) {
            for (String ddl : CREATE_TABLES) {
                statement.executeUpdate(ddl);
            }
        } catch (SQLException e) {
            throw database.failure("Could not create the tables", e);
        }
    }

    public void seedIfEmpty() {
        try (Connection connection = database.getConnection()) {
            if (countRows(connection, "SELECT COUNT(*) FROM currency") == 0) {
                seedCurrencies(connection);
            }
            if (countRows(connection, "SELECT COUNT(*) FROM app_setting") == 0) {
                seedSettings(connection);
            }
        } catch (SQLException e) {
            throw database.failure("Could not insert the default data", e);
        }
    }

    private long countRows(Connection connection, String countSql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(countSql);
             ResultSet rs = statement.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        }
    }

    private void seedCurrencies(Connection connection) throws SQLException {
        String sql = "INSERT INTO currency (code, name, usd_price, active) VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (Currency currency : Currency.defaults()) {
                statement.setString(1, currency.code());
                statement.setString(2, currency.name());
                statement.setDouble(3, currency.usdPrice());
                statement.setBoolean(4, currency.active());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void seedSettings(Connection connection) throws SQLException {
        String sql = "INSERT INTO app_setting (setting_key, setting_value) VALUES (?, ?)";
        String[][] defaults = {
            {SettingsDao.KEY_FEE, plain(AppSettings.DEFAULT_FEE)},
            {SettingsDao.KEY_SPREAD, plain(AppSettings.DEFAULT_SPREAD)},
            {SettingsDao.KEY_INJECT_CHANCE, plain(AppSettings.DEFAULT_INJECT_CHANCE)}
        };
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (String[] setting : defaults) {
                statement.setString(1, setting[0]);
                statement.setString(2, setting[1]);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    /** A plain decimal string such as 0.0005 (never 5.0E-4). */
    private static String plain(double value) {
        return BigDecimal.valueOf(value).toPlainString();
    }
}
