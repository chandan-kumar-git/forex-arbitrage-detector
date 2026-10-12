package com.arbitrage.db;

import com.arbitrage.model.Currency;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.List;

/**
 * All SQL for the currency table: full CRUD with PreparedStatement.
 * Every method opens its own connection and closes it with try-with-resources.
 */
@Component
public class CurrencyDao {

    private final DatabaseConnection database;

    public CurrencyDao(DatabaseConnection database) {
        this.database = database;
    }

    public List<Currency> findAll() {
        return query("SELECT code, name, usd_price, active FROM currency ORDER BY code");
    }

    public boolean exists(String code) {
        String sql = "SELECT 1 FROM currency WHERE code = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, code);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();                 // true when at least one row came back
            }
        } catch (SQLException e) {
            throw database.failure("Could not check the currency", e);
        }
    }

    public void insert(Currency currency) {
        String sql = "INSERT INTO currency (code, name, usd_price, active) VALUES (?, ?, ?, ?)";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, currency.code());
            statement.setString(2, currency.name());
            statement.setDouble(3, currency.usdPrice());
            statement.setBoolean(4, currency.active());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw database.failure("Could not add the currency", e);
        }
    }

    public void update(Currency currency) {
        String sql = "UPDATE currency SET name = ?, usd_price = ?, active = ? WHERE code = ?";
        int rows;
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, currency.name());
            statement.setDouble(2, currency.usdPrice());
            statement.setBoolean(3, currency.active());
            statement.setString(4, currency.code());
            rows = statement.executeUpdate();     // number of rows that were changed
        } catch (SQLException e) {
            throw database.failure("Could not update the currency", e);
        }
        if (rows == 0) {
            throw new DatabaseException("Currency not found", null);
        }
    }

    public void delete(String code) {
        String sql = "DELETE FROM currency WHERE code = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, code);
            statement.executeUpdate();
        } catch (SQLIntegrityConstraintViolationException e) {
            // The foreign key from opportunity_leg (ON DELETE RESTRICT) stopped the delete.
            throw new DatabaseException("Used in history – deactivate it instead", e);
        } catch (SQLException e) {
            throw database.failure("Could not delete the currency", e);
        }
    }

    /** Runs a SELECT without parameters and turns every row into a Currency. */
    private List<Currency> query(String sql) {
        List<Currency> currencies = new ArrayList<>();
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                currencies.add(new Currency(rs.getString("code"), rs.getString("name"),
                        rs.getDouble("usd_price"), rs.getBoolean("active")));
            }
        } catch (SQLException e) {
            throw database.failure("Could not load the currencies", e);
        }
        return currencies;
    }
}
