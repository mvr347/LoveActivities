package dev.lovelace.loveactivities.games.blackjack;

import dev.lovelace.loveactivities.gui.AbstractGUI;
import dev.lovelace.loveactivities.manager.SessionManager;
import dev.lovelace.loveactivities.util.CardItemBuilder;
import dev.lovelace.loveactivities.util.ItemBuilder;
import dev.lovelace.loveactivities.util.BetLore;
import dev.lovelace.loveactivities.util.SoundUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class BlackjackGUI extends AbstractGUI {

    private final BlackjackGame game;

    public BlackjackGUI(Player player, BlackjackGame game) {
        super(player, 54, "<gradient:#FF5E62:#FF9966>Блэкджек (21)</gradient>");
        this.game = game;
    }

    @Override
    public void initializeItems() {
        inventory.clear();
        clickActions.clear();

        ItemStack border = ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name(Component.empty()).build();
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, border);
        }

        Player p1 = Bukkit.getPlayer(game.getPlayer1());
        boolean isNpc = game.isNpcMatch();
        Player p2 = isNpc ? null : Bukkit.getPlayer(game.getPlayer2());

        List<BlackjackCard> handP1 = game.getHandP1();
        List<BlackjackCard> handP2 = game.getHandP2();

        int scoreP1 = BlackjackDeck.calculateScore(handP1);
        int scoreP2 = BlackjackDeck.calculateScore(handP2);

        boolean isP1Turn = game.isPlayer1Turn();
        boolean myTurn = (player.getUniqueId().equals(game.getPlayer1()) && isP1Turn) ||
                         (player.getUniqueId().equals(game.getPlayer2()) && !isP1Turn && !game.isP1Finished());

        // Header / Info at Slot 4 with 3-phase AFK timer
        long elapsed = (System.currentTimeMillis() - game.getLastActionTime()) / 1000L;
        long remaining = Math.max(0L, plugin.getConfigManager().getAfkTurnTimeoutSeconds() - elapsed);

        ItemBuilder timerItem;
        if (remaining > 15) {
            timerItem = plugin.getHeadManager().createBuilder("game_icons.blackjack")
                    .name("<gradient:#00C9FF:#92FE9D><bold>Блэкджек (21)</bold></gradient> <dark_gray>•</dark_gray> <green>" + remaining + "с</green>");
        } else if (remaining > 5) {
            timerItem = plugin.getHeadManager().createBuilder("ui.timer_yellow")
                    .name("<gradient:#FF9966:#FF5E62><bold>Блэкджек (21)</bold></gradient> <dark_gray>•</dark_gray> <yellow>⏳ " + remaining + "с</yellow>");
        } else {
            boolean blink = (System.currentTimeMillis() / 500) % 2 == 0;
            String blinkTex = blink ? plugin.getHeadManager().getTexture("ui.cancel") : plugin.getHeadManager().getTexture("ui.surrender");
            timerItem = ItemBuilder.base64Head(blinkTex)
                    .name("<red><bold>⚠ ВРЕМЯ НА ИСХОДЕ: " + remaining + "с ⚠</bold></red>");
        }

        if (game.getBet() > 0) {
            timerItem.lore(BetLore.withRest(game.getBet() * 2,
                    "<gray>Цель — набрать ближе к 21.</gray>",
                    "",
                    isP1Turn ? "<yellow>Сейчас ходит: <white>" + (p1 != null ? p1.getName() : "Игрок 1") + "</white></yellow>" :
                               "<yellow>Сейчас ходит: <white>" + (isNpc ? SessionManager.NPC_NAME : (p2 != null ? p2.getName() : "Игрок 2")) + "</white></yellow>"
            ));
        } else {
            timerItem.lore(
                    "<gray>Режим: <white>Без ставки</white></gray>",
                    "<gray>Цель — набрать ближе к 21.</gray>",
                    "",
                    isP1Turn ? "<yellow>Сейчас ходит: <white>" + (p1 != null ? p1.getName() : "Игрок 1") + "</white></yellow>" :
                               "<yellow>Сейчас ходит: <white>" + (isNpc ? SessionManager.NPC_NAME : (p2 != null ? p2.getName() : "Игрок 2")) + "</white></yellow>"
            );
        }
        setItem(4, timerItem.build());

        // Header Slot 7: Tutorial
        setItem(7, plugin.getHeadManager().createBuilder("ui.tutorial")
                .name("<yellow><bold>Обучение Блэкджеку</bold></yellow>")
                .lore("<gray>Правила игры</gray>")
                .build(), click -> game.openTutorial(player));

        // Header Slot 8: Surrender
        setItem(8, plugin.getHeadManager().createBuilder("ui.surrender")
                .name("<dark_red><bold>Сдаться</bold></dark_red>")
                .lore("<gray>Признать поражение</gray>")
                .build(), click -> game.resign(player));

        // Top Player (P1) Info at Slot 10
        String p1Status = game.isP1Finished() ? (BlackjackDeck.isBust(handP1) ? "<red>ПЕРЕБОР</red>" : "<green>ОСТАНОВИЛСЯ</green>") : (isP1Turn ? "<yellow>ХОДИТ...</yellow>" : "<gray>ЖДЁТ</gray>");
        setItem(10, ItemBuilder.skull().playerHead(game.getPlayer1())
                .name("<gradient:#00C9FF:#92FE9D><bold>" + (p1 != null ? p1.getName() : "Игрок 1") + "</bold></gradient>")
                .lore(
                        "<gray>Очки: <yellow><bold>" + scoreP1 + "</bold></yellow></gray>",
                        "<gray>Статус: " + p1Status + "</gray>"
                )
                .build());

        // P1 Cards (Slots 12, 13, 14, 15, 16)
        int[] p1CardSlots = {12, 13, 14, 15, 16};
        for (int i = 0; i < handP1.size() && i < p1CardSlots.length; i++) {
            BlackjackCard card = handP1.get(i);
            setItem(p1CardSlots[i], CardItemBuilder.build(plugin, card)
                    .lore("<gray>Значение: <yellow>" + card.getValue() + "</yellow></gray>")
                    .build());
        }

        // Bottom Player (P2 or Dealer) Info at Slot 28
        String p2Name = isNpc ? SessionManager.NPC_NAME : (p2 != null ? p2.getName() : "Игрок 2");
        String p2ScoreDisplay = (isNpc && isP1Turn && !game.isP1Finished()) ? "?" : String.valueOf(scoreP2);
        String p2Status = game.isP2Finished() ? (BlackjackDeck.isBust(handP2) ? "<red>ПЕРЕБОР</red>" : "<green>ОСТАНОВИЛСЯ</green>") : (!isP1Turn ? "<yellow>ХОДИТ...</yellow>" : "<gray>ЖДЁТ</gray>");

        ItemBuilder p2HeadBuilder = isNpc ? CardItemBuilder.buildBack(plugin) : ItemBuilder.skull().playerHead(game.getPlayer2());
        setItem(28, p2HeadBuilder
                .name("<gradient:#FF9966:#FF5E62><bold>" + p2Name + "</bold></gradient>")
                .lore(
                        "<gray>Очки: <yellow><bold>" + p2ScoreDisplay + "</bold></yellow></gray>",
                        "<gray>Статус: " + p2Status + "</gray>"
                )
                .build());

        // P2 Cards (Slots 30, 31, 32, 33, 34)
        int[] p2CardSlots = {30, 31, 32, 33, 34};
        for (int i = 0; i < handP2.size() && i < p2CardSlots.length; i++) {
            BlackjackCard card = handP2.get(i);
            // Hide Dealer's 2nd card if P1 hasn't finished yet
            if (isNpc && i == 1 && isP1Turn && !game.isP1Finished()) {
                setItem(p2CardSlots[i], CardItemBuilder.buildBack(plugin)
                        .name("<gray><italic>Скрытая карта дилера</italic></gray>")
                        .build());
            } else {
                setItem(p2CardSlots[i], CardItemBuilder.build(plugin, card)
                        .lore("<gray>Значение: <yellow>" + card.getValue() + "</yellow></gray>")
                        .build());
            }
        }

        // Action Buttons Row (Slots 47, 49, 51)
        // Hit Button
        setItem(47, plugin.getHeadManager().createBuilder("cards.hit")
                .name("<green><bold>ВЗЯТЬ КАРТУ (+1)</bold></green>")
                .lore(
                        "<gray>Взять ещё одну карту из колоды.</gray>",
                        myTurn ? "<green>▶ Нажмите для добора</green>" : "<red>Сейчас не ваш ход</red>"
                )
                .build(), click -> {
            if (game.isPlayerTurn(player)) {
                game.actionHit(player);
            } else {
                SoundUtil.playError(player);
            }
        });

        // Stand Button
        setItem(49, plugin.getHeadManager().createBuilder("cards.stand")
                .name("<yellow><bold>ХВАТИТ (Остановиться)</bold></yellow>")
                .lore(
                        "<gray>Завершить добор и зафиксировать очки.</gray>",
                        myTurn ? "<yellow>▶ Нажмите для фиксации</yellow>" : "<red>Сейчас не ваш ход</red>"
                )
                .build(), click -> {
            if (game.isPlayerTurn(player)) {
                game.actionStand(player);
            } else {
                SoundUtil.playError(player);
            }
        });

        // Double Down Button
        setItem(51, plugin.getHeadManager().createBuilder("cards.double_down")
                .name("<gold><bold>УДВОИТЬ СТАВКУ (x2)</bold></gold>")
                .lore(
                        "<gray>Удвоить текущую ставку, взять</gray>",
                        "<gray>ровно 1 карту и завершить ход.</gray>",
                        myTurn ? "<gold>▶ Нажмите для удвоения</gold>" : "<red>Сейчас не ваш ход</red>"
                )
                .build(), click -> {
            if (game.isPlayerTurn(player)) {
                game.actionDoubleDown(player);
            } else {
                SoundUtil.playError(player);
            }
        });
    }

    @Override
    public void handleClose() {
        if (!isSwitchingInventory()) {
            game.onPlayerClose(player);
        }
    }
}
