package dev.lovelace.loveactivities.config;

import dev.lovelace.loveactivities.LoveActivities;
import dev.lovelace.loveactivities.api.GameType;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

public class ConfigManager {

    private final LoveActivities plugin;
    private String language;
    private double maxDistance;
    private int requestTimeoutSeconds;
    private int requestCooldownSeconds;
    private int afkTurnTimeoutSeconds;
    private int betCountdownSeconds;
    private boolean preGameTutorial;

    private long minBet;
    private long maxBetGlobal;
    private List<Long> quickAmounts;
    private final Map<GameType, Long> gameMaxBets = new EnumMap<>(GameType.class);

    private boolean soundsEnabled;
    private boolean particlesEnabled;

    private String sqliteFileName;
    private int poolSize;
    private int minIdle;
    private int connectionTimeoutMs;
    private String betsLogFile;

    public ConfigManager(LoveActivities plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        this.language = config.getString("language", "ru");
        this.maxDistance = config.getDouble("gameplay.max_distance", 3.0);
        this.requestTimeoutSeconds = config.getInt("gameplay.request_timeout_seconds", 30);
        this.requestCooldownSeconds = config.getInt("gameplay.request_cooldown_seconds", 5);
        this.afkTurnTimeoutSeconds = config.getInt("gameplay.afk_turn_timeout_seconds", 45);
        this.betCountdownSeconds = config.getInt("gameplay.bet_countdown_seconds", 3);
        this.preGameTutorial = config.getBoolean("gameplay.pre_game_tutorial", false);

        this.minBet = config.getLong("betting.min_bet", 1L);
        this.maxBetGlobal = config.getLong("betting.max_bet_global", 1000000L);

        List<Long> quick = config.getLongList("betting.quick_amounts");
        if (quick.isEmpty()) {
            this.quickAmounts = List.of(1L, 5L, 10L, 50L, 100L, 500L, 1000L);
        } else {
            this.quickAmounts = Collections.unmodifiableList(quick);
        }

        this.gameMaxBets.clear();
        for (GameType game : GameType.values()) {
            long max = config.getLong("betting.limits." + game.getId(), this.maxBetGlobal);
            this.gameMaxBets.put(game, max);
        }

        this.soundsEnabled = config.getBoolean("effects.sounds_enabled", true);
        this.particlesEnabled = config.getBoolean("effects.particles_enabled", true);

        this.sqliteFileName = config.getString("database.file_name", "data.db");
        this.poolSize = config.getInt("database.maximum_pool_size", 5);
        this.minIdle = config.getInt("database.minimum_idle", 2);
        this.connectionTimeoutMs = config.getInt("database.connection_timeout_ms", 10000);

        this.npcDialoguesEnabled = config.getBoolean("npc.dialogues_enabled", true);
        String format = config.getString("npc.dialogue_format", DEFAULT_DIALOGUE_FORMAT);
        // Servers that kept the old default (gradient and brackets around the name) get the new plain one.
        if (LEGACY_DIALOGUE_FORMATS.contains(format)) {
            format = DEFAULT_DIALOGUE_FORMAT;
        }
        this.npcDialogueFormat = format;
        this.npcDefaultAcceptChance = config.getDouble("npc.default_accept_chance", 1.0);

        this.betsLogFile = config.getString("logging.bets_log_file", "bets.log");
    }

    private boolean npcDialoguesEnabled;
    /** {npc} already carries its own &a colour (see NpcNames), so the format adds none. */
    public static final String DEFAULT_DIALOGUE_FORMAT = "{npc} <dark_gray>»</dark_gray> <gray>{text}</gray>";
    private static final java.util.Set<String> LEGACY_DIALOGUE_FORMATS = java.util.Set.of(
            "<gradient:#FF9966:#FF5E62>[{npc}]</gradient> <dark_gray>»</dark_gray> <gray>{text}</gray>",
            "<gradient:#FF9966:#FF5E62><bold>[{npc}]</bold></gradient> <dark_gray>»</dark_gray> <gray>{text}</gray>");

    private String npcDialogueFormat;
    private double npcDefaultAcceptChance;

    public boolean isNpcDialoguesEnabled() {
        return npcDialoguesEnabled;
    }

    public String getNpcDialogueFormat() {
        return npcDialogueFormat;
    }

    public double getNpcDefaultAcceptChance() {
        return npcDefaultAcceptChance;
    }

    public String getLanguage() {
        return language;
    }

    public double getMaxDistance() {
        return maxDistance;
    }

    public int getRequestTimeoutSeconds() {
        return requestTimeoutSeconds;
    }

    public int getRequestCooldownSeconds() {
        return requestCooldownSeconds;
    }

    public int getAfkTurnTimeoutSeconds() {
        return afkTurnTimeoutSeconds;
    }

    public int getBetCountdownSeconds() {
        return betCountdownSeconds;
    }

    public long getMinBet() {
        return minBet;
    }

    public long getMaxBetGlobal() {
        return maxBetGlobal;
    }

    public long getMaxBet(GameType game) {
        return gameMaxBets.getOrDefault(game, maxBetGlobal);
    }

    public List<Long> getQuickAmounts() {
        return quickAmounts;
    }

    public boolean isSoundsEnabled() {
        return soundsEnabled;
    }

    public boolean isParticlesEnabled() {
        return particlesEnabled;
    }

    public String getSqliteFileName() {
        return sqliteFileName;
    }

    public int getPoolSize() {
        return poolSize;
    }

    public int getMinIdle() {
        return minIdle;
    }

    public int getConnectionTimeoutMs() {
        return connectionTimeoutMs;
    }

    public String getBetsLogFile() {
        return betsLogFile;
    }

    public boolean isPreGameTutorial() {
        return preGameTutorial;
    }
}
