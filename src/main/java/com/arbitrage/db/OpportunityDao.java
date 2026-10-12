package com.arbitrage.db;

import com.arbitrage.model.FeedSource;
import com.arbitrage.model.Opportunity;
import com.arbitrage.model.OpportunityLeg;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * All SQL for the opportunity and opportunity_leg tables.
 * An opportunity and its legs are saved together in one transaction.
 */
@Component
public class OpportunityDao {

    /** The numbers of the History summary line. */
    public record Summary(long count, double averageProfit, double bestProfit) {
    }

    private final DatabaseConnection database;

    public OpportunityDao(DatabaseConnection database) {
        this.database = database;
    }

    /** Saves the opportunity and all its legs, or nothing at all. Returns the new id. */
    public long insertWithLegs(Opportunity opportunity, List<OpportunityLeg> legs) {
        try (Connection connection = database.getConnection()) {
            connection.setAutoCommit(false);      // start a transaction
            try {
                long id = insertOpportunity(connection, opportunity);
                insertLegs(connection, id, legs);
                connection.commit();              // both inserts worked: make them permanent
                return id;
            } catch (SQLException e) {
                connection.rollback();            // something failed: undo the opportunity row too
                throw e;
            }
        } catch (SQLException e) {
            if (e.getSQLState() != null && e.getSQLState().startsWith("23")) {
                // 23xxx = constraint violation: a leg uses a code that is not in the currency table
                throw new DatabaseException("Unknown currency in " + opportunity.pathText()
                        + " – add it on the Currencies screen", e);
            }
            throw database.failure("Could not save the opportunity", e);
        }
    }

    private long insertOpportunity(Connection connection, Opportunity o) throws SQLException {
        String sql = "INSERT INTO opportunity (path_text, path_length, profit_percent, fee, source, detected_at) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, o.pathText());
            statement.setInt(2, o.path().size() - 1);
            statement.setDouble(3, o.profitPercent());
            statement.setDouble(4, o.fee());
            statement.setString(5, o.source().name());
            statement.setTimestamp(6, Timestamp.from(o.detectedAt()));
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);           // the AUTO_INCREMENT id MySQL gave the new row
            }
        }
    }

    private void insertLegs(Connection connection, long id, List<OpportunityLeg> legs) throws SQLException {
        String sql = "INSERT INTO opportunity_leg (opportunity_id, leg_no, from_code, to_code, rate) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (OpportunityLeg leg : legs) {
                statement.setLong(1, id);
                statement.setInt(2, leg.legNo());
                statement.setString(3, leg.fromCode());
                statement.setString(4, leg.toCode());
                statement.setDouble(5, leg.rate());
                statement.addBatch();             // collect the rows ...
            }
            statement.executeBatch();             // ... and send them in one go
        }
    }

    /** Newest first, at most 500 rows. codeLike is a LIKE pattern such as "%INR%". */
    public List<Opportunity> search(LocalDateTime since, double minProfit, String codeLike) {
        String sql = "SELECT id, path_text, profit_percent, fee, source, detected_at FROM opportunity "
                + "WHERE detected_at >= ? AND profit_percent >= ? AND path_text LIKE ? "
                + "ORDER BY detected_at DESC LIMIT 500";
        List<Opportunity> found = new ArrayList<>();
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, Timestamp.valueOf(since));
            statement.setDouble(2, minProfit);
            statement.setString(3, codeLike);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    found.add(new Opportunity(rs.getLong("id"), List.of(rs.getString("path_text").split(" → ")),
                            rs.getDouble("profit_percent"), rs.getDouble("fee"),
                            FeedSource.valueOf(rs.getString("source")), rs.getTimestamp("detected_at").toInstant()));
                }
            }
        } catch (SQLException e) {
            throw database.failure("Could not load the history", e);
        }
        return found;
    }

    public Summary summary(LocalDateTime since, double minProfit, String codeLike) {
        String sql = "SELECT COUNT(*), AVG(profit_percent), MAX(profit_percent) FROM opportunity "
                + "WHERE detected_at >= ? AND profit_percent >= ? AND path_text LIKE ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, Timestamp.valueOf(since));
            statement.setDouble(2, minProfit);
            statement.setString(3, codeLike);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return new Summary(rs.getLong(1), rs.getDouble(2), rs.getDouble(3));
            }
        } catch (SQLException e) {
            throw database.failure("Could not load the history summary", e);
        }
    }

    public List<OpportunityLeg> findLegs(long opportunityId) {
        String sql = "SELECT leg_no, from_code, to_code, rate FROM opportunity_leg "
                + "WHERE opportunity_id = ? ORDER BY leg_no";
        List<OpportunityLeg> legs = new ArrayList<>();
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, opportunityId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    legs.add(new OpportunityLeg(rs.getInt("leg_no"), rs.getString("from_code"),
                            rs.getString("to_code"), rs.getDouble("rate")));
                }
            }
        } catch (SQLException e) {
            throw database.failure("Could not load the legs", e);
        }
        return legs;
    }

    /** Deletes one opportunity; its legs go too (ON DELETE CASCADE). Returns the rows deleted. */
    public int delete(long id) {
        String sql = "DELETE FROM opportunity WHERE id = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            return statement.executeUpdate();
        } catch (SQLException e) {
            throw database.failure("Could not delete the opportunity", e);
        }
    }

    public int deleteAll() {
        String sql = "DELETE FROM opportunity";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            return statement.executeUpdate();
        } catch (SQLException e) {
            throw database.failure("Could not clear the history", e);
        }
    }
}
