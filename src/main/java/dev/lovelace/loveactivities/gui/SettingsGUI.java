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
        super(player, 45, "<gradient:#FF5E62:#FF9966>Личные настройки</gradient>");
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
                .name("<gradient:#00C9FF:#92FE9D>" + player.getName() + "</gradient>")
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
                .name("<yellow>Режим «Не беспокоить»</yellow>")
                .lore(
                        "<gray>Никто не сможет вызвать вас на игру,</gray>",
                        "<gray>пока режим включён. Сами вы играть можете.</gray>",
                        "",
                        "<gray>Статус: " + (settings.isDnd() ? "<red>ВКЛЮЧЁН (Блокирует вызовы)</red>" : "<green>ВЫКЛЮЧЁН</green>") + "</gray>",
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
                .name("<yellow>Звуковые эффекты</yellow>")
                .lore(
                        "<gray>Статус: " + (settings.isSounds() ? "<green>ВКЛЮЧЕНЫ</green>" : "<red>ВЫКЛЮЧЕНЫ</red>") + "</gray>",
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
                .name("<yellow>Визуальные частицы</yellow>")
                .lore(
                        "<gray>Статус: " + (settings.isParticles() ? "<green>ВКЛЮЧЕНЫ</green>" : "<red>ВЫКЛЮЧЕНЫ</red>") + "</gray>",
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
                .name("<gold>Все игры разом</gold>")
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

        // Per-game blacklist toggles: every game the plugin has, centred in row 3 (slots 28-34, up to 7).
        GameType[] all = GameType.values();
        int count = Math.min(all.length, 7);
        GameType[] games = java.util.Arrays.copyOf(all, count);
        int firstSlot = 28 + (7 - count) / 2;
        int[] gameSlots = new int[count];
        for (int i = 0; i < count; i++) {
            gameSlots[i] = firstSlot + i;
        }

        for (int i = 0; i < games.length; i++) {
            GameType game = games[i];
            int slot = gameSlots[i];
            boolean disabled = settings.isGameBlacklisted(game);

            String iconTex = plugin.getHeadManager().getTexture("game_icons." + game.getIconKey());
            setItem(slot, ItemBuilder.base64Head(iconTex)
                    .name("<yellow>" + game.getNameRu() + "</yellow>")
                    .lore(
                            "<gray>Статус вызовов: " + (disabled ? "<red>ОТКЛЮЧЕНО</red>" : "<green>РАЗРЕШЕНО</green>") + "</gray>",
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
                .name("<red>Закрыть меню</red>")
                .build(), click -> {
            SoundUtil.playClick(player);
            player.closeInventory();
        });
    }
}
