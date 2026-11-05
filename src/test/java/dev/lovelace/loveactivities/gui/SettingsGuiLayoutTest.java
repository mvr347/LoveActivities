package dev.lovelace.loveactivities.gui;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class SettingsGuiLayoutTest {

    @Test
    void headerControlsAreCentredInSlotsTwoToSeven() {
        int[][] expected = {{4}, {3, 5}, {2, 4, 6}, {2, 3, 5, 6}, {2, 3, 4, 6, 7}, {2, 3, 4, 5, 6, 7}};
        for (int count = 1; count <= 6; count++)
            for (int i = 0; i < count; i++)
                assertEquals(expected[count - 1][i], SettingsGUI.headerSlot(i, count), count + "/" + i);
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
