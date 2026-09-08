package dev.lovelace.loveactivities.command;

import dev.lovelace.loveactivities.LoveActivities;
import dev.lovelace.loveactivities.api.GameType;
import dev.lovelace.loveactivities.util.SoundUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class GameCommands implements CommandExecutor, TabCompleter {

    private final LoveActivities plugin;

    public GameCommands(LoveActivities plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command is only available to players.");
            return true;
        }

        String cmdName = command.getName().toLowerCase();
        GameType gameType = resolveGameType(cmdName, label);
        if (gameType == null) return false;
        String subMode = resolveSubMode(cmdName, label);

        if (dev.lovelace.loveactivities.integration.VesuvioBridge.isHighRisk(player)) {
            player.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(
                    "<red>[Активности]</red> <gray>Действие заблокировано: у вас зафиксирован высокий риск античита!</gray>"
            ));
            SoundUtil.playError(player);
            return true;
        }

        if (args.length < 1) {
            plugin.getLocaleManager().send(player, "error_target_offline");
            SoundUtil.playError(player);
            return true;
        }

        String targetName = args[0].toLowerCase();
        if (targetName.equals("bot") || targetName.equals("dealer") || targetName.equals("npc") || targetName.equals("ai")) {
            long bet = 0L;
            if (args.length > 1) {
                try {
                    bet = Math.max(0L, Long.parseLong(args[1]));
                } catch (NumberFormatException ignored) {}
            }
            plugin.getSessionManager().startNpcSession(player, gameType, subMode, bet);
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            plugin.getLocaleManager().send(player, "error_target_offline");
            SoundUtil.playError(player);
            return true;
        }

        plugin.getRequestManager().sendRequest(player, target, gameType, subMode);
        return true;
    }

    private GameType resolveGameType(String cmdName, String label) {
        String name = cmdName != null ? cmdName.toLowerCase() : label.toLowerCase();
        return switch (name) {
            case "poker", "покер", "texasholdem", "holdem", "durak", "дурак", "war", "война", "пьяница", "cards", "карты" -> GameType.CARDS;
            case "dice", "кости", "pokerdice", "покеркости", "костипокер", "yahtzee", "classicdice", "киданиекостей", "2d6" -> GameType.DICE;
            case "blackjack", "блэкджек", "21" -> GameType.BLACKJACK;
            case "gwent", "гвинт" -> GameType.GWENT;
            case "rps", "кнб" -> GameType.RPS;
            case "chess", "шахматы", "минишахматы", "minichess" -> GameType.CHESS;
            default -> GameType.fromString(name);
        };
    }

    private String resolveSubMode(String cmdName, String label) {
        String name = cmdName != null ? cmdName.toLowerCase() : label.toLowerCase();
        return switch (name) {
            case "poker", "покер", "texasholdem", "holdem" -> "poker";
            case "war", "война", "пьяница" -> "war";
            case "durak", "дурак", "cards", "карты" -> "durak";
            case "pokerdice", "покеркости", "костипокер", "yahtzee" -> "dice_poker";
            case "classicdice", "киданиекостей", "2d6", "dice", "кости" -> "dice_classic";
            default -> null;
        };
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            String prefix = args[0].toLowerCase();

            if ("bot".startsWith(prefix)) list.add("bot");
            if ("dealer".startsWith(prefix)) list.add("dealer");

            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.getName().equalsIgnoreCase(sender.getName()) && p.getName().toLowerCase().startsWith(prefix)) {
                    list.add(p.getName());
                }
            }
            return list;
        }
        return List.of();
    }
}
