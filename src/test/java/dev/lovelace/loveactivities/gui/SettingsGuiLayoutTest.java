package dev.lovelace.loveactivities.gui;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class SettingsGuiLayoutTest {

    @Test
    void controlsSitInTheWorkZoneRowByTheEcosystemCentringTable() {
        for (int count = 1; count <= 7; count++) {
            java.util.Set<Integer> used = new java.util.HashSet<>();
            for (int i = 0; i < count; i++) {
                int slot = SettingsGUI.controlSlot(i, count);
                assertTrue(slot >= 19 && slot <= 25, "row 2, columns 1-7: " + slot);
                assertTrue(used.add(slot), "no overlap");
            }
        }
        assertEquals(19, SettingsGUI.controlSlot(0, 4));
        assertEquals(21, SettingsGUI.controlSlot(1, 4));
        assertEquals(23, SettingsGUI.controlSlot(2, 4));
        assertEquals(25, SettingsGUI.controlSlot(3, 4));
        assertEquals(20, SettingsGUI.controlSlot(0, 3));
        assertEquals(22, SettingsGUI.controlSlot(1, 3));
        assertEquals(24, SettingsGUI.controlSlot(2, 3));
    }

    @Test
    void theNewSettingsTexturesExistAndAreDistinct() {
        var heads = resource("heads.yml");
        java.util.Set<String> textures = new java.util.HashSet<>();
        for (String key : new String[]{"toggle_on", "toggle_off", "all_games_on", "all_games_off", "game_enabled"}) {
            String value = heads.getString("ui." + key);
            assertNotNull(value, key);
            assertFalse(value.isBlank(), key);
            assertTrue(textures.add(value), key + " is a duplicate");
        }
        assertNotEquals(heads.getString("ui.status_off"), heads.getString("ui.game_enabled"));
    }

    private static YamlConfiguration resource(String name) {
        var stream = SettingsGuiLayoutTest.class.getClassLoader().getResourceAsStream(name);
        assertNotNull(stream, name);
        return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }

    @Test
    void everySettingsTextExistsInBothLanguages() {
        var messages = resource("messages.yml");
        for (String key : messages.getConfigurationSection("ru").getKeys(false))
            if (key.startsWith("settings_") || key.startsWith("gui_"))
                assertTrue(messages.contains("en." + key), "en." + key);
    }

    @Test
    void theOffTextureMigrationReplacesOnlyTheShippedValueAndMatchesTheBundledHead() {
        var manifest = resource("gui-style-followup.yml");
        var update = manifest.getMapList("updates").getFirst();
        assertEquals("heads.yml", update.get("file"));
        assertEquals(Boolean.TRUE, update.get("preserve-custom"));
        assertNotEquals(update.get("before"), update.get("value"));
        assertEquals(update.get("value"), resource("heads.yml").getString("ui.status_off"));
    }
}
