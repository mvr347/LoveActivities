package dev.lovelace.loveactivities.gui;

import dev.lovelace.loveactivities.api.GameType;
import dev.lovelace.loveactivities.util.ItemBuilder;
import dev.lovelace.loveactivities.util.MenuLayout;
import dev.lovelace.loveactivities.util.SoundUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class WaitingTutorialGUI extends AbstractGUI {

    private final Player student;
    private final GameType gameType;
    private final Runnable onOpenTutorialForMe;
    private final Runnable onCancelGame;

    public WaitingTutorialGUI(Player player, Player student, GameType gameType,
                              Runnable onOpenTutorialForMe,
                              Runnable onCancelGame) {
        super(player, MenuLayout.SIZE, "<gradient:#FF5E62:#FF9966>Ожидание: " + gameType.getNameRu() + "</gradient>");
        this.student = student;
        this.gameType = gameType;
        this.onOpenTutorialForMe = onOpenTutorialForMe;
        this.onCancelGame = onCancelGame;
    }

    @Override
    public void initializeItems() {
        inventory.clear();
        clickActions.clear();

        ItemStack border = ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name(Component.empty()).build();
        for (int i = 0; i < MenuLayout.SIZE; i++) {
            inventory.setItem(i, border);
        }

        // 9 slots: [ info ] . . [ read tutorial ] . [ cancel ] . . .
        setItem(0, plugin.getHeadManager().createBuilder("game_icons." + gameType.getIconKey())
                .name("<gold>" + gameType.getNameRu() + " — Ожидание старта</gold>")
                .lore(
                        "<gray>Игрок <yellow>" + (student != null ? student.getName() : "Соперник") + "</yellow> проходит обучение.</gray>",
                        "<gray>Игра начнется автоматически, как только соперник закончит чтение.</gray>"
                )
                .build());

        // Slot 3: Read tutorial too
        setItem(MenuLayout.controlSlots(2)[0], plugin.getHeadManager().createBuilder("ui.tutorial")
                .name("<yellow>📖 Тоже почитать правила</yellow>")
                .lore(
                        "<gray>Пока соперник изучает игру, вы тоже можете освежить правила.</gray>",
                        "<green>▶ Нажмите для просмотра правил</green>"
                )
                .build(), click -> {
            SoundUtil.playClick(player);
            setSwitchingInventory(true);
            if (onOpenTutorialForMe != null) {
                onOpenTutorialForMe.run();
            }
        });

        // Slot 5: Cancel Match
        setItem(MenuLayout.controlSlots(2)[1], plugin.getHeadManager().createBuilder("ui.cancel")
                .name("<red>✖ Отменить игру</red>")
                .lore(
                        "<gray>Не хотите ждать? Отмените игру и верните все ставки.</gray>",
                        "<red>▶ Нажмите для отмены</red>"
                )
                .build(), click -> {
            SoundUtil.playClick(player);
            setSwitchingInventory(true);
            if (onCancelGame != null) {
                onCancelGame.run();
            }
        });
    }

    @Override
    public void handleClose() {
        if (!isSwitchingInventory()) {
            if (onCancelGame != null) {
                onCancelGame.run();
            }
        }
    }
}
