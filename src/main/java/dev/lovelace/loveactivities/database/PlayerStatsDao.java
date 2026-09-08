package dev.lovelace.loveactivities.database;

import dev.lovelace.loveactivities.LoveActivities;
import dev.lovelace.loveactivities.api.GameType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class PlayerStatsDao {

    private final DatabaseManager db;

    public record LeaderboardEntry(UUID uuid, String name, long value) {}

    public PlayerStatsDao(DatabaseManager db) {
        this.db = db;
    }

    public CompletableFuture<Map<GameType, PlayerStats>> loadAllStats(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            Map<GameType, PlayerStats> map = new EnumMap<>(GameType.class);
            for (GameType g : GameType.values()) {
                map.put(g, new PlayerStats(uuid, g));
            }
            String sql = "SELECT game, wins, losses, won_money, lost_money FROM player_stats WHERE uuid = ?";
            try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String gameId = rs.getString("game");
                        GameType type = GameType.fromString(gameId);
                        if (type != null) {
                            int wins = rs.getInt("wins");
                            int losses = rs.getInt("losses");
                            long won = rs.getLong("won_money");
                            long lost = rs.getLong("lost_money");
                            map.put(type, new PlayerStats(uuid, type, wins, losses, won, lost));
                        }
                    }
                }
            } catch (SQLException e) {
                LoveActivities.getInstance().getLogger().log(Level.SEVERE, "Failed to load player stats for " + uuid, e);
            }
            return map;
        }, db.getAsyncExecutor());
    }

    public void saveStats(PlayerStats stats) {
        db.runAsync(() -> saveStatsSync(stats));
    }

    public void saveStatsSync(PlayerStats stats) {
        String sql = """
            INSERT INTO player_stats (uuid, game, wins, losses, won_money, lost_money)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT(uuid, game) DO UPDATE SET
                wins = excluded.wins,
                losses = excluded.losses,
                won_money = excluded.won_money,
                lost_money = excluded.lost_money;
        """;
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, stats.getUuid().toString());
            ps.setString(2, stats.getGame().getId());
            ps.setInt(3, stats.getWins());
            ps.setInt(4, stats.getLosses());
            ps.setLong(5, stats.getWonMoney());
            ps.setLong(6, stats.getLostMoney());
            ps.executeUpdate();
        } catch (SQLException e) {
            LoveActivities.getInstance().getLogger().log(Level.SEVERE, "Failed to save player stats for " + stats.getUuid(), e);
        }
    }

    public List<LeaderboardEntry> getTopWins(GameType game, int limit) {
        List<LeaderboardEntry> list = new ArrayList<>();
        String sql = "SELECT uuid, wins FROM player_stats WHERE game = ? ORDER BY wins DESC LIMIT ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, game.getId());
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    int wins = rs.getInt("wins");
                    Player online = Bukkit.getPlayer(uuid);
                    String name = online != null ? online.getName() : Bukkit.getOfflinePlayer(uuid).getName();
                    list.add(new LeaderboardEntry(uuid, name != null ? name : "Игрок", wins));
                }
            }
        } catch (SQLException e) {
            LoveActivities.getInstance().getLogger().log(Level.SEVERE, "Failed to query top wins", e);
        }
        return list;
    }

    public List<LeaderboardEntry> getTopWonMoney(GameType game, int limit) {
        List<LeaderboardEntry> list = new ArrayList<>();
        String sql = "SELECT uuid, won_money FROM player_stats WHERE game = ? ORDER BY won_money DESC LIMIT ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, game.getId());
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    long won = rs.getLong("won_money");
                    Player online = Bukkit.getPlayer(uuid);
                    String name = online != null ? online.getName() : Bukkit.getOfflinePlayer(uuid).getName();
                    list.add(new LeaderboardEntry(uuid, name != null ? name : "Игрок", won));
                }
            }
        } catch (SQLException e) {
            LoveActivities.getInstance().getLogger().log(Level.SEVERE, "Failed to query top won money", e);
        }
        return list;
    }
}
