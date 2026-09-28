package dev.lovelace.loveactivities;

import dev.lovelace.loveactivities.command.AdminActivityCommand;
import dev.lovelace.loveactivities.command.BoardGamesCommand;
import dev.lovelace.loveactivities.command.GameCommands;
import dev.lovelace.loveactivities.command.PlayerActivityCommand;
import dev.lovelace.loveactivities.config.ConfigManager;
import dev.lovelace.loveactivities.config.HeadManager;
import dev.lovelace.loveactivities.config.LocaleManager;
import dev.lovelace.loveactivities.database.DatabaseManager;
import dev.lovelace.loveactivities.database.OfflinePayoutDao;
import dev.lovelace.loveactivities.database.PlayerSettingsDao;
import dev.lovelace.loveactivities.database.PlayerStatsDao;
import dev.lovelace.loveactivities.integration.LoveCoreBridge;
import dev.lovelace.loveactivities.integration.PlaceholderHook;
import dev.lovelace.loveactivities.listener.CombatListener;
import dev.lovelace.loveactivities.listener.InventoryListener;
import dev.lovelace.loveactivities.listener.NpcInteractListener;
import dev.lovelace.loveactivities.listener.PlayerActivityListener;
import dev.lovelace.loveactivities.manager.*;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public class LoveActivities extends JavaPlugin {

    private static LoveActivities instance;

    private ConfigManager configManager;
    private LocaleManager localeManager;
    private HeadManager headManager;
    private DatabaseManager databaseManager;

    private PlayerSettingsDao playerSettingsDao;
    private PlayerStatsDao playerStatsDao;
    private OfflinePayoutDao offlinePayoutDao;

    private LoveCoreBridge loveCoreBridge;
    private BetLogger betLogger;

    private SettingsManager settingsManager;
    private StatsManager statsManager;
    private RequestManager requestManager;
    private SessionManager sessionManager;
    private NpcManager npcManager;

    @Override
    public void onEnable() {
        instance = this;

        // 1. Configurations & Textures
        this.configManager = new ConfigManager(this);
        this.configManager.load();

        this.localeManager = new LocaleManager(this);
        this.localeManager.load();

        this.headManager = new HeadManager(this);
        this.headManager.load();

        // 2. Database & DAOs
        this.databaseManager = new DatabaseManager(this);
        this.databaseManager.initialize();

        this.playerSettingsDao = new PlayerSettingsDao(databaseManager);
        this.playerStatsDao = new PlayerStatsDao(databaseManager);
        this.offlinePayoutDao = new OfflinePayoutDao(databaseManager);

        // 3. Integrations & Logger
        this.loveCoreBridge = new LoveCoreBridge();
        this.betLogger = new BetLogger(this);
        this.betLogger.initialize();

        // 4. Managers
        this.settingsManager = new SettingsManager(this, playerSettingsDao);
        this.statsManager = new StatsManager(this, playerStatsDao);
        this.requestManager = new RequestManager(this);
        this.sessionManager = new SessionManager(this);
        this.npcManager = new NpcManager(this);
        this.npcManager.load();

        // 5. Listeners
        Bukkit.getPluginManager().registerEvents(new InventoryListener(), this);
        Bukkit.getPluginManager().registerEvents(new PlayerActivityListener(this), this);
        Bukkit.getPluginManager().registerEvents(new CombatListener(this), this);
        Bukkit.getPluginManager().registerEvents(new NpcInteractListener(this), this);

        // 6. Commands
        // Each English command name has its own Russian-named command entry in plugin.yml
        // (not just an alias - see the comment there for why) gated by a separate permission,
        // both wired to the same executor here.
        GameCommands gameCommands = new GameCommands(this);
        registerCommand("blackjack", gameCommands);
        registerCommand("блэкджек", gameCommands);
        registerCommand("dice", gameCommands);
        registerCommand("кости", gameCommands);
        registerCommand("rps", gameCommands);
        registerCommand("кнб", gameCommands);
        registerCommand("gwent", gameCommands);
        registerCommand("гвинт", gameCommands);
        registerCommand("cards", gameCommands);
        registerCommand("карты", gameCommands);
        registerCommand("poker", gameCommands);
        registerCommand("покер", gameCommands);
        registerCommand("durak", gameCommands);
        registerCommand("дурак", gameCommands);
        registerCommand("war", gameCommands);
        registerCommand("война", gameCommands);
        registerCommand("pokerdice", gameCommands);
        registerCommand("покеркости", gameCommands);
        registerCommand("classicdice", gameCommands);
        registerCommand("киданиекостей", gameCommands);
        registerCommand("chess", gameCommands);
        registerCommand("шахматы", gameCommands);

        PlayerActivityCommand playerCommand = new PlayerActivityCommand(this);
        registerCommand("loveactivities", playerCommand);
        registerCommand("активности", playerCommand);

        BoardGamesCommand boardGamesCommand = new BoardGamesCommand(this);
        registerCommand("boardgames", boardGamesCommand);
        registerCommand("настолки", boardGamesCommand);

        AdminActivityCommand adminCommand = new AdminActivityCommand(this);
        registerCommand("loveactivitiesadmin", adminCommand);

        // 7. PlaceholderAPI Expansion
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new PlaceholderHook(this).register();
            getLogger().info("PlaceholderAPI hook registered successfully.");
        }

        // 8. Timers
        this.requestManager.startCleanupTask();
        this.sessionManager.startTicker();

        getLogger().info("LoveActivities v" + getDescription().getVersion() + " has been successfully enabled!");
    }

    private void registerCommand(String name, org.bukkit.command.CommandExecutor executor) {
        var cmd = getCommand(name);
        if (cmd != null) {
            cmd.setExecutor(executor);
            if (executor instanceof org.bukkit.command.TabCompleter completer) {
                cmd.setTabCompleter(completer);
            }
        } else {
            getLogger().log(Level.WARNING, "Failed to register command: /" + name);
        }
    }

    public void reloadAll() {
        this.configManager.load();
        this.localeManager.load();
        this.headManager.load();
        this.npcManager.load();
    }

    @Override
    public void onDisable() {
        safeDisable("SessionManager", () -> {
            if (sessionManager != null) sessionManager.shutdown();
        });

        safeDisable("RequestManager", () -> {
            if (requestManager != null) requestManager.shutdown();
        });

        safeDisable("SettingsManager", () -> {
            if (settingsManager != null) settingsManager.saveAll();
        });

        safeDisable("StatsManager", () -> {
            if (statsManager != null) statsManager.saveAll();
        });

        safeDisable("DatabaseManager", () -> {
            if (databaseManager != null) databaseManager.shutdown();
        });

        safeDisable("BetLogger", () -> {
            if (betLogger != null) betLogger.shutdown();
        });

        getLogger().info("LoveActivities has been successfully disabled.");
        instance = null;
    }

    private void safeDisable(String componentName, Runnable action) {
        try {
            action.run();
        } catch (Throwable t) {
            getLogger().log(Level.SEVERE, "Error while disabling " + componentName, t);
        }
    }

    public static LoveActivities getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public LocaleManager getLocaleManager() {
        return localeManager;
    }

    public HeadManager getHeadManager() {
        return headManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public OfflinePayoutDao getOfflinePayoutDao() {
        return offlinePayoutDao;
    }

    public LoveCoreBridge getLoveCoreBridge() {
        return loveCoreBridge;
    }

    public BetLogger getBetLogger() {
        return betLogger;
    }

    public SettingsManager getSettingsManager() {
        return settingsManager;
    }

    public StatsManager getStatsManager() {
        return statsManager;
    }

    public RequestManager getRequestManager() {
        return requestManager;
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public NpcManager getNpcManager() {
        return npcManager;
    }
}
