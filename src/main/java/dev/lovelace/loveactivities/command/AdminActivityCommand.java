package dev.lovelace.loveactivities.command;

import dev.lovelace.loveactivities.LoveActivities;
import dev.lovelace.loveactivities.api.GameSession;
import dev.lovelace.loveactivities.api.GameType;
import dev.lovelace.loveactivities.database.PlayerStats;
import dev.lovelace.loveactivities.manager.NpcActivityConfig;
import dev.lovelace.loveactivities.util.SoundUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AdminActivityCommand implements CommandExecutor, TabCompleter {

    private final LoveActivities plugin;

    public AdminActivityCommand(LoveActivities plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("loveactivities.admin")) {
            sender.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sender.sendMessage("§6=== LoveActivities Admin ===");
            sender.sendMessage("§e/laadmin reload §7— Перезагрузка конфигураций, сообщений и голов");
            sender.sendMessage("§e/laadmin endgame <player> §7— Принудительно завершить игру игрока");
            sender.sendMessage("§e/laadmin stats <player> §7— Просмотреть статистику игрока");
            sender.sendMessage("§e/laadmin npc bind <game> [bet] [name] §7— Привязать игру к NPC (смотрите на сущность)");
            sender.sendMessage("§e/laadmin npc unbind §7— Отвязать игру от NPC (смотрите на сущность)");
            sender.sendMessage("§e/laadmin npc list §7— Список всех привязанных NPC");
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "reload" -> {
                plugin.reloadAll();
                plugin.getLocaleManager().send(sender, "admin_reload");
            }
            case "endgame" -> {
                if (args.length < 2) {
                    sender.sendMessage("§cИспользование: /laadmin endgame <player>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage("§cИгрок не найден.");
                    return true;
                }
                GameSession session = plugin.getSessionManager().getSession(target.getUniqueId());
                if (session == null) {
                    plugin.getLocaleManager().send(sender, "admin_player_not_in_game", Map.of("player", target.getName()));
                    return true;
                }
                plugin.getSessionManager().handleCancelAndRefund(session, "admin_game_ended");
                plugin.getLocaleManager().send(sender, "admin_game_ended", Map.of("player", target.getName()));
            }
            case "stats" -> {
                Player target = (args.length > 1) ? Bukkit.getPlayer(args[1]) : (sender instanceof Player p ? p : null);
                if (target == null) {
                    sender.sendMessage("§cИгрок не найден.");
                    return true;
                }
                Map<GameType, PlayerStats> statsMap = plugin.getStatsManager().getPlayerStats(target.getUniqueId());
                plugin.getLocaleManager().send(sender, "admin_stats_header", Map.of("player", target.getName()));
                for (GameType game : GameType.values()) {
                    PlayerStats st = statsMap.get(game);
                    plugin.getLocaleManager().send(sender, "admin_stats_line", Map.of(
                            "game", game.getNameRu(),
                            "wins", String.valueOf(st != null ? st.getWins() : 0),
                            "losses", String.valueOf(st != null ? st.getLosses() : 0),
                            "won", String.valueOf(st != null ? st.getWonMoney() : 0),
                            "lost", String.valueOf(st != null ? st.getLostMoney() : 0)
                    ));
                }
            }
            case "npc" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cЭта команда доступна только игрокам.");
                    return true;
                }

                if (args.length < 2) {
                    sender.sendMessage("§cИспользование: /laadmin npc <bind|unbind|list> ...");
                    return true;
                }

                String npcSub = args[1].toLowerCase();
                switch (npcSub) {
                    case "bind" -> {
                        if (args.length < 3) {
                            sender.sendMessage("§cИспользование: /laadmin npc bind <game> [bet] [max_bet] [plays_bets: true|false] [name]");
                            return true;
                        }

                        GameType gameType = GameType.fromString(args[2]);
                        if (gameType == null) {
                            sender.sendMessage("§cНеизвестная игра: " + args[2] + ". Доступны: blackjack, cards, dice, rps, gwent, chess");
                            return true;
                        }

                        long bet = 0L;
                        long maxBet = 0L;
                        boolean playsBets = true;
                        int nextIndex = 3;

                        // Arg 3: bet (number)
                        if (args.length > nextIndex) {
                            try {
                                bet = Math.max(0L, Long.parseLong(args[nextIndex]));
                                nextIndex++;
                            } catch (NumberFormatException ignored) {}
                        }

                        // Arg 4: max_bet (number) or plays_bets (boolean)
                        if (args.length > nextIndex) {
                            if (args[nextIndex].equalsIgnoreCase("true") || args[nextIndex].equalsIgnoreCase("false")) {
                                playsBets = Boolean.parseBoolean(args[nextIndex]);
                                nextIndex++;
                            } else {
                                try {
                                    maxBet = Math.max(0L, Long.parseLong(args[nextIndex]));
                                    nextIndex++;
                                } catch (NumberFormatException ignored) {}
                            }
                        }

                        // Arg 5: plays_bets (boolean) if not already consumed
                        if (args.length > nextIndex) {
                            if (args[nextIndex].equalsIgnoreCase("true") || args[nextIndex].equalsIgnoreCase("false")) {
                                playsBets = Boolean.parseBoolean(args[nextIndex]);
                                nextIndex++;
                            }
                        }

                        // Remaining args: custom name
                        String customName = null;
                        if (args.length > nextIndex) {
                            StringBuilder sb = new StringBuilder();
                            for (int i = nextIndex; i < args.length; i++) {
                                if (i > nextIndex) sb.append(" ");
                                sb.append(args[i]);
                            }
                            customName = sb.toString();
                        }

                        Entity target = plugin.getNpcManager().findTargetEntity(player);
                        if (target == null) {
                            sender.sendMessage("§cВы не смотрите на сущность / NPC (радиус 6 блоков)!");
                            SoundUtil.playError(player);
                            return true;
                        }

                        plugin.getNpcManager().bindNpc(target, gameType, bet, maxBet, playsBets, customName);
                        SoundUtil.playSuccess(player);
                        sender.sendMessage("§a✔ NPC §e" + (customName != null ? customName : target.getName()) +
                                "§a успешно привязан к игре §6" + gameType.getNameRu() +
                                "§a (Ставка: §e" + bet +
                                "§a, Макс: §e" + (maxBet > 0 ? maxBet : "∞") +
                                "§a, На деньги: §b" + (playsBets ? "Да" : "Нет") + "§a)!");
                    }
                    case "unbind" -> {
                        Entity target = plugin.getNpcManager().findTargetEntity(player);
                        if (target == null) {
                            sender.sendMessage("§cВы не смотрите на сущность / NPC (радиус 6 блоков)!");
                            SoundUtil.playError(player);
                            return true;
                        }

                        if (plugin.getNpcManager().unbindNpc(target.getUniqueId())) {
                            SoundUtil.playSuccess(player);
                            sender.sendMessage("§a✔ NPC §e" + target.getName() + "§a успешно отвязан от LoveActivities.");
                        } else {
                            sender.sendMessage("§cЭтот NPC не был привязан к активности.");
                            SoundUtil.playError(player);
                        }
                    }
                    case "list" -> {
                        Map<UUID, NpcActivityConfig> npcs = plugin.getNpcManager().getAllNpcs();
                        if (npcs.isEmpty()) {
                            sender.sendMessage("§7Нет привязанных NPC.");
                            return true;
                        }
                        sender.sendMessage("§6=== Список привязанных NPC (" + npcs.size() + ") ===");
                        for (NpcActivityConfig n : npcs.values()) {
                            String mood = n.isRefusing() ?
                                    "§cОтказывается (~" + Math.max(1, (n.getRefusedUntil() - System.currentTimeMillis()) / 60000L) + " мин)§7" :
                                    "§aГотов играть§7";
                            sender.sendMessage("§7• §e" + n.getCustomName() +
                                    " §7| Игра: §6" + n.getGameType().getNameRu() +
                                    " §7| Ставка: §e" + n.getDefaultBet() +
                                    " §7| Макс: §e" + (n.getMaxBet() > 0 ? n.getMaxBet() : "∞") +
                                    " §7| Ставки: §b" + (n.isPlaysBets() ? "Да" : "Нет") +
                                    " §7| Настроение: " + mood);
                        }
                    }
                    default -> sender.sendMessage("§cИспользование: /laadmin npc <bind|unbind|list>");
                }
            }
            default -> sender.sendMessage("§cНеизвестная подкоманда. Используйте /laadmin help");
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            for (String s : List.of("reload", "endgame", "stats", "npc")) {
                if (s.startsWith(args[0].toLowerCase())) {
                    list.add(s);
                }
            }
            return list;
        } else if (args.length == 2 && args[0].equalsIgnoreCase("npc")) {
            List<String> list = new ArrayList<>();
            for (String s : List.of("bind", "unbind", "list")) {
                if (s.startsWith(args[1].toLowerCase())) {
                    list.add(s);
                }
            }
            return list;
        } else if (args.length == 3 && args[0].equalsIgnoreCase("npc") && args[1].equalsIgnoreCase("bind")) {
            List<String> list = new ArrayList<>();
            for (GameType g : GameType.values()) {
                if (g.name().toLowerCase().startsWith(args[2].toLowerCase())) {
                    list.add(g.name().toLowerCase());
                }
            }
            return list;
        } else if (args.length == 4 && args[0].equalsIgnoreCase("npc") && args[1].equalsIgnoreCase("bind")) {
            return List.of("0", "10", "50", "100", "500");
        } else if (args.length == 5 && args[0].equalsIgnoreCase("npc") && args[1].equalsIgnoreCase("bind")) {
            return List.of("0", "100", "500", "1000", "5000", "true", "false");
        } else if (args.length == 6 && args[0].equalsIgnoreCase("npc") && args[1].equalsIgnoreCase("bind")) {
            return List.of("true", "false");
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("endgame") || args[0].equalsIgnoreCase("stats"))) {
            List<String> list = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                    list.add(p.getName());
                }
            }
            return list;
        }
        return List.of();
    }
}
