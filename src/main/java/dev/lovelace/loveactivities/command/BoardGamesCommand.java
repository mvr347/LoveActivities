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

        // Print Rich Interactive MiniMessage Catalog
        player.sendMessage(TextUtil.parse("<gradient:#FF5E62:#FF9966><bold>════════════════ [НАСТОЛЬНЫЕ ИГРЫ] ════════════════</bold></gradient>"));
        player.sendMessage(TextUtil.parse("<gray>Выберите мини-игру для вызова игрока или изучения правил:</gray>\n"));

        // 1. Durak
        player.sendMessage(TextUtil.parse(
                "<yellow><bold>1. 🃏 Дурак (подкидной)</bold></yellow> <dark_gray>—</dark_gray> <gray>Карточная игра на 36 карт</gray>\n" +
                "   <click:suggest_command:'/durak '><hover:show_text:'<green>Нажмите для вызова игрока:\n<white>/durak <ник></white>'><green><bold>[ВЫЗВАТЬ]</bold></green></hover></click>  " +
                "<click:run_command:'/loveactivities tutorial durak'><hover:show_text:'<yellow>Нажмите, чтобы открыть правила игры'><yellow><bold>[ОБУЧЕНИЕ]</bold></yellow></hover></click>"
        ));

        // 2. Texas Hold'em Poker
        player.sendMessage(TextUtil.parse(
                "<yellow><bold>2. ♠ Техасский Холдем (Покер)</bold></yellow> <dark_gray>—</dark_gray> <gray>2 карты на руках + 5 на столе</gray>\n" +
                "   <click:suggest_command:'/poker '><hover:show_text:'<green>Нажмите для вызова игрока:\n<white>/poker <ник></white>'><green><bold>[ВЫЗВАТЬ]</bold></green></hover></click>  " +
                "<click:run_command:'/loveactivities tutorial poker'><hover:show_text:'<yellow>Нажмите, чтобы открыть правила и комбинации'><yellow><bold>[ОБУЧЕНИЕ]</bold></yellow></hover></click>"
        ));

        // 3. War (Пьяница)
        player.sendMessage(TextUtil.parse(
                "<yellow><bold>3. ⚔ Пьяница / Война</bold></yellow> <dark_gray>—</dark_gray> <gray>Быстрая карточная дуэль на старшинство</gray>\n" +
                "   <click:suggest_command:'/war '><hover:show_text:'<green>Нажмите для вызова игрока:\n<white>/war <ник></white>'><green><bold>[ВЫЗВАТЬ]</bold></green></hover></click>  " +
                "<click:run_command:'/loveactivities tutorial war'><hover:show_text:'<yellow>Нажмите, чтобы открыть правила игры'><yellow><bold>[ОБУЧЕНИЕ]</bold></yellow></hover></click>"
        ));

        // 4. Poker on Dice (5D6)
        player.sendMessage(TextUtil.parse(
                "<yellow><bold>4. 🎲 Покер на костях (5D6)</bold></yellow> <dark_gray>—</dark_gray> <gray>5 костей, перебросы и сбор комбинаций</gray>\n" +
                "   <click:suggest_command:'/pokerdice '><hover:show_text:'<green>Нажмите для вызова игрока:\n<white>/pokerdice <ник></white>'><green><bold>[ВЫЗВАТЬ]</bold></green></hover></click>  " +
                "<click:run_command:'/loveactivities tutorial dice_poker'><hover:show_text:'<yellow>Нажмите, чтобы открыть правила и комбинации'><yellow><bold>[ОБУЧЕНИЕ]</bold></yellow></hover></click>"
        ));

        // 5. Classic Dice (2D6)
        player.sendMessage(TextUtil.parse(
                "<yellow><bold>5. 🎲 Кидание костей (2D6)</bold></yellow> <dark_gray>—</dark_gray> <gray>Бросок 2 костей на наибольшую сумму</gray>\n" +
                "   <click:suggest_command:'/classicdice '><hover:show_text:'<green>Нажмите для вызова игрока:\n<white>/classicdice <ник></white>'><green><bold>[ВЫЗВАТЬ]</bold></green></hover></click>  " +
                "<click:run_command:'/loveactivities tutorial dice_classic'><hover:show_text:'<yellow>Нажмите, чтобы открыть правила игры'><yellow><bold>[ОБУЧЕНИЕ]</bold></yellow></hover></click>"
        ));

        // 6. Blackjack (21)
        player.sendMessage(TextUtil.parse(
                "<yellow><bold>6. 🃏 Блэкджек (21)</bold></yellow> <dark_gray>—</dark_gray> <gray>Набор карт до 21, дабл-бет и дилер</gray>\n" +
                "   <click:suggest_command:'/blackjack '><hover:show_text:'<green>Нажмите для вызова игрока:\n<white>/blackjack <ник></white>'><green><bold>[ВЫЗВАТЬ]</bold></green></hover></click>  " +
                "<click:run_command:'/loveactivities tutorial blackjack'><hover:show_text:'<yellow>Нажмите, чтобы открыть правила игры'><yellow><bold>[ОБУЧЕНИЕ]</bold></yellow></hover></click>"
        ));

        // 7. Mini Chess
        player.sendMessage(TextUtil.parse(
                "<yellow><bold>7. ♟ Мини-шахматы (5x5)</bold></yellow> <dark_gray>—</dark_gray> <gray>Компактные тактические шахматы Гарднера</gray>\n" +
                "   <click:suggest_command:'/chess '><hover:show_text:'<green>Нажмите для вызова игрока:\n<white>/chess <ник></white>'><green><bold>[ВЫЗВАТЬ]</bold></green></hover></click>  " +
                "<click:run_command:'/loveactivities tutorial chess'><hover:show_text:'<yellow>Нажмите, чтобы открыть правила игры'><yellow><bold>[ОБУЧЕНИЕ]</bold></yellow></hover></click>"
        ));

        // 8. Gwent
        player.sendMessage(TextUtil.parse(
                "<yellow><bold>8. 🎴 Гвинт (Minecraft Edition)</bold></yellow> <dark_gray>—</dark_gray> <gray>3 боевых ряда, погода, шпионы, медики</gray>\n" +
                "   <click:suggest_command:'/gwent '><hover:show_text:'<green>Нажмите для вызова игрока:\n<white>/gwent <ник></white>'><green><bold>[ВЫЗВАТЬ]</bold></green></hover></click>  " +
                "<click:run_command:'/loveactivities tutorial gwent'><hover:show_text:'<yellow>Нажмите, чтобы открыть правила игры'><yellow><bold>[ОБУЧЕНИЕ]</bold></yellow></hover></click>"
        ));

        // 9. RPS (КНБ)
        player.sendMessage(TextUtil.parse(
                "<yellow><bold>9. ✂ Камень, Ножницы, Бумага (КНБ)</bold></yellow> <dark_gray>—</dark_gray> <gray>Серия дуэлей до 2 побед</gray>\n" +
                "   <click:suggest_command:'/rps '><hover:show_text:'<green>Нажмите для вызова игрока:\n<white>/rps <ник></white>'><green><bold>[ВЫЗВАТЬ]</bold></green></hover></click>  " +
                "<click:run_command:'/loveactivities tutorial rps'><hover:show_text:'<yellow>Нажмите, чтобы открыть правила игры'><yellow><bold>[ОБУЧЕНИЕ]</bold></yellow></hover></click>"
        ));

        player.sendMessage(TextUtil.parse("<gradient:#FF5E62:#FF9966><bold>═══════════════════════════════════════════════════</bold></gradient>"));
        return true;
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
