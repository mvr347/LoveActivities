package dev.lovelace.loveactivities.games.dice;

import dev.lovelace.loveactivities.gui.AbstractGUI;
import dev.lovelace.loveactivities.util.ItemBuilder;
import dev.lovelace.loveactivities.util.SoundUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class DiceGUI extends AbstractGUI {

    private final DiceGame game;

    public DiceGUI(Player player, DiceGame game) {
        super(player, 54, "<gradient:#FF5E62:#FF9966>Кости</gradient> <dark_gray>[" + game.getMode().getNameRu() + "]</dark_gray>");
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
        Player p2 = Bukkit.getPlayer(game.getPlayer2());

        int[] diceP1 = game.getDiceP1();
        int[] diceP2 = game.getDiceP2();

        boolean isP1Turn = game.isPlayer1Turn();
        boolean myTurn = (player.getUniqueId().equals(game.getPlayer1()) && isP1Turn) ||
                         (player.getUniqueId().equals(game.getPlayer2()) && !isP1Turn && !game.isP1Finished());

        // Header info at Slot 4 with 3-phase AFK timer
        long elapsed = (System.currentTimeMillis() - game.getLastActionTime()) / 1000L;
        long remaining = Math.max(0L, plugin.getConfigManager().getAfkTurnTimeoutSeconds() - elapsed);

        ItemBuilder timerItem;
        if (remaining > 15) {
            timerItem = plugin.getHeadManager().createBuilder("game_icons.dice")
                    .name("<gradient:#00C9FF:#92FE9D><bold>" + game.getMode().getNameRu() + "</bold></gradient> <dark_gray>•</dark_gray> <green>" + remaining + "с</green>");
        } else if (remaining > 5) {
            timerItem = plugin.getHeadManager().createBuilder("ui.timer_yellow")
                    .name("<gradient:#FF9966:#FF5E62><bold>" + game.getMode().getNameRu() + "</bold></gradient> <dark_gray>•</dark_gray> <yellow>⏳ " + remaining + "с</yellow>");
        } else {
            boolean blink = (System.currentTimeMillis() / 500) % 2 == 0;
            String blinkTex = blink ? plugin.getHeadManager().getTexture("ui.cancel") : plugin.getHeadManager().getTexture("ui.surrender");
            timerItem = ItemBuilder.base64Head(blinkTex)
                    .name("<red><bold>⚠ ВРЕМЯ НА ИСХОДЕ: " + remaining + "с ⚠</bold></red>");
        }

        if (game.getBet() > 0) {
            timerItem.lore(
                    "<gray>Банк: <gold>" + (game.getBet() * 2) + " " + plugin.getLoveCoreBridge().currencyName() + "</gold></gray>",
                    "",
                    isP1Turn ? "<yellow>Бросает: <white>" + (p1 != null ? p1.getName() : "Игрок 1") + "</white></yellow>" :
                               "<yellow>Бросает: <white>" + (p2 != null ? p2.getName() : "Игрок 2") + "</white></yellow>"
            );
        } else {
            timerItem.lore(
                    "<gray>Режим: <white>Без ставки</white></gray>",
                    "",
                    isP1Turn ? "<yellow>Бросает: <white>" + (p1 != null ? p1.getName() : "Игрок 1") + "</white></yellow>" :
                               "<yellow>Бросает: <white>" + (p2 != null ? p2.getName() : "Игрок 2") + "</white></yellow>"
            );
        }
        setItem(4, timerItem.build());

        // Header Slot 7: Tutorial / Combinations
        String tutName = (game.getMode() == DiceMode.POKER) ? "<yellow><bold>Комбинации Костей</bold></yellow>" : "<yellow><bold>Правила игры</bold></yellow>";
        String tutLore = (game.getMode() == DiceMode.POKER) ? "<gray>Таблица старшинства комбинаций покера</gray>" : "<gray>Правила броска и подсчёта очков</gray>";
        setItem(7, plugin.getHeadManager().createBuilder("ui.tutorial")
                .name(tutName)
                .lore(tutLore, "<green>▶ Нажмите для просмотра</green>")
                .build(), click -> game.openTutorial(player));

        // Header Slot 8: Resign
        setItem(8, plugin.getHeadManager().createBuilder("ui.surrender")
                .name("<dark_red><bold>Сдаться</bold></dark_red>")
                .lore("<gray>Признать поражение</gray>")
                .build(), click -> game.resign(player));

        // Player 1 Info at Slot 10
        String p1Combo = game.getP1ComboName();
        String inactiveTex = plugin.getHeadManager().getTexture("ui.inactive_player");
        ItemBuilder p1Head;
        if (isP1Turn) {
            p1Head = ItemBuilder.skull().playerHead(game.getPlayer1())
                    .name("<gradient:#00C9FF:#92FE9D><bold>" + (p1 != null ? p1.getName() : "Игрок 1") + "</bold></gradient>")
                    .lore(
                            "<green><bold>▶ ВАШ ХОД!</bold></green>",
                            "<gray>Результат: <yellow><bold>" + p1Combo + "</bold></yellow></gray>",
                            "<gray>Бросков осталось: <gold>" + game.getP1RerollsLeft() + "</gold></gray>"
                    )
                    .glow(true);
        } else {
            p1Head = ItemBuilder.base64Head(inactiveTex)
                    .name("<gray><bold>" + (p1 != null ? p1.getName() : "Игрок 1") + " (Ожидание)</bold></gray>")
                    .lore(
                            "<dark_gray>Ожидает хода...</dark_gray>",
                            "<gray>Результат: <yellow>" + p1Combo + "</yellow></gray>"
                    );
        }
        setItem(10, p1Head.build());

        // P1 Dice
        int[] p1Slots = (game.getMode() == DiceMode.CLASSIC) ? new int[]{13, 15} : new int[]{12, 13, 14, 15, 16};
        for (int i = 0; i < game.getMode().getDiceCount(); i++) {
            int slot = p1Slots[i];
            int dieVal = (diceP1 != null && i < diceP1.length) ? diceP1[i] : 0;
            boolean isRolling = game.isP1Rolling();

            String faceTex;
            if (isRolling) {
                faceTex = plugin.getHeadManager().getTexture("dice.rolling");
            } else if (dieVal >= 1 && dieVal <= 6) {
                faceTex = plugin.getHeadManager().getTexture("dice.face_" + dieVal);
            } else {
                faceTex = plugin.getHeadManager().getTexture("dice.rolling");
            }

            boolean isHold = game.isP1Hold(i);
            final int dieIndex = i;

            setItem(slot, ItemBuilder.base64Head(faceTex)
                    .name(dieVal > 0 ? "<yellow><bold>Грань: " + dieVal + "</bold></yellow>" : "<gray>Бросок...</gray>")
                    .lore(game.getMode() == DiceMode.POKER ? (isHold ? "<green><bold>✔ ЗАФИКСИРОВАНО</bold></green>" : "<dark_gray>Нажмите для фиксации</dark_gray>") : null)
                    .build(), click -> {
                if (player.getUniqueId().equals(game.getPlayer1()) && game.getMode() == DiceMode.POKER && isP1Turn) {
                    game.toggleP1Hold(dieIndex);
                    SoundUtil.playClick(player);
                    initializeItems();
                }
            });
        }

        // Player 2 Info at Slot 28
        String p2Combo = game.getP2ComboName();
        ItemBuilder p2Head;
        if (!isP1Turn) {
            p2Head = ItemBuilder.skull().playerHead(game.getPlayer2())
                    .name("<gradient:#FF9966:#FF5E62><bold>" + (p2 != null ? p2.getName() : "Игрок 2") + "</bold></gradient>")
                    .lore(
                            "<green><bold>▶ ВАШ ХОД!</bold></green>",
                            "<gray>Результат: <yellow><bold>" + p2Combo + "</bold></yellow></gray>",
                            "<gray>Бросков осталось: <gold>" + game.getP2RerollsLeft() + "</gold></gray>"
                    )
                    .glow(true);
        } else {
            p2Head = ItemBuilder.base64Head(inactiveTex)
                    .name("<gray><bold>" + (p2 != null ? p2.getName() : "Игрок 2") + " (Ожидание)</bold></gray>")
                    .lore(
                            "<dark_gray>Ожидает хода...</dark_gray>",
                            "<gray>Результат: <yellow>" + p2Combo + "</yellow></gray>"
                    );
        }
        setItem(28, p2Head.build());

        // P2 Dice
        int[] p2Slots = (game.getMode() == DiceMode.CLASSIC) ? new int[]{31, 33} : new int[]{30, 31, 32, 33, 34};
        for (int i = 0; i < game.getMode().getDiceCount(); i++) {
            int slot = p2Slots[i];
            int dieVal = (diceP2 != null && i < diceP2.length) ? diceP2[i] : 0;
            boolean isRolling = game.isP2Rolling();

            String faceTex;
            if (isRolling) {
                faceTex = plugin.getHeadManager().getTexture("dice.rolling");
            } else if (dieVal >= 1 && dieVal <= 6) {
                faceTex = plugin.getHeadManager().getTexture("dice.face_" + dieVal);
            } else {
                faceTex = plugin.getHeadManager().getTexture("dice.rolling");
            }

            boolean isHold = game.isP2Hold(i);
            final int dieIndex = i;

            setItem(slot, ItemBuilder.base64Head(faceTex)
                    .name(dieVal > 0 ? "<yellow><bold>Грань: " + dieVal + "</bold></yellow>" : "<gray>Бросок...</gray>")
                    .lore(game.getMode() == DiceMode.POKER ? (isHold ? "<green><bold>✔ ЗАФИКСИРОВАНО</bold></green>" : "<dark_gray>Нажмите для фиксации</dark_gray>") : null)
                    .build(), click -> {
                if (player.getUniqueId().equals(game.getPlayer2()) && game.getMode() == DiceMode.POKER && !isP1Turn) {
                    game.toggleP2Hold(dieIndex);
                    SoundUtil.playClick(player);
                    initializeItems();
                }
            });
        }

        // Action Buttons Row (Slots 48, 50)
        // Roll Button
        setItem(48, plugin.getHeadManager().createBuilder("dice.rolling")
                .name("<green><bold>БРОСИТЬ КОСТИ</bold></green>")
                .lore(
                        myTurn ? "<green>▶ Нажмите для броска</green>" : "<red>Сейчас не ваш ход</red>"
                )
                .build(), click -> {
            if (game.isPlayerTurn(player)) {
                game.actionRoll(player);
            } else {
                SoundUtil.playError(player);
            }
        });

        // Stand / Confirm Hand (Poker only or pass)
        if (game.getMode() == DiceMode.POKER) {
            setItem(50, plugin.getHeadManager().createBuilder("ui.confirm")
                    .name("<yellow><bold>ЗАВЕРШИТЬ ХОД</bold></yellow>")
                    .lore(
                            myTurn ? "<yellow>▶ Нажмите для подтверждения</yellow>" : "<red>Сейчас не ваш ход</red>"
                    )
                    .build(), click -> {
                if (game.isPlayerTurn(player)) {
                    game.actionStand(player);
                } else {
                    SoundUtil.playError(player);
                }
            });
        }
    }

    @Override
    public void handleClose() {
        if (!isSwitchingInventory()) {
            game.onPlayerClose(player);
        }
    }
}
