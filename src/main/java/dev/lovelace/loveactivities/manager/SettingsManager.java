package dev.lovelace.loveactivities.manager;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.lovelace.loveactivities.LoveActivities;
import dev.lovelace.loveactivities.api.GameType;
import dev.lovelace.loveactivities.database.PlayerSettings;
import dev.lovelace.loveactivities.database.PlayerSettingsDao;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class SettingsManager {

    private final LoveActivities plugin;
    private final PlayerSettingsDao dao;
    private final Cache<UUID, PlayerSettings> cache;

    public SettingsManager(LoveActivities plugin, PlayerSettingsDao dao) {
        this.plugin = plugin;
        this.dao = dao;
        this.cache = Caffeine.newBuilder()
                .expireAfterAccess(30, TimeUnit.MINUTES)
                .build();
        warmUpCache();
    }

    private void warmUpCache() {
        try {
            cache.asMap().values().iterator();
            cache.asMap().keySet().iterator();
            cache.asMap().entrySet().iterator();
        } catch (Throwable ignored) {
        }
    }

    public PlayerSettings getSettings(UUID uuid) {
        PlayerSettings cached = cache.getIfPresent(uuid);
        if (cached != null) return cached;
        PlayerSettings def = new PlayerSettings(uuid);
        cache.put(uuid, def);
        loadAsync(uuid);
        return def;
    }

    public void loadAsync(UUID uuid) {
        dao.loadSettings(uuid).thenAccept(settings -> cache.put(uuid, settings));
    }

    public void unload(UUID uuid) {
        PlayerSettings settings = cache.asMap().remove(uuid);
        if (settings != null) {
            dao.saveSettings(settings);
        }
    }

    public boolean isDnd(UUID uuid) {
        return getSettings(uuid).isDnd();
    }

    public void setDnd(UUID uuid, boolean dnd) {
        PlayerSettings s = getSettings(uuid);
        s.setDnd(dnd);
        dao.saveSettings(s);
    }

    public boolean toggleDnd(UUID uuid) {
        PlayerSettings s = getSettings(uuid);
        boolean newState = !s.isDnd();
        s.setDnd(newState);
        dao.saveSettings(s);
        return newState;
    }

    public boolean hasSounds(UUID uuid) {
        if (!plugin.getConfigManager().isSoundsEnabled()) return false;
        return getSettings(uuid).isSounds();
    }

    public boolean toggleSounds(UUID uuid) {
        PlayerSettings s = getSettings(uuid);
        boolean newState = !s.isSounds();
        s.setSounds(newState);
        dao.saveSettings(s);
        return newState;
    }

    public boolean hasParticles(UUID uuid) {
        if (!plugin.getConfigManager().isParticlesEnabled()) return false;
        return getSettings(uuid).isParticles();
    }

    public boolean toggleParticles(UUID uuid) {
        PlayerSettings s = getSettings(uuid);
        boolean newState = !s.isParticles();
        s.setParticles(newState);
        dao.saveSettings(s);
        return newState;
    }

    public boolean isGameBlacklisted(UUID uuid, GameType game) {
        return getSettings(uuid).isGameBlacklisted(game);
    }

    public void toggleGameBlacklist(UUID uuid, GameType game) {
        PlayerSettings s = getSettings(uuid);
        s.toggleGameBlacklist(game);
        dao.saveSettings(s);
    }

    public void setAllGamesBlacklisted(UUID uuid, boolean blacklisted) {
        PlayerSettings s = getSettings(uuid);
        s.setAllGamesBlacklisted(blacklisted);
        dao.saveSettings(s);
    }

    public void saveAll() {
        try {
            for (PlayerSettings s : cache.asMap().values()) {
                dao.saveSettingsSync(s);
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("Failed to save all cached settings during shutdown: " + t.getMessage());
        }
    }
}
