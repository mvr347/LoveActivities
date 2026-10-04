package dev.lovelace.loveactivities.games.rps;

import dev.lovelace.loveactivities.gui.AbstractGUI;
import dev.lovelace.loveactivities.util.ItemBuilder;
import dev.lovelace.loveactivities.util.BetLore;
import dev.lovelace.loveactivities.util.SoundUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class RPSGUI extends AbstractGUI {

    private final RPSGame game;

    public RPSGUI(Player player, RPSGame game) {
        super(player, 45, "<gradient:#FF5E62:#FF9966>Камень-Ножницы-Бумага</gradient>");
        this.game = game;
    }

    @Override
    public void initializeItems() {
        inventory.clear();
        clickActions.clear();

        ItemStack border = ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name(Component.empty()).build();
        for (int i = 0; i < 45; i++) {
            inventory.setItem(i, border);
        }

        Player p1 = Bukkit.getPlayer(game.getPlayer1());
        Player p2 = Bukkit.getPlayer(game.getPlayer2());

        boolean isP1 = player.getUniqueId().equals(game.getPlayer1());
        RPSChoice myChoice = isP1 ? game.getChoiceP1() : game.getChoiceP2();

        // Round & Bank info with 3-phase AFK timer at Slot 4
        long elapsed = (System.currentTimeMillis() - game.getLastActionTime()) / 1000L;
        long remaining = Math.max(0L, plugin.getConfigManager().getAfkTurnTimeoutSeconds() - elapsed);

        ItemBuilder timerItem;
        if (remaining > 15) {
            timerItem = plugin.getHeadManager().createBuilder("game_icons.rps")
                    .name("<gradient:#00C9FF:#92FE9D><bold>Раунд " + game.getCurrentRound() + " (До " + game.getTargetScore() + " побед)</bold></gradient> <dark_gray>•</dark_gray> <green>" + remaining + "с</green>");
        } else if (remaining > 5) {
            timerItem = plugin.getHeadManager().createBuilder("ui.timer_yellow")
                    .name("<gradient:#FF9966:#FF5E62><bold>Раунд " + game.getCurrentRound() + " (До " + game.getTargetScore() + " побед)</bold></gradient> <dark_gray>•</dark_gray> <yellow>⏳ " + remaining + "с</yellow>");
        } else {
            boolean blink = (System.currentTimeMillis() / 500) % 2 == 0;
            String blinkTex = blink ? plugin.getHeadManager().getTexture("ui.cancel") : plugin.getHeadManager().getTexture("ui.surrender");
            timerItem = ItemBuilder.base64Head(blinkTex)
                    .name("<red><bold>⚠ ВРЕМЯ НА ИСХОДЕ: " + remaining + "с ⚠</bold></red>");
        }

        if (game.getBet() > 0) {
            timerItem.lore(BetLore.withRest(game.getBet() * 2,
                    "<gray>Счёт: <yellow>" + (p1 != null ? p1.getName() : "P1") + " [" + game.getScoreP1() + "]</yellow> : <yellow>[" + game.getScoreP2() + "] " + (p2 != null ? p2.getName() : "P2") + "</yellow></gray>"
            ));
        } else {
            timerItem.lore(
                    "<gray>Режим: <white>Без ставки</white></gray>",
                    "<gray>Счёт: <yellow>" + (p1 != null ? p1.getName() : "P1") + " [" + game.getScoreP1() + "]</yellow> : <yellow>[" + game.getScoreP2() + "] " + (p2 != null ? p2.getName() : "P2") + "</yellow></gray>"
            );
        }
        setItem(4, timerItem.build());

        // Header Slot 8: Surrender Button
        setItem(8, plugin.getHeadManager().createBuilder("ui.surrender")
                .name("<dark_red><bold>Сдаться</bold></dark_red>")
                .lore("<gray>Признать поражение в матче</gray>")
                .build(), click -> game.resign(player));

        // Player 1 Info at Slot 10
        String p1Status = game.getChoiceP1() != null ? "<green>ВЫБРАНО ✔</green>" : "<yellow>ВЫБИРАЕТ...</yellow>";
        setItem(10, ItemBuilder.skull().playerHead(game.getPlayer1())
                .name("<gradient:#00C9FF:#92FE9D><bold>" + (p1 != null ? p1.getName() : "Игрок 1") + "</bold></gradient>")
                .lore(
                        "<gray>Побед в серии: <yellow><bold>" + game.getScoreP1() + "</bold></yellow></gray>",
                        "<gray>Статус: " + p1Status + "</gray>"
                )
                .build());

        // Player 2 Info at Slot 16
        String p2Status = game.getChoiceP2() != null ? "<green>ВЫБРАНО ✔</green>" : "<yellow>ВЫБИРАЕТ...</yellow>";
        setItem(16, ItemBuilder.skull().playerHead(game.getPlayer2())
                .name("<gradient:#FF9966:#FF5E62><bold>" + (p2 != null ? p2.getName() : "Игрок 2") + "</bold></gradient>")
                .lore(
                        "<gray>Побед в серии: <yellow><bold>" + game.getScoreP2() + "</bold></yellow></gray>",
                        "<gray>Статус: " + p2Status + "</gray>"
                )
                .build());

        // Countdown / Reveal Display in Center (Slot 22 or Row 2)
        if (game.isCountingDown()) {
            String countHead = switch (game.getCountdown()) {
                case 3 -> plugin.getHeadManager().getTexture("ui.countdown_3");
                case 2 -> plugin.getHeadManager().getTexture("ui.countdown_2");
                case 1 -> plugin.getHeadManager().getTexture("ui.countdown_1");
                default -> plugin.getHeadManager().getTexture("ui.coin_stack");
            };
            setItem(22, ItemBuilder.base64Head(countHead)
                    .name("<gold><bold>Вскрытие через: " + game.getCountdown() + "...</bold></gold>")
                    .build());
        } else if (game.isRevealing()) {
            String p1ChoiceTex = plugin.getHeadManager().getTexture(game.getChoiceP1().getTextureKey());
            String p2ChoiceTex = plugin.getHeadManager().getTexture(game.getChoiceP2().getTextureKey());

            setItem(20, ItemBuilder.base64Head(p1ChoiceTex)
                    .name("<yellow><bold>" + (p1 != null ? p1.getName() : "P1") + ": " + game.getChoiceP1().getNameRu() + "</bold></yellow>")
                    .build());

            setItem(22, plugin.getHeadManager().createBuilder("game_icons.rps")
                    .name("<gold><bold>" + game.getLastRoundResultText() + "</bold></gold>")
                    .build());

            setItem(24, ItemBuilder.base64Head(p2ChoiceTex)
                    .name("<yellow><bold>" + (p2 != null ? p2.getName() : "P2") + ": " + game.getChoiceP2().getNameRu() + "</bold></yellow>")
                    .build());
        } else {
            // Rock
            boolean isRock = myChoice == RPSChoice.ROCK;
            setItem(20, plugin.getHeadManager().createBuilder("rps.rock")
                    .name("<white><bold>КАМЕНЬ</bold></white>")
                    .lore(
                            "<gray>Побеждает Ножницы.</gray>",
                            isRock ? "<green><bold>✔ ВАШ ВЫБОР</bold></green>" : "<yellow>▶ Нажмите для выбора</yellow>"
                    )
                    .build(), click -> game.makeChoice(player, RPSChoice.ROCK));

            // Paper
            boolean isPaper = myChoice == RPSChoice.PAPER;
            setItem(22, plugin.getHeadManager().createBuilder("rps.paper")
                    .name("<white><bold>БУМАГА</bold></white>")
                    .lore(
                            "<gray>Побеждает Камень.</gray>",
                            isPaper ? "<green><bold>✔ ВАШ ВЫБОР</bold></green>" : "<yellow>▶ Нажмите для выбора</yellow>"
                    )
                    .build(), click -> game.makeChoice(player, RPSChoice.PAPER));

            // Scissors
            boolean isScissors = myChoice == RPSChoice.SCISSORS;
            setItem(24, plugin.getHeadManager().createBuilder("rps.scissors")
                    .name("<white><bold>НОЖНИЦЫ</bold></white>")
                    .lore(
                            "<gray>Побеждают Бумагу.</gray>",
                            isScissors ? "<green><bold>✔ ВАШ ВЫБОР</bold></green>" : "<yellow>▶ Нажмите для выбора</yellow>"
                    )
                    .build(), click -> game.makeChoice(player, RPSChoice.SCISSORS));
        }
    }

    @Override
    public void handleClose() {
        if (!isSwitchingInventory()) {
            game.onPlayerClose(player);
        }
    }
}
