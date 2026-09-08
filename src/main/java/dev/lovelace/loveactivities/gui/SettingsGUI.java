package dev.lovelace.loveactivities.gui;

import dev.lovelace.loveactivities.api.GameType;
import dev.lovelace.loveactivities.database.PlayerSettings;
import dev.lovelace.loveactivities.util.ItemBuilder;
import dev.lovelace.loveactivities.util.SoundUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class SettingsGUI extends AbstractGUI {

    public SettingsGUI(Player player) {
        super(player, 45, "<gradient:#FF5E62:#FF9966><bold>Личные настройки</bold></gradient>");
    }

    @Override
    public void initializeItems() {
        inventory.clear();
        clickActions.clear();

        ItemStack glass = ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name(Component.empty()).build();

        // Header Row 0 (0-8)
        for (int i = 0; i <= 8; i++) {
            inventory.setItem(i, glass);
        }

        // Header Row 1 (9-17) - 100% glass (RULE 5)
        for (int i = 9; i <= 17; i++) {
            inventory.setItem(i, glass);
        }

        // Footer Row 4 (36-44) - (RULE 7)
        for (int i = 36; i <= 44; i++) {
            inventory.setItem(i, glass);
        }

        // Slot 0 - Player Head (RULE 3)
        setItem(0, ItemBuilder.skull()
                .playerHead(player.getUniqueId())
                .name("<gradient:#00C9FF:#92FE9D><bold>" + player.getName() + "</bold></gradient>")
                .lore(
                        "<gray>Ваш профиль настроек</gray>",
                        "<dark_gray>Управляйте приватностью и играми</dark_gray>"
                )
                .build());

        PlayerSettings settings = plugin.getSettingsManager().getSettings(player.getUniqueId());

        // Working area (18-35) - strictly NO glass, side columns empty (AIR)
        // Global Toggles (Row 2: 19, 21, 23, 25)
        // 19: DND
        String dndTex = settings.isDnd() ? plugin.getHeadManager().getTexture("ui.status_on") : plugin.getHeadManager().getTexture("ui.status_off");
        setItem(19, ItemBuilder.base64Head(dndTex)
                .name("<yellow><bold>Режим «Не беспокоить»</bold></yellow>")
                .lore(
                        "<gray>Статус: " + (settings.isDnd() ? "<red><bold>ВКЛЮЧЁН (Блокирует вызовы)</bold></red>" : "<green><bold>ВЫКЛЮЧЁН</bold></green>") + "</gray>",
                        "",
                        "<yellow>▶ Нажмите для переключения</yellow>"
                )
                .build(), click -> {
            boolean newState = plugin.getSettingsManager().toggleDnd(player.getUniqueId());
            SoundUtil.playClick(player);
            plugin.getLocaleManager().send(player, newState ? "dnd_enabled" : "dnd_disabled");
            initializeItems();
        });

        // 21: Sounds
        String soundTex = settings.isSounds() ? plugin.getHeadManager().getTexture("ui.status_on") : plugin.getHeadManager().getTexture("ui.status_off");
        setItem(21, ItemBuilder.base64Head(soundTex)
                .name("<yellow><bold>Звуковые эффекты</bold></yellow>")
                .lore(
                        "<gray>Статус: " + (settings.isSounds() ? "<green><bold>ВКЛЮЧЕНЫ</bold></green>" : "<red><bold>ВЫКЛЮЧЕНЫ</bold></red>") + "</gray>",
                        "",
                        "<yellow>▶ Нажмите для переключения</yellow>"
                )
                .build(), click -> {
            plugin.getSettingsManager().toggleSounds(player.getUniqueId());
            SoundUtil.playClick(player);
            initializeItems();
        });

        // 23: Particles
        String particleTex = settings.isParticles() ? plugin.getHeadManager().getTexture("ui.status_on") : plugin.getHeadManager().getTexture("ui.status_off");
        setItem(23, ItemBuilder.base64Head(particleTex)
                .name("<yellow><bold>Визуальные частицы</bold></yellow>")
                .lore(
                        "<gray>Статус: " + (settings.isParticles() ? "<green><bold>ВКЛЮЧЕНЫ</bold></green>" : "<red><bold>ВЫКЛЮЧЕНЫ</bold></red>") + "</gray>",
                        "",
                        "<yellow>▶ Нажмите для переключения</yellow>"
                )
                .build(), click -> {
            plugin.getSettingsManager().toggleParticles(player.getUniqueId());
            SoundUtil.playClick(player);
            initializeItems();
        });

        // 25: Master Toggle All Games
        setItem(25, plugin.getHeadManager().createBuilder("ui.all_games_icon")
                .name("<gold><bold>Все игры разом</bold></gold>")
                .lore(
                        "<gray>Включить или отключить все игры сразу.</gray>",
                        "",
                        "<green>▶ ЛКМ — Включить все игры</green>",
                        "<red>▶ ПКМ — Отключить все игры</red>"
                )
                .build(), click -> {
            if (click.isLeftClick()) {
                plugin.getSettingsManager().setAllGamesBlacklisted(player.getUniqueId(), false);
            } else {
                plugin.getSettingsManager().setAllGamesBlacklisted(player.getUniqueId(), true);
            }
            SoundUtil.playClick(player);
            initializeItems();
        });

        // Per-Game Blacklist Toggles (Row 3: 29, 30, 31, 32, 33)
        GameType[] games = {GameType.BLACKJACK, GameType.DICE, GameType.RPS, GameType.GWENT, GameType.CARDS};
        int[] gameSlots = {29, 30, 31, 32, 33};

        for (int i = 0; i < games.length; i++) {
            GameType game = games[i];
            int slot = gameSlots[i];
            boolean disabled = settings.isGameBlacklisted(game);

            String iconTex = plugin.getHeadManager().getTexture("game_icons." + game.getIconKey());
            setItem(slot, ItemBuilder.base64Head(iconTex)
                    .name("<yellow><bold>" + game.getNameRu() + "</bold></yellow>")
                    .lore(
                            "<gray>Статус вызовов: " + (disabled ? "<red><bold>ОТКЛЮЧЕНО</bold></red>" : "<green><bold>РАЗРЕШЕНО</bold></green>") + "</gray>",
                            "",
                            "<yellow>▶ Нажмите для переключения</yellow>"
                    )
                    .build(), click -> {
                plugin.getSettingsManager().toggleGameBlacklist(player.getUniqueId(), game);
                SoundUtil.playClick(player);
                initializeItems();
            });
        }

        // Footer Close Button (Slot 44) (RULE 7)
        setItem(44, plugin.getHeadManager().createBuilder("ui.close")
                .name("<red><bold>Закрыть меню</bold></red>")
                .build(), click -> {
            SoundUtil.playClick(player);
            player.closeInventory();
        });
    }
}
