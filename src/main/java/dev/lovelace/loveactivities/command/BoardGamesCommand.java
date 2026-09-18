package dev.lovelace.loveactivities.command;

import dev.lovelace.loveactivities.LoveActivities;
import dev.lovelace.loveactivities.api.GameType;
import dev.lovelace.loveactivities.gui.TutorialGUI;
import dev.lovelace.loveactivities.gui.TutorialRegistry;
import dev.lovelace.loveactivities.util.SoundUtil;
import dev.lovelace.loveactivities.util.TextUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class BoardGamesCommand implements CommandExecutor, TabCompleter {

    private final LoveActivities plugin;

    public BoardGamesCommand(LoveActivities plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command is only available to players.");
            return true;
        }

        if (args.length > 0) {
            // Direct tutorial shortcut: /настолки <game>
            String gameKey = args[0].toLowerCase();
            List<List<String>> pages = TutorialRegistry.getTutorialPages(gameKey);
            GameType gt = TutorialRegistry.getGameTypeByKey(gameKey);
            new TutorialGUI(player, gt, pages).open();
            SoundUtil.playClick(player);
            return true;
        }

        SoundUtil.playClick(player);

        // Игрок без loveactivities.english не может выполнить английские команды (см.
        // plugin.yml) - подставляем в кликабельные подсказки русские имена команд, иначе
        // каталог предлагал бы кнопки, которые такому игроку сервер тут же отклонит.
        boolean english = player.hasPermission("loveactivities.english");
        String tutorialCmd = english ? "loveactivities" : "активности";

        // Print Rich Interactive MiniMessage Catalog
        player.sendMessage(TextUtil.parse("<gradient:#FF5E62:#FF9966><bold>════════════════ [НАСТОЛЬНЫЕ ИГРЫ] ════════════════</bold></gradient>"));
        player.sendMessage(TextUtil.parse("<gray>Выберите мини-игру для вызова игрока или изучения правил:</gray>\n"));

        // 1. Durak
        player.sendMessage(gameLine("1. 🃏 Дурак (подкидной)", "Карточная игра на 36 карт",
                english ? "durak" : "дурак", tutorialCmd, "durak"));

        // 2. Texas Hold'em Poker
        player.sendMessage(gameLine("2. ♠ Техасский Холдем (Покер)", "2 карты на руках + 5 на столе",
                english ? "poker" : "покер", tutorialCmd, "poker"));

        // 3. War (Пьяница)
        player.sendMessage(gameLine("3. ⚔ Пьяница / Война", "Быстрая карточная дуэль на старшинство",
                english ? "war" : "война", tutorialCmd, "war"));

        // 4. Poker on Dice (5D6)
        player.sendMessage(gameLine("4. 🎲 Покер на костях (5D6)", "5 костей, перебросы и сбор комбинаций",
                english ? "pokerdice" : "покеркости", tutorialCmd, "dice_poker"));

        // 5. Classic Dice (2D6)
        player.sendMessage(gameLine("5. 🎲 Кидание костей (2D6)", "Бросок 2 костей на наибольшую сумму",
                english ? "classicdice" : "киданиекостей", tutorialCmd, "dice_classic"));

        // 6. Blackjack (21)
        player.sendMessage(gameLine("6. 🃏 Блэкджек (21)", "Набор карт до 21, дабл-бет и дилер",
                english ? "blackjack" : "блэкджек", tutorialCmd, "blackjack"));

        // 7. Mini Chess
        player.sendMessage(gameLine("7. ♟ Мини-шахматы (5x5)", "Компактные тактические шахматы Гарднера",
                english ? "chess" : "шахматы", tutorialCmd, "chess"));

        // 8. Gwent
        player.sendMessage(gameLine("8. 🎴 Гвинт (Minecraft Edition)", "3 боевых ряда, погода, шпионы, медики",
                english ? "gwent" : "гвинт", tutorialCmd, "gwent"));

        // 9. RPS (КНБ)
        player.sendMessage(gameLine("9. ✂ Камень, Ножницы, Бумага (КНБ)", "Серия дуэлей до 2 побед",
                english ? "rps" : "кнб", tutorialCmd, "rps"));

        player.sendMessage(TextUtil.parse("<gradient:#FF5E62:#FF9966><bold>═══════════════════════════════════════════════════</bold></gradient>"));
        return true;
    }

    private net.kyori.adventure.text.Component gameLine(String title, String desc, String challengeCmd, String tutorialCmd, String tutorialKey) {
        return TextUtil.parse(
                "<yellow><bold>" + title + "</bold></yellow> <dark_gray>—</dark_gray> <gray>" + desc + "</gray>\n" +
                "   <click:suggest_command:'/" + challengeCmd + " '><hover:show_text:'<green>Нажмите для вызова игрока:\n<white>/" + challengeCmd + " <ник></white>'><green><bold>[ВЫЗВАТЬ]</bold></green></hover></click>  " +
                "<click:run_command:'/" + tutorialCmd + " tutorial " + tutorialKey + "'><hover:show_text:'<yellow>Нажмите, чтобы открыть правила игры'><yellow><bold>[ОБУЧЕНИЕ]</bold></yellow></hover></click>"
        );
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            for (String g : List.of("durak", "poker", "war", "blackjack", "dice_poker", "dice_classic", "chess", "gwent", "rps")) {
                if (g.startsWith(args[0].toLowerCase())) {
                    list.add(g);
                }
            }
            return list;
        }
        return List.of();
    }
}
