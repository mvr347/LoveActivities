package dev.lovelace.loveactivities.command;

import dev.lovelace.loveactivities.LoveActivities;
import dev.lovelace.loveactivities.api.GameType;
import dev.lovelace.loveactivities.gui.SettingsGUI;
import dev.lovelace.loveactivities.util.SoundUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PlayerActivityCommand implements CommandExecutor, TabCompleter {

    private final LoveActivities plugin;

    public PlayerActivityCommand(LoveActivities plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command is only available to players.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("settings")) {
            new SettingsGUI(player).open();
            SoundUtil.playClick(player);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "disable", "dnd" -> {
                boolean newState = plugin.getSettingsManager().toggleDnd(player.getUniqueId());
                SoundUtil.playClick(player);
                plugin.getLocaleManager().send(player, newState ? "dnd_enabled" : "dnd_disabled");
            }
            case "accept" -> {
                String reqId = args.length > 1 ? args[1] : null;
                plugin.getRequestManager().acceptRequest(player, reqId);
            }
            case "decline" -> {
                String reqId = args.length > 1 ? args[1] : null;
                plugin.getRequestManager().declineRequest(player, reqId);
            }
            case "cancel" -> {
                String reqId = args.length > 1 ? args[1] : null;
                plugin.getRequestManager().cancelRequest(player, reqId);
            }
            case "tutorial", "guide", "обучение", "rules" -> {
                String gameKey = args.length > 1 ? args[1] : "durak";
                List<List<String>> pages = dev.lovelace.loveactivities.gui.TutorialRegistry.getTutorialPages(gameKey);
                GameType gt = dev.lovelace.loveactivities.gui.TutorialRegistry.getGameTypeByKey(gameKey);
                new dev.lovelace.loveactivities.gui.TutorialGUI(player, gt, pages).open();
                SoundUtil.playClick(player);
            }
            case "games", "настолки", "list" -> {
                player.performCommand("boardgames");
            }
            case "bot", "play" -> {
                GameType type = args.length > 1 ? GameType.fromString(args[1]) : GameType.BLACKJACK;
                if (type == null) type = GameType.BLACKJACK;

                long bet = 0L;
                if (args.length > 2) {
                    try {
                        bet = Math.max(0L, Long.parseLong(args[2]));
                    } catch (NumberFormatException ignored) {}
                }
                plugin.getSessionManager().startNpcSession(player, type, bet);
            }
            default -> {
                new SettingsGUI(player).open();
                SoundUtil.playClick(player);
            }
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            for (String s : List.of("settings", "disable", "accept", "decline", "cancel", "tutorial", "games", "bot")) {
                if (s.startsWith(args[0].toLowerCase())) {
                    list.add(s);
                }
            }
            return list;
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("tutorial") || args[0].equalsIgnoreCase("guide"))) {
            List<String> list = new ArrayList<>();
            for (String g : List.of("durak", "poker", "war", "blackjack", "dice_poker", "dice_classic", "gwent", "rps")) {
                if (g.startsWith(args[1].toLowerCase())) {
                    list.add(g);
                }
            }
            return list;
        } else if (args.length == 2 && args[0].equalsIgnoreCase("bot")) {
            List<String> list = new ArrayList<>();
            for (GameType g : GameType.values()) {
                if (g.name().toLowerCase().startsWith(args[1].toLowerCase())) {
                    list.add(g.name().toLowerCase());
                }
            }
            return list;
        }
        return List.of();
    }
}
