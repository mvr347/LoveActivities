package dev.lovelace.loveactivities.gui;

import dev.lovelace.loveactivities.api.GameType;
import dev.lovelace.loveactivities.config.LocaleManager;
import dev.lovelace.loveactivities.database.PlayerSettings;
import dev.lovelace.loveactivities.util.ItemBuilder;
import dev.lovelace.loveactivities.util.SoundUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Personal settings (45 slots): the Header (rows 0-1) holds only the player's head, the work zone holds
 * the menu controls (row 2) and the game switches (row 3), the one-row Footer holds Back (only when opened
 * from another menu) and Close. 2026-11-07, owner request: the controls moved out of the Header into the
 * work zone (a deliberate departure from the gui_gen rule that controls live in Header slots 2-7).
 */
public class SettingsGUI extends AbstractGUI {

    private static final int SIZE = 45;
    private static final int FOOTER_START = 36;
    private static final int SLOT_CLOSE = 44;
    private static final int SLOT_BACK = 43;
    /** Row of the work zone that holds the game switches (columns 1-7, side walls stay empty). */
    private static final int GAMES_FIRST_SLOT = 28;
    private static final int GAMES_PER_ROW = 7;
    /** First column (1) of the work-zone row that holds the controls. */
    private static final int CONTROLS_FIRST_SLOT = 19;

    /** Command that opened this menu, or null when it was opened directly. */
    private final String backCommand;

    public SettingsGUI(Player player, String backCommand) {
        super(player, SIZE, LoveActivitiesTitles.settings());
        this.backCommand = backCommand;
    }

    /** Slot of control {@code index} of {@code count} in the controls row (19-25), by the ecosystem's centring table. */
    static int controlSlot(int index, int count) {
        int[] offsets = switch (Math.clamp(count, 1, GAMES_PER_ROW)) {
            case 1 -> new int[]{4};
            case 2 -> new int[]{3, 5};
            case 3 -> new int[]{2, 4, 6};
            case 4 -> new int[]{1, 3, 5, 7};
            case 5 -> new int[]{1, 2, 4, 6, 7};
            case 6 -> new int[]{1, 2, 3, 5, 6, 7};
            default -> new int[]{1, 2, 3, 4, 5, 6, 7};
        };
        return CONTROLS_FIRST_SLOT - 1 + offsets[Math.clamp(index, 0, offsets.length - 1)];
    }

    private String raw(String key) {
        return plugin.getLocaleManager().getRaw(key);
    }

    private List<String> lore(String sub, List<String> description, String statusLine, String click) {
        List<String> lore = new ArrayList<>();
        lore.add(raw(sub));
        lore.add("");
        lore.add(raw("gui_sec_purpose"));
        for (String line : description) lore.add("  " + raw(line));
        if (statusLine != null) {
            lore.add("");
            lore.add(raw("gui_sec_status"));
            lore.add("  " + statusLine);
        }
        lore.add("");
        lore.add(raw("gui_sep"));
        lore.add(click);
        return lore;
    }

    private ItemStack glass() {
        return ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name(Component.empty()).build();
    }

    @Override
    public void initializeItems() {
        inventory.clear();
        clickActions.clear();

        // Glass only in the Header (rows 0-1) and the one-row Footer; the work zone is content only.
        for (int i = 0; i < 18; i++) inventory.setItem(i, glass());
        for (int i = FOOTER_START; i < SIZE; i++) inventory.setItem(i, glass());

        LocaleManager locale = plugin.getLocaleManager();
        PlayerSettings settings = plugin.getSettingsManager().getSettings(player.getUniqueId());

        // Slot 0: the player's own head.
        List<String> headLore = new ArrayList<>();
        headLore.add(raw("settings_head_sub"));
        headLore.add("");
        headLore.add(raw("settings_head_d1"));
        headLore.add(raw("settings_head_d2"));
        setItem(0, ItemBuilder.skull()
                .playerHead(player.getUniqueId())
                .name("<gold>" + player.getName() + "</gold>")
                .lore(headLore.toArray(new String[0]))
                .build());

        // Work-zone controls: four buttons, centred in the row above the games.
        int controls = 4;

        setItem(controlSlot(0, controls), switchHead(settings.isDnd(), raw("settings_dnd_title"),
                lore("settings_dnd_sub", List.of("settings_dnd_d1", "settings_dnd_d2"),
                        settings.isDnd() ? raw("settings_dnd_on") : raw("settings_off"), raw("settings_click_toggle"))), click -> {
            boolean newState = plugin.getSettingsManager().toggleDnd(player.getUniqueId());
            SoundUtil.playClick(player);
            locale.send(player, newState ? "dnd_enabled" : "dnd_disabled");
            initializeItems();
        });

        setItem(controlSlot(2, controls), switchHead(settings.isSounds(), raw("settings_sounds_title"),
                lore("settings_sounds_sub", List.of("settings_sounds_d1"),
                        settings.isSounds() ? raw("settings_on") : raw("settings_off"), raw("settings_click_toggle"))), click -> {
            plugin.getSettingsManager().toggleSounds(player.getUniqueId());
            SoundUtil.playClick(player);
            initializeItems();
        });

        setItem(controlSlot(1, controls), switchHead(settings.isParticles(), raw("settings_particles_title"),
                lore("settings_particles_sub", List.of("settings_particles_d1"),
                        settings.isParticles() ? raw("settings_on") : raw("settings_off"), raw("settings_click_toggle"))), click -> {
            plugin.getSettingsManager().toggleParticles(player.getUniqueId());
            SoundUtil.playClick(player);
            initializeItems();
        });

        // Master switch: any game off -> turn all on; all on -> turn all off.
        GameType[] all = GameType.values();
        long blocked = Arrays.stream(all).filter(settings::isGameBlacklisted).count();
        boolean allOn = blocked == 0;
        String state = allOn ? raw("settings_all_on")
                : blocked == all.length ? raw("settings_all_off")
                : raw("settings_all_partial").replace("{on}", String.valueOf(all.length - blocked))
                        .replace("{total}", String.valueOf(all.length));
        setItem(controlSlot(3, controls), plugin.getHeadManager().createBuilder(allOn ? "ui.all_games_on" : "ui.all_games_off")
                .name(raw("settings_all_title"))
                .lore(lore("settings_all_sub", List.of("settings_all_d1", "settings_all_d2"), state,
                        allOn ? raw("settings_click_all_off") : raw("settings_click_all_on")).toArray(new String[0]))
                .build(), click -> {
            plugin.getSettingsManager().setAllGamesBlacklisted(player.getUniqueId(), allOn);
            SoundUtil.playClick(player);
            initializeItems();
        });

        // Work zone: one switch per game, centred in columns 1-7. A disabled game shows the common "off" head.
        int count = Math.min(all.length, GAMES_PER_ROW);
        int first = GAMES_FIRST_SLOT + (GAMES_PER_ROW - count) / 2;
        for (int i = 0; i < count; i++) {
            GameType game = all[i];
            boolean disabled = settings.isGameBlacklisted(game);
            String texture = plugin.getHeadManager().getTexture(disabled ? "ui.status_off" : "ui.game_enabled");
            setItem(first + i, ItemBuilder.base64Head(texture)
                    .name("<gold>" + game.getNameRu() + "</gold>")
                    .lore(lore("settings_game_sub", List.of("settings_game_d1"),
                            disabled ? raw("settings_denied") : raw("settings_allowed"), raw("settings_click_toggle")).toArray(new String[0]))
                    .build(), click -> {
                plugin.getSettingsManager().toggleGameBlacklist(player.getUniqueId(), game);
                SoundUtil.playClick(player);
                initializeItems();
            });
        }

        // Footer: Back only when another menu opened this one; Close is always the last slot.
        if (backCommand != null) {
            setItem(SLOT_BACK, plugin.getHeadManager().createBuilder("ui.back")
                    .name(raw("settings_back"))
                    .lore(raw("gui_sep"), raw("settings_back_click"))
                    .build(), click -> {
                SoundUtil.playClick(player);
                player.performCommand(backCommand);
            });
        }
        setItem(SLOT_CLOSE, plugin.getHeadManager().createBuilder("ui.close")
                .name(raw("settings_close"))
                .lore(raw("gui_sep"), raw("settings_close_click"))
                .build(), click -> {
            SoundUtil.playClick(player);
            player.closeInventory();
        });
    }

    private ItemStack switchHead(boolean on, String title, List<String> lore) {
        String texture = plugin.getHeadManager().getTexture(on ? "ui.toggle_on" : "ui.toggle_off");
        return ItemBuilder.base64Head(texture).name(title).lore(lore.toArray(new String[0])).build();
    }

    /** Title is read from messages.yml once the plugin is up; kept in one place for the constructor. */
    private static final class LoveActivitiesTitles {
        static String settings() {
            return dev.lovelace.loveactivities.LoveActivities.getInstance().getLocaleManager().getRaw("settings_title");
        }
    }
}
