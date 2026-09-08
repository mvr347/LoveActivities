package dev.lovelace.loveactivities.database;

import dev.lovelace.loveactivities.LoveActivities;
import dev.lovelace.loveactivities.api.GameType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class PlayerSettingsDao {

    private final DatabaseManager db;

    public PlayerSettingsDao(DatabaseManager db) {
        this.db = db;
    }

    public CompletableFuture<PlayerSettings> loadSettings(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT dnd, sounds, particles, blacklisted_games FROM player_settings WHERE uuid = ?";
            try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        boolean dnd = rs.getBoolean("dnd");
                        boolean sounds = rs.getBoolean("sounds");
                        boolean particles = rs.getBoolean("particles");
                        String blacklistStr = rs.getString("blacklisted_games");
                        Set<GameType> blacklist = PlayerSettings.deserializeBlacklist(blacklistStr);
                        return new PlayerSettings(uuid, dnd, sounds, particles, blacklist);
                    }
                }
            } catch (SQLException e) {
                LoveActivities.getInstance().getLogger().log(Level.SEVERE, "Failed to load player settings for " + uuid, e);
            }
            return new PlayerSettings(uuid);
        }, db.getAsyncExecutor());
    }

    public void saveSettings(PlayerSettings settings) {
        db.runAsync(() -> saveSettingsSync(settings));
    }

    public void saveSettingsSync(PlayerSettings settings) {
        String sql = """
            INSERT INTO player_settings (uuid, dnd, sounds, particles, blacklisted_games)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT(uuid) DO UPDATE SET
                dnd = excluded.dnd,
                sounds = excluded.sounds,
                particles = excluded.particles,
                blacklisted_games = excluded.blacklisted_games;
        """;
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, settings.getUuid().toString());
            ps.setBoolean(2, settings.isDnd());
            ps.setBoolean(3, settings.isSounds());
            ps.setBoolean(4, settings.isParticles());
            ps.setString(5, settings.serializeBlacklist());
            ps.executeUpdate();
        } catch (SQLException e) {
            LoveActivities.getInstance().getLogger().log(Level.SEVERE, "Failed to save player settings for " + settings.getUuid(), e);
        }
    }
}
