package dev.lovelace.loveactivities.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.lovelace.loveactivities.LoveActivities;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

public class DatabaseManager {

    private final LoveActivities plugin;
    private HikariDataSource dataSource;
    private final ExecutorService asyncExecutor = Executors.newFixedThreadPool(4);

    public DatabaseManager(LoveActivities plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        File dbFile = new File(plugin.getDataFolder(), plugin.getConfigManager().getSqliteFileName());
        if (!dbFile.getParentFile().exists()) {
            dbFile.getParentFile().mkdirs();
        }

        HikariConfig config = new HikariConfig();
        config.setPoolName("LoveActivitiesPool");
        config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        config.setDriverClassName("org.sqlite.JDBC");
        config.setMaximumPoolSize(plugin.getConfigManager().getPoolSize());
        config.setMinimumIdle(plugin.getConfigManager().getMinIdle());
        config.setConnectionTimeout(plugin.getConfigManager().getConnectionTimeoutMs());
        config.setIdleTimeout(60000);
        config.setMaxLifetime(1800000);
        config.addDataSourceProperty("journal_mode", "WAL");
        config.addDataSourceProperty("synchronous", "NORMAL");

        this.dataSource = new HikariDataSource(config);

        createTables();
    }

    private void createTables() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS player_settings (
                    uuid VARCHAR(36) PRIMARY KEY,
                    dnd BOOLEAN DEFAULT 0,
                    sounds BOOLEAN DEFAULT 1,
                    particles BOOLEAN DEFAULT 1,
                    blacklisted_games TEXT DEFAULT ''
                );
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS player_stats (
                    uuid VARCHAR(36) NOT NULL,
                    game VARCHAR(32) NOT NULL,
                    wins INT DEFAULT 0,
                    losses INT DEFAULT 0,
                    won_money BIGINT DEFAULT 0,
                    lost_money BIGINT DEFAULT 0,
                    PRIMARY KEY (uuid, game)
                );
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS offline_payouts (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    uuid VARCHAR(36) NOT NULL,
                    amount BIGINT NOT NULL,
                    reason TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
            """);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create SQLite tables", e);
        }
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("Data source is closed");
        }
        return dataSource.getConnection();
    }

    public CompletableFuture<Void> runAsync(Runnable runnable) {
        return CompletableFuture.runAsync(runnable, asyncExecutor)
                .exceptionally(ex -> {
                    plugin.getLogger().log(Level.SEVERE, "Database async operation error", ex);
                    return null;
                });
    }

    public ExecutorService getAsyncExecutor() {
        return asyncExecutor;
    }

    public void shutdown() {
        asyncExecutor.shutdown();
        try {
            if (!asyncExecutor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                asyncExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            asyncExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
