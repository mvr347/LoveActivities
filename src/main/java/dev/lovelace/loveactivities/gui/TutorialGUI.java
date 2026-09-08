package dev.lovelace.loveactivities.gui;

import dev.lovelace.loveactivities.api.GameType;
import dev.lovelace.loveactivities.util.ItemBuilder;
import dev.lovelace.loveactivities.util.SoundUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class TutorialGUI extends AbstractGUI {

    private final GameType gameType;
    private int page = 0;
    private final List<List<String>> pages;
    private final Runnable onCloseAction;

    public TutorialGUI(Player player, GameType gameType, List<List<String>> pages) {
        this(player, gameType, pages, null);
    }

    public TutorialGUI(Player player, GameType gameType, List<List<String>> pages, Runnable onCloseAction) {
        super(player, (pages != null && pages.size() <= 3) ? 27 : 36, "<gradient:#FF5E62:#FF9966><bold>Обучение: " + gameType.getNameRu() + "</bold></gradient>");
        this.gameType = gameType;
        this.pages = (pages != null && !pages.isEmpty()) ? pages : List.of(List.of("<gray>Инструкция отсутствует.</gray>"));
        this.onCloseAction = onCloseAction;
    }

    @Override
    public void initializeItems() {
        inventory.clear();
        clickActions.clear();

        ItemStack glass = ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name(Component.empty()).build();

        if (size == 27) {
            // --- 27 Slots: 3 Rows ---
            // Header: Row 0 (0-8)
            for (int i = 0; i <= 8; i++) {
                inventory.setItem(i, glass);
            }
            // Footer: Row 2 (18-26)
            for (int i = 18; i <= 26; i++) {
                inventory.setItem(i, glass);
            }

            // Header Slot 4: Game Icon
            setItem(4, plugin.getHeadManager().createBuilder("game_icons." + gameType.getIconKey())
                    .name("<yellow><bold>" + gameType.getNameRu() + "</bold></yellow>")
                    .lore("<gray>Обучающее руководство</gray>")
                    .build());

            // Work Zone (Row 1: 9-17) - NO GLASS (Rule 2 & 6)
            List<String> content = pages.get(page);
            String pageTitle = pages.size() > 1
                    ? "<gold><bold>Страница " + (page + 1) + " из " + pages.size() + "</bold></gold>"
                    : "<gold><bold>Правила игры</bold></gold>";

            setItem(13, plugin.getHeadManager().createBuilder("ui.info")
                    .name(pageTitle)
                    .lore(content.toArray(new String[0]))
                    .build());

            // Footer (Row 2: 18-26)
            // Slot 19: Previous Page
            if (pages.size() > 1 && page > 0) {
                setItem(19, plugin.getHeadManager().createBuilder("ui.arrow_left")
                        .name("<yellow><bold>← Предыдущая страница</bold></yellow>")
                        .build(), click -> {
                    page--;
                    SoundUtil.playClick(player);
                    initializeItems();
                });
            }

            // Slot 22: Back to Game
            setItem(22, plugin.getHeadManager().createBuilder("ui.back")
                    .name("<yellow><bold>Вернуться к игре</bold></yellow>")
                    .build(), click -> {
                SoundUtil.playClick(player);
                setSwitchingInventory(true);
                setClosed(true);
                if (onCloseAction != null) {
                    onCloseAction.run();
                } else {
                    player.closeInventory();
                }
            });

            // Slot 25: Next Page
            if (pages.size() > 1 && page < pages.size() - 1) {
                setItem(25, plugin.getHeadManager().createBuilder("ui.arrow_right")
                        .name("<yellow><bold>Следующая страница →</bold></yellow>")
                        .build(), click -> {
                    page++;
                    SoundUtil.playClick(player);
                    initializeItems();
                });
            }
        } else {
            // --- 36 Slots: 4 Rows ---
            // Header: Row 0 (0-8)
            for (int i = 0; i <= 8; i++) {
                inventory.setItem(i, glass);
            }
            // Footer: Row 3 (27-35)
            for (int i = 27; i <= 35; i++) {
                inventory.setItem(i, glass);
            }

            // Header Slot 4: Game Icon
            setItem(4, plugin.getHeadManager().createBuilder("game_icons." + gameType.getIconKey())
                    .name("<yellow><bold>" + gameType.getNameRu() + "</bold></yellow>")
                    .lore("<gray>Обучающее руководство</gray>")
                    .build());

            // Work Zone (Rows 1 & 2: 9-26) - NO GLASS
            List<String> content = pages.get(page);
            String pageTitle = pages.size() > 1
                    ? "<gold><bold>Страница " + (page + 1) + " из " + pages.size() + "</bold></gold>"
                    : "<gold><bold>Правила игры</bold></gold>";

            setItem(13, plugin.getHeadManager().createBuilder("ui.info")
                    .name(pageTitle)
                    .lore(content.toArray(new String[0]))
                    .build());

            // Footer (Row 3: 27-35)
            // Slot 28: Previous Page
            if (pages.size() > 1 && page > 0) {
                setItem(28, plugin.getHeadManager().createBuilder("ui.arrow_left")
                        .name("<yellow><bold>← Предыдущая страница</bold></yellow>")
                        .build(), click -> {
                    page--;
                    SoundUtil.playClick(player);
                    initializeItems();
                });
            }

            // Slot 31: Back to Game
            setItem(31, plugin.getHeadManager().createBuilder("ui.back")
                    .name("<yellow><bold>Вернуться к игре</bold></yellow>")
                    .build(), click -> {
                SoundUtil.playClick(player);
                setSwitchingInventory(true);
                setClosed(true);
                if (onCloseAction != null) {
                    onCloseAction.run();
                } else {
                    player.closeInventory();
                }
            });

            // Slot 34: Next Page
            if (pages.size() > 1 && page < pages.size() - 1) {
                setItem(34, plugin.getHeadManager().createBuilder("ui.arrow_right")
                        .name("<yellow><bold>Следующая страница →</bold></yellow>")
                        .build(), click -> {
                    page++;
                    SoundUtil.playClick(player);
                    initializeItems();
                });
            }
        }
    }

    @Override
    public void handleClose() {
        if (!isSwitchingInventory() && onCloseAction != null) {
            onCloseAction.run();
        }
    }
}
