package dev.lovelace.loveactivities.integration;

import dev.lovelace.loveactivities.LoveActivities;
import dev.lovelace.loveactivities.api.GameType;
import dev.lovelace.loveactivities.database.PlayerStats;
import dev.lovelace.loveactivities.database.PlayerStatsDao;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class PlaceholderHook extends PlaceholderExpansion {

    private final LoveActivities plugin;

    public PlaceholderHook(LoveActivities plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "loveactivities";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Lovelace";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        String lower = params.toLowerCase();

        // Leaderboard placeholders: top_wins_<game>_<rank>, top_won_money_<game>_<rank>
        if (lower.startsWith("top_wins_")) {
            String[] parts = lower.substring("top_wins_".length()).split("_");
            if (parts.length >= 2) {
                GameType game = GameType.fromString(parts[0]);
                try {
                    int rank = Integer.parseInt(parts[1]);
                    if (game != null && rank > 0) {
                        List<PlayerStatsDao.LeaderboardEntry> top = plugin.getStatsManager().getTopWins(game, rank);
                        if (top.size() >= rank) {
                            PlayerStatsDao.LeaderboardEntry entry = top.get(rank - 1);
                            return entry.name() + " (" + entry.value() + ")";
                        }
                        return "---";
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        if (lower.startsWith("top_won_money_")) {
            String[] parts = lower.substring("top_won_money_".length()).split("_");
            if (parts.length >= 2) {
                GameType game = GameType.fromString(parts[0]);
                try {
                    int rank = Integer.parseInt(parts[1]);
                    if (game != null && rank > 0) {
                        List<PlayerStatsDao.LeaderboardEntry> top = plugin.getStatsManager().getTopWonMoney(game, rank);
                        if (top.size() >= rank) {
                            PlayerStatsDao.LeaderboardEntry entry = top.get(rank - 1);
                            return entry.name() + " (" + entry.value() + ")";
                        }
                        return "---";
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        if (player == null) return null;

        if (lower.equals("dnd")) {
            return plugin.getSettingsManager().isDnd(player.getUniqueId()) ? "Включен" : "Выключен";
        }

        Map<GameType, PlayerStats> allStats = plugin.getStatsManager().getPlayerStats(player.getUniqueId());

        if (lower.equals("wins_total")) {
            int total = 0;
            for (PlayerStats st : allStats.values()) total += st.getWins();
            return String.valueOf(total);
        }

        if (lower.equals("losses_total")) {
            int total = 0;
            for (PlayerStats st : allStats.values()) total += st.getLosses();
            return String.valueOf(total);
        }

        if (lower.equals("won_money_total")) {
            long total = 0;
            for (PlayerStats st : allStats.values()) total += st.getWonMoney();
            return String.valueOf(total);
        }

        if (lower.equals("lost_money_total")) {
            long total = 0;
            for (PlayerStats st : allStats.values()) total += st.getLostMoney();
            return String.valueOf(total);
        }

        if (lower.equals("winrate_total")) {
            int wins = 0;
            int losses = 0;
            for (PlayerStats st : allStats.values()) {
                wins += st.getWins();
                losses += st.getLosses();
            }
            int total = wins + losses;
            if (total == 0) return "0.0%";
            return String.format("%.1f%%", ((double) wins / total) * 100.0);
        }

        // Per-game placeholders: wins_<game>, losses_<game>, won_money_<game>, lost_money_<game>, winrate_<game>
        for (GameType game : GameType.values()) {
            String gId = game.getId();
            PlayerStats st = allStats.get(game);
            if (lower.equals("wins_" + gId)) {
                return String.valueOf(st != null ? st.getWins() : 0);
            }
            if (lower.equals("losses_" + gId)) {
                return String.valueOf(st != null ? st.getLosses() : 0);
            }
            if (lower.equals("won_money_" + gId)) {
                return String.valueOf(st != null ? st.getWonMoney() : 0);
            }
            if (lower.equals("lost_money_" + gId)) {
                return String.valueOf(st != null ? st.getLostMoney() : 0);
            }
            if (lower.equals("winrate_" + gId)) {
                return String.format("%.1f%%", st != null ? st.getWinrate() : 0.0);
            }
        }

        return null;
    }
}
