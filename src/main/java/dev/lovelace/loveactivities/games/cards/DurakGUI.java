package dev.lovelace.loveactivities.games.cards;

import dev.lovelace.loveactivities.gui.AbstractGUI;
import dev.lovelace.loveactivities.manager.SessionManager;
import dev.lovelace.loveactivities.util.CardItemBuilder;
import dev.lovelace.loveactivities.util.ItemBuilder;
import dev.lovelace.loveactivities.util.BetLore;
import dev.lovelace.loveactivities.util.ParticleUtil;
import dev.lovelace.loveactivities.util.SoundUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DurakGUI extends AbstractGUI {

    private final CardsGame game;
    private int handPage = 0;
    private int oppHandPage = 0;
    private final Set<Integer> barrierSlots = new HashSet<>();

    public DurakGUI(Player player, CardsGame game) {
        super(player, 54, "<gradient:#FF5E62:#FF9966>Дурак подкидной</gradient>");
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

        // Row 5 (Hand row background: 45-53)
        for (int i = 45; i <= 53; i++) {
            inventory.setItem(i, glass);
        }

        boolean isP1 = player.getUniqueId().equals(game.getPlayer1());
        boolean isNpc = game.isNpcMatch();
        Player opp = isNpc ? null : Bukkit.getPlayer(game.getOpponent(player.getUniqueId()));

        DurakBoard board = game.getDurakBoard();
        boolean isMyAttack = isP1 == game.isP1Attacking();
        List<PlayingCard> myHand = isP1 ? game.getHandP1() : game.getHandP2();
        List<PlayingCard> oppHand = isP1 ? game.getHandP2() : game.getHandP1();
        Set<PlayingCard> myKnownCards = isP1 ? game.getKnownToOpponentP1() : game.getKnownToOpponentP2();
        Set<PlayingCard> oppKnownCards = isP1 ? game.getKnownToOpponentP2() : game.getKnownToOpponentP1();

        long oppRevealed = oppHand.stream().filter(oppKnownCards::contains).count();
        long oppHidden = oppHand.size() - oppRevealed;

        // Header Slot 0: Opponent Head
        String oppName = isNpc ? SessionManager.NPC_NAME : (opp != null ? opp.getName() : "Соперник");
        ItemBuilder oppHead = isNpc ? CardItemBuilder.buildBack(plugin) : ItemBuilder.skull().playerHead(opp != null ? opp.getUniqueId() : null);
        setItem(0, oppHead
                .name("<gradient:#FF9966:#FF5E62><bold>" + oppName + "</bold></gradient>")
                .lore(
                        "<gray>Карт в руке: <aqua><bold>" + oppHand.size() + "</bold></aqua></gray>",
                        "<gray>• Раскрытых: <yellow><bold>" + oppRevealed + "</bold></yellow></gray>",
                        "<gray>• Скрытых: <white><bold>" + oppHidden + "</bold></white></gray>",
                        "<gray>Роль: " + (!isMyAttack ? "<red><bold>АТАКУЕТ</bold></red>" : "<green><bold>ЗАЩИЩАЕТСЯ</bold></green>") + "</gray>"
                )
                .build());

        // Header Slot 3: Trump Card Display
        PlayingCard trump = board.getTrumpCard();
        if (trump != null) {
            setItem(3, CardItemBuilder.build(plugin, trump)
                    .name("<gold><bold>Козырь: " + trump.getFormattedName() + "</bold></gold>")
                    .lore("<gray>Козырная масть: " + trump.getSuit().getColorTag() + trump.getSuit().getNameRu() + " " + trump.getSuit().getSymbol() + trump.getSuit().getCloseColorTag() + "</gray>")
                    .build());
        }

        // Header Slot 4: Center Info / Bank with 3-phase AFK timer
        long elapsed = (System.currentTimeMillis() - game.getLastActionTime()) / 1000L;
        long remaining = Math.max(0L, plugin.getConfigManager().getAfkTurnTimeoutSeconds() - elapsed);

        ItemBuilder timerItem;
        if (remaining > 15) {
            timerItem = CardItemBuilder.buildBack(plugin)
                    .name("<gradient:#00C9FF:#92FE9D><bold>Колода: " + board.getDeckRemaining() + " карт</bold></gradient> <dark_gray>•</dark_gray> <green>" + remaining + "с</green>");
        } else if (remaining > 5) {
            timerItem = plugin.getHeadManager().createBuilder("ui.timer_yellow")
                    .name("<gradient:#FF9966:#FF5E62><bold>Колода: " + board.getDeckRemaining() + " карт</bold></gradient> <dark_gray>•</dark_gray> <yellow>⏳ " + remaining + "с</yellow>");
        } else {
            boolean blink = (System.currentTimeMillis() / 500) % 2 == 0;
            String blinkTex = blink ? plugin.getHeadManager().getTexture("ui.cancel") : plugin.getHeadManager().getTexture("ui.surrender");
            timerItem = ItemBuilder.base64Head(blinkTex)
                    .name("<red><bold>⚠ ВРЕМЯ НА ИСХОДЕ: " + remaining + "с ⚠</bold></red>");
        }

        if (game.getBet() > 0) {
            timerItem.lore(BetLore.withRest(game.getBet() * 2,
                    isMyAttack ? "<green>▶ Ваш ход: атакуйте!</green>" : "<yellow>▶ Отбивайте атаки!</yellow>"
            ));
        } else {
            timerItem.lore(
                    "<gray>Режим: <white>Без ставки</white></gray>",
                    isMyAttack ? "<green>▶ Ваш ход: атакуйте!</green>" : "<yellow>▶ Отбивайте атаки!</yellow>"
            );
        }
        setItem(4, timerItem.build());

        // Header Slot 7: Tutorial
        setItem(7, plugin.getHeadManager().createBuilder("ui.tutorial")
                .name("<yellow><bold>Обучение Дураку</bold></yellow>")
                .lore("<gray>Правила игры</gray>")
                .build(), click -> game.openTutorial(player));

        // Header Slot 8: Surrender
        setItem(8, plugin.getHeadManager().createBuilder("ui.surrender")
                .name("<dark_red><bold>Сдаться</bold></dark_red>")
                .lore("<gray>Признать поражение</gray>")
                .build(), click -> game.resign(player));

        // --- Row 1: Opponent Hand (Slots 9-17) with pagination ---
        int oppMaxPerPage = 7;
        int oppTotalPages = Math.max(1, (int) Math.ceil((double) oppHand.size() / oppMaxPerPage));
        if (oppHandPage >= oppTotalPages) oppHandPage = oppTotalPages - 1;
        if (oppHandPage < 0) oppHandPage = 0;

        // Slot 9: Prev page arrow for opponent hand
        if (oppHandPage > 0) {
            setItem(9, plugin.getHeadManager().createBuilder("ui.arrow_left")
                    .name("<yellow><bold>← Предыдущие карты соперника</bold></yellow>")
                    .build(), click -> {
                oppHandPage--;
                SoundUtil.playClick(player);
                initializeItems();
            });
        } else {
            setItem(9, glass);
        }

        // Slots 10-16: Opponent Cards
        int oppStartIndex = oppHandPage * oppMaxPerPage;
        for (int i = 0; i < oppMaxPerPage; i++) {
            int cardIdx = oppStartIndex + i;
            int slot = 10 + i;
            if (cardIdx < oppHand.size()) {
                PlayingCard card = oppHand.get(cardIdx);
                boolean isRevealed = oppKnownCards.contains(card);
                if (isRevealed) {
                    // Card was taken from the table or is known! Show actual face!
                    setItem(slot, CardItemBuilder.build(plugin, card)
                            .name(card.getFormattedName() + " <yellow><bold>[РАСКРЫТА]</bold></yellow>")
                            .lore(
                                    "<yellow>Раскрытая карта соперника</yellow>",
                                    "<gray>Была взята со стола (известна вам)</gray>",
                                    "<dark_gray>Находится в руке соперника</dark_gray>"
                            )
                            .build());
                } else {
                    // Secret card drawn secretly from the deck! Show cardback!
                    setItem(slot, CardItemBuilder.buildBack(plugin)
                            .name("<gradient:#FF9966:#FF5E62><bold>Карта соперника</bold></gradient> <dark_gray>#" + (cardIdx + 1) + "</dark_gray>")
                            .lore(
                                    "<gray>Закрытая карта (рубашка)</gray>",
                                    "<dark_gray>Взята в закрытую из колоды</dark_gray>"
                            )
                            .build());
                }
            } else {
                setItem(slot, glass);
            }
        }

        // Slot 17: Next page arrow for opponent hand
        if (oppHandPage < oppTotalPages - 1) {
            setItem(17, plugin.getHeadManager().createBuilder("ui.arrow_right")
                    .name("<yellow><bold>Следующие карты соперника →</bold></yellow>")
                    .build(), click -> {
                oppHandPage++;
                SoundUtil.playClick(player);
                initializeItems();
            });
        } else {
            setItem(17, glass);
        }

        // Centered Table Area:
        // Attack Row (Slots 20, 21, 22, 23, 24)
        // Defense Row (Slots 29, 30, 31, 32, 33)
        List<DurakBoard.TablePair> table = board.getTable();
        int[] atkSlots = {20, 21, 22, 23, 24};
        int[] defSlots = {29, 30, 31, 32, 33};

        for (int i = 0; i < 5; i++) {
            if (i < table.size()) {
                DurakBoard.TablePair pair = table.get(i);
                PlayingCard atk = pair.attack();
                setItem(atkSlots[i], CardItemBuilder.build(plugin, atk)
                        .name("<red>Атака: </red>" + atk.getFormattedName())
                        .build());

                if (pair.defense() != null) {
                    PlayingCard def = pair.defense();
                    setItem(defSlots[i], CardItemBuilder.build(plugin, def)
                            .name("<green>Отбито: </green>" + def.getFormattedName())
                            .build());
                } else {
                    setItem(defSlots[i], ItemBuilder.from(Material.AIR).build());
                }
            } else {
                setItem(atkSlots[i], ItemBuilder.from(Material.AIR).build());
                setItem(defSlots[i], ItemBuilder.from(Material.AIR).build());
            }
        }

        // Action Buttons:
        // Slot 37: Bito Button (if attacker and all defended)
        if (isMyAttack && board.isAllDefended() && !table.isEmpty()) {
            setItem(37, plugin.getHeadManager().createBuilder("cards.bito")
                    .name("<green><bold>БИТО (Сброс)</bold></green>")
                    .lore("<gray>Все карты отбиты. Завершить кон.</gray>")
                    .build(), click -> game.actionDurakBito(player));
        }

        // Slot 43: Take Button (if defender and table not empty)
        if (!isMyAttack && !table.isEmpty()) {
            setItem(43, plugin.getHeadManager().createBuilder("cards.take_cards")
                    .name("<red><bold>ВЗЯТЬ КАРТЫ</bold></red>")
                    .lore("<gray>Забрать все карты со стола в руку.</gray>")
                    .build(), click -> game.actionDurakTake(player));
        }

        // Find undefended card on table for defense check
        PlayingCard undefendedCard = null;
        for (DurakBoard.TablePair p : table) {
            if (p.defense() == null) {
                undefendedCard = p.attack();
                break;
            }
        }

        // --- Hand Cards in the very bottom row (Slots 45-53) with pagination ---
        int maxPerPage = 7;
        int totalPages = Math.max(1, (int) Math.ceil((double) myHand.size() / maxPerPage));
        if (handPage >= totalPages) handPage = totalPages - 1;
        if (handPage < 0) handPage = 0;

        // Slot 45: Prev page arrow
        if (handPage > 0) {
            setItem(45, plugin.getHeadManager().createBuilder("ui.arrow_left")
                    .name("<yellow><bold>← Предыдущие карты</bold></yellow>")
                    .build(), click -> {
                handPage--;
                SoundUtil.playClick(player);
                initializeItems();
            });
        }

        // Slots 46-52: Hand Cards
        int startIndex = handPage * maxPerPage;
        for (int i = 0; i < maxPerPage; i++) {
            int cardIdx = startIndex + i;
            int slot = 46 + i;
            if (cardIdx < myHand.size()) {
                PlayingCard card = myHand.get(cardIdx);

                if (barrierSlots.contains(slot)) {
                    // Custom error head instead of barrier
                    setItem(slot, plugin.getHeadManager().createBuilder("ui.cancel")
                            .name("<red><bold>✖ Нельзя походить этой картой!</bold></red>")
                            .build());
                    continue;
                }

                boolean canPlay;
                String statusLore;

                if (isMyAttack) {
                    if (table.isEmpty()) {
                        canPlay = true;
                        statusLore = "<green>▶ Нажмите для атаки</green>";
                    } else if (board.canAttackWith(card, oppHand.size() + game.getUndefendedCount())) {
                        canPlay = true;
                        statusLore = "<green><bold>✔ Можно подкинуть</bold></green>";
                    } else {
                        canPlay = false;
                        statusLore = "<red>✖ Нет такого достоинства на столе</red>";
                    }
                } else {
                    if (undefendedCard != null && card.canBeat(undefendedCard, board.getTrumpSuit())) {
                        canPlay = true;
                        statusLore = "<green><bold>✔ Можно отбиться</bold></green>";
                    } else if (undefendedCard != null) {
                        canPlay = false;
                        statusLore = "<red>✖ Не может побить атакующую карту</red>";
                    } else {
                        canPlay = false;
                        statusLore = "<gray>Все карты уже отбиты</gray>";
                    }
                }

                final boolean legal = canPlay;
                final int finalCardIdx = cardIdx;
                final int finalSlot = slot;

                List<String> lores = new ArrayList<>();
                lores.add(statusLore);
                if (myKnownCards.contains(card)) {
                    lores.add("<yellow>👁 Известна сопернику (вы взяли её со стола)</yellow>");
                }
                lores.add(legal ? "<yellow>Клик — сыграть карту</yellow>" : "<dark_red>Недоступно для хода</dark_red>");

                setItem(slot, CardItemBuilder.build(plugin, card)
                        .name(card.getFormattedName())
                        .lore(lores.toArray(new String[0]))
                        .build(), click -> {
                    if (legal) {
                        game.actionDurakPlayCard(player, finalCardIdx);
                    } else {
                        triggerBarrierAnimation(finalSlot);
                    }
                });
            } else {
                setItem(slot, ItemBuilder.from(Material.AIR).build());
            }
        }

        // Slot 53: Next page arrow
        if (handPage < totalPages - 1) {
            setItem(53, plugin.getHeadManager().createBuilder("ui.arrow_right")
                    .name("<yellow><bold>Следующие карты →</bold></yellow>")
                    .build(), click -> {
                handPage++;
                SoundUtil.playClick(player);
                initializeItems();
            });
        }
    }

    private void triggerBarrierAnimation(int slot) {
        SoundUtil.playError(player);
        ParticleUtil.spawnError(player);

        barrierSlots.add(slot);
        inventory.setItem(slot, plugin.getHeadManager().createBuilder("ui.cancel")
                .name("<red><bold>✖ Нельзя походить этой картой!</bold></red>")
                .build());

        // 14 ticks = 0.70 seconds
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            barrierSlots.remove(slot);
            if (player.isOnline() && player.getOpenInventory().getTopInventory().equals(inventory)) {
                initializeItems();
            }
        }, 14L);
    }

    @Override
    public void handleClose() {
        if (!isSwitchingInventory()) {
            game.onPlayerClose(player);
        }
    }
}
