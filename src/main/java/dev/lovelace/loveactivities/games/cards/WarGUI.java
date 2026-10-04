package dev.lovelace.loveactivities.games.cards;

import dev.lovelace.loveactivities.gui.AbstractGUI;
import dev.lovelace.loveactivities.util.CardItemBuilder;
import dev.lovelace.loveactivities.util.ItemBuilder;
import dev.lovelace.loveactivities.util.BetLore;
import dev.lovelace.loveactivities.util.SoundUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class WarGUI extends AbstractGUI {

    private final CardsGame game;

    public WarGUI(Player player, CardsGame game) {
        super(player, 45, "<gradient:#FF5E62:#FF9966>Карточная война (War)</gradient>");
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

        // Header info at Slot 4 with 3-phase AFK timer
        long elapsed = (System.currentTimeMillis() - game.getLastActionTime()) / 1000L;
        long remaining = Math.max(0L, plugin.getConfigManager().getAfkTurnTimeoutSeconds() - elapsed);

        ItemBuilder timerItem;
        if (remaining > 15) {
            timerItem = plugin.getHeadManager().createBuilder("game_icons.cards")
                    .name("<gradient:#00C9FF:#92FE9D><bold>Раунд " + game.getWarRound() + " / 18</bold></gradient> <dark_gray>•</dark_gray> <green>" + remaining + "с</green>");
        } else if (remaining > 5) {
            timerItem = plugin.getHeadManager().createBuilder("ui.timer_yellow")
                    .name("<gradient:#FF9966:#FF5E62><bold>Раунд " + game.getWarRound() + " / 18</bold></gradient> <dark_gray>•</dark_gray> <yellow>⏳ " + remaining + "с</yellow>");
        } else {
            boolean blink = (System.currentTimeMillis() / 500) % 2 == 0;
            String blinkTex = blink ? plugin.getHeadManager().getTexture("ui.cancel") : plugin.getHeadManager().getTexture("ui.surrender");
            timerItem = ItemBuilder.base64Head(blinkTex)
                    .name("<red><bold>⚠ ВРЕМЯ НА ИСХОДЕ: " + remaining + "с ⚠</bold></red>");
        }

        if (game.getBet() > 0) {
            timerItem.lore(BetLore.withRest(game.getBet() * 2,
                    "<gray>Счёт: <yellow>" + (p1 != null ? p1.getName() : "P1") + " [" + game.getWarWonP1() + "]</yellow> : <yellow>[" + game.getWarWonP2() + "] " + (p2 != null ? p2.getName() : "P2") + "</yellow></gray>"
            ));
        } else {
            timerItem.lore(
                    "<gray>Режим: <white>Без ставки</white></gray>",
                    "<gray>Счёт: <yellow>" + (p1 != null ? p1.getName() : "P1") + " [" + game.getWarWonP1() + "]</yellow> : <yellow>[" + game.getWarWonP2() + "] " + (p2 != null ? p2.getName() : "P2") + "</yellow></gray>"
            );
        }
        setItem(4, timerItem.build());

        // Header Slot 7: Tutorial
        setItem(7, plugin.getHeadManager().createBuilder("ui.tutorial")
                .name("<yellow><bold>Обучение игре Война</bold></yellow>")
                .lore("<gray>Правила игры</gray>")
                .build(), click -> game.openTutorial(player));

        // Header Slot 8: Resign
        setItem(8, plugin.getHeadManager().createBuilder("ui.surrender")
                .name("<dark_red><bold>Сдаться</bold></dark_red>")
                .lore("<gray>Признать поражение</gray>")
                .build(), click -> game.resign(player));

        // Player 1 Info at Slot 10
        setItem(10, ItemBuilder.skull().playerHead(game.getPlayer1())
                .name("<gradient:#00C9FF:#92FE9D><bold>" + (p1 != null ? p1.getName() : "Игрок 1") + "</bold></gradient>")
                .lore(
                        "<gray>Выиграно карт: <gold><bold>" + game.getWarWonP1() + "</bold></gold></gray>",
                        "<gray>Карт в колоде: <yellow>" + game.getHandP1().size() + "</yellow></gray>"
                )
                .build());

        // Player 2 Info at Slot 16
        setItem(16, ItemBuilder.skull().playerHead(game.getPlayer2())
                .name("<gradient:#FF9966:#FF5E62><bold>" + (p2 != null ? p2.getName() : "Игрок 2") + "</bold></gradient>")
                .lore(
                        "<gray>Выиграно карт: <gold><bold>" + game.getWarWonP2() + "</bold></gold></gray>",
                        "<gray>Карт в колоде: <yellow>" + game.getHandP2().size() + "</yellow></gray>"
                )
                .build());

        // Revealed Cards at Slots 20 and 24
        PlayingCard card1 = game.getWarCardP1();
        PlayingCard card2 = game.getWarCardP2();

        if (card1 != null) {
            setItem(20, CardItemBuilder.build(plugin, card1)
                    .name("<yellow>" + (p1 != null ? p1.getName() : "P1") + ": " + card1.getFormattedName() + "</yellow>")
                    .lore("<gray>Сила: <gold>" + card1.getValue() + "</gold></gray>")
                    .build());
        } else {
            setItem(20, CardItemBuilder.buildBack(plugin)
                    .name("<gray>Рубашка карты</gray>")
                    .build());
        }

        if (card2 != null) {
            setItem(24, CardItemBuilder.build(plugin, card2)
                    .name("<yellow>" + (p2 != null ? p2.getName() : "P2") + ": " + card2.getFormattedName() + "</yellow>")
                    .lore("<gray>Сила: <gold>" + card2.getValue() + "</gold></gray>")
                    .build());
        } else {
            setItem(24, CardItemBuilder.buildBack(plugin)
                    .name("<gray>Рубашка карты</gray>")
                    .build());
        }

        // Center Clash Banner (Slot 22)
        setItem(22, plugin.getHeadManager().createBuilder("game_icons.cards")
                .name("<gold><bold>" + (game.getWarResultText().isEmpty() ? "VS" : game.getWarResultText()) + "</bold></gold>")
                .build());

        // Flip Card Button (Slot 31)
        setItem(31, plugin.getHeadManager().createBuilder("cards.hit")
                .name("<green><bold>ВСКРЫТЬ КАРТУ</bold></green>")
                .lore("<gray>Вскрыть верхнюю карту из своей колоды.</gray>")
                .build(), click -> game.actionWarFlip(player));
    }

    @Override
    public void handleClose() {
        if (!isSwitchingInventory()) {
            game.onPlayerClose(player);
        }
    }
}
