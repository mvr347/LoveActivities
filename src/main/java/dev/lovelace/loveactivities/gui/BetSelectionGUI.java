package dev.lovelace.loveactivities.gui;

import dev.lovelace.loveactivities.LoveActivities;
import dev.lovelace.loveactivities.api.GameType;
import dev.lovelace.loveactivities.util.Hints;
import dev.lovelace.loveactivities.util.ItemBuilder;
import dev.lovelace.loveactivities.util.MenuLayout;
import dev.lovelace.loveactivities.util.SoundUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class BetSelectionGUI extends AbstractGUI {

    private final Player opponent;
    private final GameType gameType;
    private final String subMode;
    private boolean selectionMade = false;
    private WaitingOpponentGUI waitingGUI;

    public BetSelectionGUI(Player player, Player opponent, GameType gameType) {
        this(player, opponent, gameType, null);
    }

    public BetSelectionGUI(Player player, Player opponent, GameType gameType, String subMode) {
        super(player, MenuLayout.SIZE, "<gradient:#FF5E62:#FF9966>Выбор формата игры</gradient>");
        this.opponent = opponent;
        this.gameType = gameType;
        this.subMode = subMode;
    }

    public void openBoth() {
        if (opponent != null && opponent.isOnline()) {
            this.waitingGUI = new WaitingOpponentGUI(opponent, player, gameType, () -> {
                if (!selectionMade) {
                    selectionMade = true;
                    player.closeInventory();
                    plugin.getLocaleManager().send(player, "request_cancelled", Map.of("player", opponent.getName()));
                    plugin.getLocaleManager().send(opponent, "request_cancelled", Map.of("player", player.getName()));
                }
            });
            this.waitingGUI.open();
        }
        this.open();
    }

    @Override
    public void initializeItems() {
        inventory.clear();
        clickActions.clear();

        // 9 slots: [ game info ] . [ with bet ] . [ without bet ] . [ cancel ] .
        ItemStack border = ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name(Component.empty()).build();
        for (int i = 0; i < MenuLayout.SIZE; i++) {
            inventory.setItem(i, border);
        }

        // Slot 0: Game Info
        String displayName = gameType.getNameRu();
        if ("poker".equalsIgnoreCase(subMode)) {
            displayName = "Покер";
        } else if ("war".equalsIgnoreCase(subMode)) {
            displayName = "Пьяница";
        } else if ("durak".equalsIgnoreCase(subMode)) {
            displayName = "Дурак";
        } else if ("dice_poker".equalsIgnoreCase(subMode)) {
            displayName = "Покер на костях";
        } else if ("dice_classic".equalsIgnoreCase(subMode)) {
            displayName = "Кидание костей";
        }

        setItem(0, plugin.getHeadManager().createBuilder("game_icons." + gameType.getIconKey())
                .name("<yellow>" + displayName + "</yellow>")
                .lore("<gray>Выберите формат матча</gray>")
                .build());

        // Slot 2: Play with Bet (Simultaneous Shared Betting Room)
        setItem(MenuLayout.controlSlots(3)[0], plugin.getHeadManager().createBuilder("ui.with_bets")
                .name("<gold>Играть со ставкой</gold>")
                .lore(
                        "<gray>Внесите физические монеты в общую комнату ставок.</gray>",
                        "<gray>Победитель забирает все ставки!</gray>",
                        "",
                        Hints.act("ЛКМ", "перейти к ставкам")
                )
                .build(), click -> {
            selectionMade = true;
            SoundUtil.playClick(player);
            setSwitchingInventory(true);
            if (waitingGUI != null) waitingGUI.setSwitchingInventory(true);

            SharedBetReviewGUI.openForBoth(player, opponent, gameType, subMode);
        });

        // Slot 4: Play without Bet (Friendly)
        setItem(MenuLayout.controlSlots(3)[1], plugin.getHeadManager().createBuilder("ui.without_bets")
                .name("<green>Играть без ставки</green>")
                .lore(
                        "<gray>Дружеская игра на интерес.</gray>",
                        "<gray>Без риска потерять монеты.</gray>",
                        "",
                        Hints.act("ЛКМ", "начать сразу")
                )
                .build(), click -> {
            selectionMade = true;
            SoundUtil.playSuccess(player);
            SoundUtil.playSuccess(opponent);
            setSwitchingInventory(true);
            if (waitingGUI != null) waitingGUI.setSwitchingInventory(true);
            plugin.getSessionManager().createAndStartSession(player, opponent, gameType, subMode, 0L);
        });

        // Slot 6: Cancel Button
        setItem(MenuLayout.controlSlots(3)[2], plugin.getHeadManager().createBuilder("ui.back")
                .name("<red>Отмена</red>")
                .lore("<gray>Отменить игру и закрыть меню</gray>")
                .build(), click -> {
            selectionMade = true;
            SoundUtil.playClick(player);
            player.closeInventory();
            if (opponent != null && opponent.isOnline()) {
                opponent.closeInventory();
                plugin.getLocaleManager().send(opponent, "request_cancelled", Map.of("player", player.getName()));
            }
            plugin.getLocaleManager().send(player, "request_cancelled", Map.of("player", opponent != null ? opponent.getName() : "Unknown"));
        });
    }

    @Override
    public void handleClose() {
        if (!selectionMade && !isSwitchingInventory()) {
            selectionMade = true;
            if (opponent != null && opponent.isOnline()) {
                opponent.closeInventory();
                plugin.getLocaleManager().send(opponent, "request_cancelled", Map.of("player", player.getName()));
            }
        }
    }
}
