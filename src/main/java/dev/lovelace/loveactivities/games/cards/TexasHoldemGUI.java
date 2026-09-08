package dev.lovelace.loveactivities.games.cards;

import dev.lovelace.loveactivities.gui.AbstractGUI;
import dev.lovelace.loveactivities.manager.SessionManager;
import dev.lovelace.loveactivities.util.CardItemBuilder;
import dev.lovelace.loveactivities.util.CurrencyUtil;
import dev.lovelace.loveactivities.util.ItemBuilder;
import dev.lovelace.loveactivities.util.SoundUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class TexasHoldemGUI extends AbstractGUI {

    private final CardsGame game;

    public TexasHoldemGUI(Player player, CardsGame game) {
        super(player, 54, "<gradient:#FF5E62:#FF9966><bold>Техасский Холдем (Покер)</bold></gradient>");
        this.game = game;
    }

    @Override
    public void initializeItems() {
        inventory.clear();
        clickActions.clear();

        ItemStack glass = ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name(Component.empty()).build();

        // Header (Row 0) & Row 1 (Header 2nd row per gui-gen-5)
        for (int i = 0; i <= 17; i++) {
            inventory.setItem(i, glass);
        }

        // Row 4 separator (36-44)
        for (int i = 36; i <= 44; i++) {
            inventory.setItem(i, glass);
        }

        // Row 5 background (45-53)
        for (int i = 45; i <= 53; i++) {
            inventory.setItem(i, glass);
        }

        boolean isP1 = player.getUniqueId().equals(game.getPlayer1());
        boolean isNpc = game.isNpcMatch();
        Player opp = isNpc ? null : Bukkit.getPlayer(game.getOpponent(player.getUniqueId()));

        boolean myTurn = game.isPokerPlayerTurn(player);
        List<PlayingCard> myHole = isP1 ? game.getPokerHoleP1() : game.getPokerHoleP2();
        List<PlayingCard> oppHole = isP1 ? game.getPokerHoleP2() : game.getPokerHoleP1();
        List<PlayingCard> community = game.getPokerCommunityCards();

        // Header Slot 0: Opponent Info
        String oppName = isNpc ? SessionManager.NPC_NAME : (opp != null ? opp.getName() : "Соперник");
        ItemBuilder oppHead = isNpc ? plugin.getHeadManager().createBuilder("cards.card_back") : ItemBuilder.skull().playerHead(opp != null ? opp.getUniqueId() : null);
        setItem(0, oppHead
                .name("<gradient:#FF9966:#FF5E62><bold>" + oppName + "</bold></gradient>")
                .lore(
                        "<gray>Карты на руках: <aqua>2 шт.</aqua></gray>",
                        "<gray>Статус: " + (!myTurn ? "<green>Думает над ходом...</green>" : "<yellow>Ждет вашего решения</yellow>") + "</gray>"
                )
                .build());

        // Header Slot 4: Center Match Info with 3-phase AFK timer & ItemsAdder coin images
        long elapsed = (System.currentTimeMillis() - game.getLastActionTime()) / 1000L;
        long remaining = Math.max(0L, plugin.getConfigManager().getAfkTurnTimeoutSeconds() - elapsed);
        boolean hasPot = game.getPokerPot() > 0;
        String potTitlePrefix = hasPot
                ? "Банк: " + CurrencyUtil.formatCoinsShort(game.getPokerPot())
                : "Таймер хода";

        ItemBuilder timerItem;
        if (remaining > 15) {
            timerItem = plugin.getHeadManager().createBuilder("game_icons.cards")
                    .name("<gradient:#00C9FF:#92FE9D><bold>" + potTitlePrefix + "</bold></gradient> <dark_gray>•</dark_gray> <green>" + remaining + "с</green>");
        } else if (remaining > 5) {
            timerItem = plugin.getHeadManager().createBuilder("ui.timer_yellow")
                    .name("<gradient:#FF9966:#FF5E62><bold>" + potTitlePrefix + "</bold></gradient> <dark_gray>•</dark_gray> <yellow>⏳ " + remaining + "с</yellow>");
        } else {
            boolean blink = (System.currentTimeMillis() / 500) % 2 == 0;
            String blinkTex = blink ? plugin.getHeadManager().getTexture("ui.cancel") : plugin.getHeadManager().getTexture("ui.surrender");
            timerItem = ItemBuilder.base64Head(blinkTex)
                    .name("<red><bold>⚠ ВРЕМЯ НА ИСХОДЕ: " + remaining + "с ⚠</bold></red>");
        }

        List<String> loreList = new ArrayList<>();
        loreList.add("<gray>Этап: <yellow><bold>" + game.getPokerStageName() + "</bold></yellow></gray>");
        if (hasPot) {
            loreList.add("<gray>Банк: <gold>" + CurrencyUtil.formatCoinsWords(game.getPokerPot()) + "</gold></gray>");
        } else {
            loreList.add("<gray>Режим: <green>Без ставок</green></gray>");
        }
        loreList.add("");
        loreList.add(myTurn ? "<green>▶ Сейчас ваш ход!</green>" : "<red>⏳ Ход соперника...</red>");
        timerItem.lore(loreList.toArray(new String[0]));
        setItem(4, timerItem.build());

        // Header Slot 7: Combinations
        setItem(7, plugin.getHeadManager().createBuilder("ui.tutorial")
                .name("<yellow><bold>Комбинации Покера</bold></yellow>")
                .lore(
                        "<gray>Таблица старшинства всех комбинаций карт</gray>",
                        "<green>▶ Нажмите для просмотра</green>"
                )
                .build(), click -> game.openTutorial(player));

        // Header Slot 8: Gray glass (Fold is in action bar slot 53)
        setItem(8, glass);

        // --- Row 2: Opponent Hole Cards (Slots 21, 23) ---
        boolean isShowdown = game.isPokerShowdown();
        if (isShowdown && oppHole.size() >= 2) {
            setItem(21, CardItemBuilder.build(plugin, oppHole.get(0)).build());
            setItem(23, CardItemBuilder.build(plugin, oppHole.get(1)).build());
        } else {
            setItem(21, CardItemBuilder.buildBack(plugin).name("<gray>Закрытая карта соперника</gray>").build());
            setItem(23, CardItemBuilder.buildBack(plugin).name("<gray>Закрытая карта соперника</gray>").build());
        }

        // --- Row 3: 5 Community Cards (Slots 29, 30, 31, 32, 33) ---
        int[] commSlots = {29, 30, 31, 32, 33};
        for (int i = 0; i < 5; i++) {
            if (i < community.size()) {
                PlayingCard card = community.get(i);
                setItem(commSlots[i], CardItemBuilder.build(plugin, card)
                        .lore("<gray>Общая карта на столе</gray>")
                        .build());
            } else {
                setItem(commSlots[i], CardItemBuilder.buildBack(plugin)
                        .name("<dark_gray>Карта ещё не открыта</dark_gray>")
                        .build());
            }
        }

        // Evaluate Current Player Combination
        List<PlayingCard> allMyCards = new ArrayList<>(myHole);
        allMyCards.addAll(community);
        PokerHandEvaluator.PokerScore myScore = PokerHandEvaluator.evaluate7Cards(allMyCards);

        // Player Score Info at Slot 47
        setItem(47, plugin.getHeadManager().createBuilder("game_icons.cards")
                .name("<gradient:#FFE000:#799F0C><bold>Ваша комбинация:</bold></gradient>")
                .lore(
                        "<yellow><bold>" + myScore.description() + "</bold></yellow>",
                        "<gray>Ранг: <gold>" + myScore.rank().getNameRu() + "</gold></gray>"
                )
                .build());

        // --- Row 5: Player Hole Cards (Slots 48, 49) ---
        if (myHole.size() >= 2) {
            setItem(48, CardItemBuilder.build(plugin, myHole.get(0)).lore("<green>Ваша карманная карта #1</green>").build());
            setItem(49, CardItemBuilder.build(plugin, myHole.get(1)).lore("<green>Ваша карманная карта #2</green>").build());
        }

        // --- Action Buttons ---
        // Slot 51: Check / Call Button
        setItem(51, plugin.getHeadManager().createBuilder("ui.confirm")
                .name("<green><bold>ЧЕК / ПРОПУСТИТЬ ХОД</bold></green>")
                .lore(
                        "<gray>Остаться в игре без дополнительной ставки.</gray>",
                        myTurn ? "<green>▶ Нажмите для действия</green>" : "<red>Сейчас не ваш ход</red>"
                )
                .build(), click -> {
            if (myTurn) {
                game.actionPokerCheck(player);
            } else {
                SoundUtil.playError(player);
            }
        });

        // Slot 52: Bet / Raise Button
        if (game.getPokerPot() > 0) {
            setItem(52, plugin.getHeadManager().createBuilder("ui.coin_stack")
                    .name("<gold><bold>ПОВЫСИТЬ СТАВКУ (+10%)</bold></gold>")
                    .lore(
                            "<gray>Увеличить общий банк на 10%.</gray>",
                            myTurn ? "<gold>▶ Нажмите для повышения</gold>" : "<red>Сейчас не ваш ход</red>"
                    )
                    .build(), click -> {
                if (myTurn) {
                    game.actionPokerRaise(player);
                } else {
                    SoundUtil.playError(player);
                }
            });
        } else {
            setItem(52, plugin.getHeadManager().createBuilder("ui.without_bets")
                    .name("<gray><bold>Без ставок</bold></gray>")
                    .lore("<dark_gray>Повышение ставок недоступно в игре без ставок.</dark_gray>")
                    .build());
        }

        // Slot 53: Fold / Surrender Button
        setItem(53, plugin.getHeadManager().createBuilder("ui.surrender")
                .name("<red><bold>СБРОСИТЬ КАРТЫ (ФОЛД)</bold></red>")
                .lore(
                        "<gray>Сбросить карты и признать поражение в раздаче.</gray>",
                        "<red>▶ Нажмите для выхода / сброса карт</red>"
                )
                .build(), click -> {
            game.actionPokerFold(player);
        });
    }

    @Override
    public void handleClose() {
        if (!isSwitchingInventory()) {
            game.onPlayerClose(player);
        }
    }
}
