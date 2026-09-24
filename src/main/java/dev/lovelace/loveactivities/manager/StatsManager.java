package dev.lovelace.loveactivities.manager;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import dev.lovelace.loveactivities.LoveActivities;
import dev.lovelace.loveactivities.api.GameType;
import dev.lovelace.loveactivities.database.PlayerStats;
import dev.lovelace.loveactivities.database.PlayerStatsDao;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class StatsManager {

    private final LoveActivities plugin;
    private final PlayerStatsDao dao;
    private final Cache<UUID, Map<GameType, PlayerStats>> cache;

    // Leaderboard caches with 60 seconds expiration
    private final LoadingCache<GameType, List<PlayerStatsDao.LeaderboardEntry>> topWinsCache;
    private final LoadingCache<GameType, List<PlayerStatsDao.LeaderboardEntry>> topWonMoneyCache;

    public StatsManager(LoveActivities plugin, PlayerStatsDao dao) {
        this.plugin = plugin;
        this.dao = dao;

        // executor(Runnable::run): без этого Caffeine планирует обслуживание кэша (эвикшены)
        // на общем ForkJoinPool.commonPool() — потоке, который переживает выгрузку плагина.
        // После disable/reload класслоадер плагина закрывается, а отложенная задача на
        // commonPool всё ещё пытается лениво подгрузить класс через уже закрытый classloader
        // -> "zip file closed" / IllegalStateException в логах. Синхронное исполнение на
        // вызывающем потоке убирает этот класс багов целиком.
        this.cache = Caffeine.newBuilder()
                .expireAfterAccess(30, TimeUnit.MINUTES)
                .executor(Runnable::run)
                .build();

        this.topWinsCache = Caffeine.newBuilder()
                .expireAfterWrite(60, TimeUnit.SECONDS)
                .executor(Runnable::run)
                .build(game -> dao.getTopWins(game, 10));

        this.topWonMoneyCache = Caffeine.newBuilder()
                .expireAfterWrite(60, TimeUnit.SECONDS)
                .executor(Runnable::run)
                .build(game -> dao.getTopWonMoney(game, 10));

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

    public Map<GameType, PlayerStats> getPlayerStats(UUID uuid) {
        Map<GameType, PlayerStats> cached = cache.getIfPresent(uuid);
        if (cached != null) return cached;
        Map<GameType, PlayerStats> def = new EnumMap<>(GameType.class);
        for (GameType g : GameType.values()) {
            def.put(g, new PlayerStats(uuid, g));
        }
        cache.put(uuid, def);
        loadAsync(uuid);
        return def;
    }

    public void loadAsync(UUID uuid) {
        dao.loadAllStats(uuid).thenAccept(statsMap -> cache.put(uuid, statsMap));
    }

    public PlayerStats getGameStats(UUID uuid, GameType game) {
        return getPlayerStats(uuid).computeIfAbsent(game, g -> new PlayerStats(uuid, g));
    }

    public void recordWin(UUID winner, UUID loser, GameType game, long bet) {
        PlayerStats winStats = getGameStats(winner, game);
        winStats.addWin(bet);
        dao.saveStats(winStats);

        PlayerStats loseStats = getGameStats(loser, game);
        loseStats.addLoss(bet);
        dao.saveStats(loseStats);

        topWinsCache.invalidate(game);
        topWonMoneyCache.invalidate(game);
    }

    public void unload(UUID uuid) {
        Map<GameType, PlayerStats> statsMap = cache.asMap().remove(uuid);
        if (statsMap != null) {
            for (PlayerStats stats : statsMap.values()) {
                dao.saveStats(stats);
            }
        }
    }

    public List<PlayerStatsDao.LeaderboardEntry> getTopWins(GameType game, int limit) {
        List<PlayerStatsDao.LeaderboardEntry> list = topWinsCache.get(game);
        if (list == null) return Collections.emptyList();
        return list.subList(0, Math.min(limit, list.size()));
    }

    public List<PlayerStatsDao.LeaderboardEntry> getTopWonMoney(GameType game, int limit) {
        List<PlayerStatsDao.LeaderboardEntry> list = topWonMoneyCache.get(game);
        if (list == null) return Collections.emptyList();
        return list.subList(0, Math.min(limit, list.size()));
    }

    public void saveAll() {
        try {
            for (Map<GameType, PlayerStats> statsMap : cache.asMap().values()) {
                for (PlayerStats stats : statsMap.values()) {
                    dao.saveStatsSync(stats);
                }
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("Failed to save all cached stats during shutdown: " + t.getMessage());
        }
    }
}
