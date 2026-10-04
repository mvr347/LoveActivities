package dev.lovelace.loveactivities.gui;

import dev.lovelace.loveactivities.api.GameType;
import dev.lovelace.loveactivities.util.ItemBuilder;
import dev.lovelace.loveactivities.util.SoundUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

public class PreGameTutorialGUI extends AbstractGUI {

    private final GameType gameType;
    private final Player opponent;
    private final Runnable onStartGame;
    private final Runnable onOpenTutorial;
    private final Runnable onCancelGame;

    public PreGameTutorialGUI(Player player, Player opponent, GameType gameType,
                              Runnable onStartGame,
                              Runnable onOpenTutorial,
                              Runnable onCancelGame) {
        super(player, 5, "<gradient:#FF5E62:#FF9966>Новая игра: " + gameType.getNameRu() + "</gradient>");
        this.gameType = gameType;
        this.opponent = opponent;
        this.onStartGame = onStartGame;
        this.onOpenTutorial = onOpenTutorial;
        this.onCancelGame = onCancelGame;
    }

    @Override
    public void initializeItems() {
        inventory.clear();
        clickActions.clear();

        ItemStack glass = ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name(Component.empty()).build();

        // Hopper: [ game banner ] [ tutorial ] [ glass ] [ skip ] [ cancel ]
        for (int i = 0; i < 5; i++) {
            inventory.setItem(i, glass);
        }

        // Slot 0: Game Banner
        setItem(0, plugin.getHeadManager().createBuilder("game_icons." + gameType.getIconKey())
                .name("<yellow>Вы впервые играете в " + gameType.getNameRu() + "!</yellow>")
                .lore(
                        "<gray>Соперник: <white>" + (opponent != null ? opponent.getName() : "Бот") + "</white></gray>",
                        "<gray>Хотите ознакомиться с краткими правилами перед началом?</gray>"
                )
                .build());

        // Slot 1: Open Tutorial Button
        setItem(1, plugin.getHeadManager().createBuilder("ui.tutorial")
                .name("<green>📖 Пройти обучение</green>")
                .lore(
                        "<gray>Открыть правила игры и комбинации.</gray>",
                        "<yellow>▶ Соперник подождёт вас в меню ожидания</yellow>"
                )
                .build(), click -> {
            SoundUtil.playClick(player);
            setSwitchingInventory(true);
            setClosed(true);
            if (onOpenTutorial != null) {
                onOpenTutorial.run();
            }
        });

        // Slot 3: Skip Tutorial Button
        setItem(3, plugin.getHeadManager().createBuilder("ui.confirm_ready")
                .name("<gold>▶ Пропустить и начать игру</gold>")
                .lore(
                        "<gray>Я уже знаю правила, сразу к игре!</gray>",
                        "<green>▶ Нажмите для немедленного старта</green>"
                )
                .build(), click -> {
            SoundUtil.playSuccess(player);
            setSwitchingInventory(true);
            setClosed(true);
            if (onStartGame != null) {
                onStartGame.run();
            }
        });

        // Slot 4: Cancel Match Button
        setItem(4, plugin.getHeadManager().createBuilder("ui.cancel")
                .name("<red>✖ Отменить игру</red>")
                .lore("<gray>Вернуть ставки и закрыть матч</gray>")
                .build(), click -> {
            SoundUtil.playClick(player);
            setSwitchingInventory(true);
            setClosed(true);
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
