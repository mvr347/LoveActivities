package dev.lovelace.loveactivities.manager;

import dev.lovelace.loveactivities.LoveActivities;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

public class BetLogger {

    private final LoveActivities plugin;
    private final ExecutorService logExecutor = Executors.newSingleThreadExecutor();
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private File logFile;

    public BetLogger(LoveActivities plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        this.logFile = new File(plugin.getDataFolder(), plugin.getConfigManager().getBetsLogFile());
        if (!logFile.getParentFile().exists()) {
            logFile.getParentFile().mkdirs();
        }
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to create bets.log file", e);
            }
        }
    }

    public void log(String message) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String formattedLine = "[" + timestamp + "] " + message + System.lineSeparator();
        logExecutor.submit(() -> {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
                writer.write(formattedLine);
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to write to bets.log", e);
            }
        });
    }

    public void shutdown() {
        logExecutor.shutdown();
    }
}
