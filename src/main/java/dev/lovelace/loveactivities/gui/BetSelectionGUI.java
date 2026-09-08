package dev.lovelace.loveactivities.gui;

import dev.lovelace.loveactivities.LoveActivities;
import dev.lovelace.loveactivities.api.GameType;
import dev.lovelace.loveactivities.util.ItemBuilder;
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
        super(player, 27, "<gradient:#FF5E62:#FF9966><bold>Выбор режима игры</bold></gradient>");
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

        ItemStack border = ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name(Component.empty()).build();
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, border);
        }

        // Slot 4: Game Info
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

        setItem(4, plugin.getHeadManager().createBuilder("game_icons." + gameType.getIconKey())
                .name("<yellow><bold>" + displayName + "</bold></yellow>")
                .lore("<gray>Выберите формат матча</gray>")
                .build());

        // Slot 11: Play with Bet (Physical coins flow)
        setItem(11, plugin.getHeadManager().createBuilder("ui.with_bets")
                .name("<gold><bold>Играть со ставкой</bold></gold>")
                .lore(
                        "<gray>Внесите физические монеты из инвентаря.</gray>",
                        "<gray>Победитель забирает весь банк!</gray>",
                        "",
                        "<yellow>▶ Нажмите для внесения монет</yellow>"
                )
                .build(), click -> {
            selectionMade = true;
            SoundUtil.playClick(player);
            setSwitchingInventory(true);
            if (waitingGUI != null) waitingGUI.setSwitchingInventory(true);

            SharedBetReviewGUI.SharedPhysicalState state = new SharedBetReviewGUI.SharedPhysicalState();

            // Step 1: P1 deposits, P2 waits
            WaitingOpponentGUI p2Wait = new WaitingOpponentGUI(opponent, player, gameType, () -> {
                if (player.isOnline()) player.closeInventory();
            });
            p2Wait.open();

            new PhysicalDepositGUI(player, opponent, gameType, null, p1Items -> {
                state.itemsP1 = p1Items;
                p2Wait.setSwitchingInventory(true);

                // Step 2: P2 deposits, P1 waits
                WaitingOpponentGUI p1Wait = new WaitingOpponentGUI(player, opponent, gameType, () -> {
                    if (opponent.isOnline()) opponent.closeInventory();
                });
                p1Wait.open();

                new PhysicalDepositGUI(opponent, player, gameType, null, p2Items -> {
                    state.itemsP2 = p2Items;
                    p1Wait.setSwitchingInventory(true);

                    // Step 3: Shared review for both
                    SharedBetReviewGUI.openForBoth(player, opponent, gameType, subMode, state);
                }, () -> {
                    p1Wait.setSwitchingInventory(true);
                    player.closeInventory();
                }).open();

            }, () -> {
                p2Wait.setSwitchingInventory(true);
                opponent.closeInventory();
            }).open();
        });

        // Slot 15: Play without Bet (Friendly)
        setItem(15, plugin.getHeadManager().createBuilder("ui.without_bets")
                .name("<green><bold>Играть без ставки</bold></green>")
                .lore(
                        "<gray>Дружеская игра на интерес.</gray>",
                        "<gray>Без риска потерять монеты.</gray>",
                        "",
                        "<green>▶ Нажмите для мгновенного старта</green>"
                )
                .build(), click -> {
            selectionMade = true;
            SoundUtil.playSuccess(player);
            SoundUtil.playSuccess(opponent);
            setSwitchingInventory(true);
            if (waitingGUI != null) waitingGUI.setSwitchingInventory(true);
            plugin.getSessionManager().createAndStartSession(player, opponent, gameType, subMode, 0L);
        });

        // Slot 22: Back / Cancel Button
        setItem(22, plugin.getHeadManager().createBuilder("ui.back")
                .name("<red><bold>← Назад / Отмена</bold></red>")
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
