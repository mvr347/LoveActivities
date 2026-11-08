package dev.lovelace.loveactivities.gui;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class SettingsGuiLayoutTest {

    @Test
    void controlsSitInTheWorkZoneRowCentredInColumnsOneToSeven() {
        for (int count = 1; count <= 7; count++) {
            java.util.Set<Integer> used = new java.util.HashSet<>();
            for (int i = 0; i < count; i++) {
                int slot = SettingsGUI.controlSlot(i, count);
                assertTrue(slot >= 19 && slot <= 25, "row 2, columns 1-7: " + slot);
                assertTrue(used.add(slot), "no overlap");
            }
            int left = SettingsGUI.controlSlot(0, count) - 19;
            int right = 25 - SettingsGUI.controlSlot(count - 1, count);
            assertTrue(Math.abs(left - right) <= 1, "centred for " + count);
        }
        assertEquals(20, SettingsGUI.controlSlot(0, 4), "four controls: 20-23");
        assertEquals(23, SettingsGUI.controlSlot(3, 4));
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
