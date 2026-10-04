package dev.lovelace.loveactivities.gui;

import dev.lovelace.loveactivities.api.GameType;
import dev.lovelace.loveactivities.util.ItemBuilder;
import dev.lovelace.loveactivities.util.SoundUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class WaitingOpponentGUI extends AbstractGUI {

    private final Player opponent;
    private final GameType gameType;
    private final Runnable onCancelAction;

    public WaitingOpponentGUI(Player player, Player opponent, GameType gameType, Runnable onCancelAction) {
        super(player, 5, "<gradient:#FF5E62:#FF9966>Ожидание соперника...</gradient>");
        this.opponent = opponent;
        this.gameType = gameType;
        this.onCancelAction = onCancelAction;
    }

    @Override
    public void initializeItems() {
        inventory.clear();
        clickActions.clear();

        ItemStack border = ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name(Component.empty()).build();
        for (int i = 0; i < 5; i++) {
            inventory.setItem(i, border);
        }

        // Slot 2: Waiting info (hopper: glass, glass, info, glass, cancel)
        setItem(2, plugin.getHeadManager().createBuilder("game_icons." + (gameType != null ? gameType.getIconKey() : "blackjack"))
                .name("<gold>Ожидание выбора режима...</gold>")
                .lore(
                        "<gray>Соперник: <white>" + (opponent != null ? opponent.getName() : "Игрок") + "</white></gray>",
                        "<gray>Игра: <yellow>" + (gameType != null ? gameType.getNameRu() : "Мини-игра") + "</yellow></gray>",
                        "",
                        "<yellow>⏳ Ожидайте, пока соперник выберет ставки или старт...</yellow>"
                )
                .build());

        // Slot 4: Cancel Button
        setItem(4, plugin.getHeadManager().createBuilder("ui.cancel")
                .name("<red>Отменить вызов</red>")
                .lore("<gray>Выйти из ожидания</gray>")
                .build(), click -> {
            SoundUtil.playClick(player);
            setSwitchingInventory(true);
            player.closeInventory();
            if (onCancelAction != null) {
                onCancelAction.run();
            }
        });
    }

    @Override
    public void handleClose() {
        if (!isSwitchingInventory() && onCancelAction != null) {
            onCancelAction.run();
        }
    }
}
