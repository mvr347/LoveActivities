package dev.lovelace.loveactivities.database;

import dev.lovelace.loveactivities.LoveActivities;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class OfflinePayoutDao {

    private final DatabaseManager db;

    public OfflinePayoutDao(DatabaseManager db) {
        this.db = db;
    }

    public void addPayout(UUID uuid, long amount, String reason) {
        db.runAsync(() -> {
            String sql = "INSERT INTO offline_payouts (uuid, amount, reason) VALUES (?, ?, ?)";
            try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setLong(2, amount);
                ps.setString(3, reason != null ? reason : "");
                ps.executeUpdate();
            } catch (SQLException e) {
                LoveActivities.getInstance().getLogger().log(Level.SEVERE, "Failed to save offline payout for " + uuid, e);
            }
        });
    }

    public CompletableFuture<Long> claimPayouts(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            long total = 0L;
            List<Integer> ids = new ArrayList<>();
            String selectSql = "SELECT id, amount FROM offline_payouts WHERE uuid = ?";

            try (Connection conn = db.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                        ps.setString(1, uuid.toString());
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                ids.add(rs.getInt("id"));
                                total += rs.getLong("amount");
                            }
                        }
                    }
                    if (!ids.isEmpty()) {
                        StringBuilder deleteSql = new StringBuilder("DELETE FROM offline_payouts WHERE id IN (");
                        for (int i = 0; i < ids.size(); i++) {
                            if (i > 0) deleteSql.append(",");
                            deleteSql.append("?");
                        }
                        deleteSql.append(")");

                        try (PreparedStatement ps = conn.prepareStatement(deleteSql.toString())) {
                            for (int i = 0; i < ids.size(); i++) {
                                ps.setInt(i + 1, ids.get(i));
                            }
                            ps.executeUpdate();
                        }
                    }
                    conn.commit();
                } catch (SQLException ex) {
                    conn.rollback();
                    throw ex;
                } finally {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException e) {
                LoveActivities.getInstance().getLogger().log(Level.SEVERE, "Failed to claim offline payouts for " + uuid, e);
            }
            return total;
        }, db.getAsyncExecutor());
    }
}
