package dev.lovelace.loveactivities.config;

import dev.lovelace.loveactivities.LoveActivities;
import dev.lovelace.loveactivities.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HeadManager {

    private final LoveActivities plugin;
    private final Map<String, String> textureCache = new HashMap<>();

    public HeadManager(LoveActivities plugin) {
        this.plugin = plugin;
    }

    public void load() {
        textureCache.clear();

        // 1. Load embedded defaults from JAR first so all new heads are always available
        java.io.InputStream defaultStream = plugin.getResource("heads.yml");
        java.util.Map<String, String> jarDefaults = new java.util.HashMap<>();
        if (defaultStream != null) {
            try (java.io.InputStreamReader reader = new java.io.InputStreamReader(defaultStream, java.nio.charset.StandardCharsets.UTF_8)) {
                FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(reader);
                parseConfigIntoCache(defaultConfig);
                jarDefaults.putAll(textureCache);
            } catch (Exception ignored) {
            }
        }

        // 2. Load file from disk (and save default if missing)
        File file = new File(plugin.getDataFolder(), "heads.yml");
        if (!file.exists()) {
            plugin.saveResource("heads.yml", false);
        }
        if (file.exists()) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            parseConfigIntoCache(config);
            // The file on disk wins over the jar, so an old shipped head in it must be swapped for the new default.
            textureCache.replaceAll((key, value) -> LegacyHeads.upgrade(key, value, jarDefaults.get(key)));
        }
    }

    private void parseConfigIntoCache(FileConfiguration config) {
        for (String rootKey : config.getKeys(false)) {
            if (config.isConfigurationSection(rootKey)) {
                for (String childKey : config.getConfigurationSection(rootKey).getKeys(true)) {
                    String fullPath = rootKey + "." + childKey;
                    if (config.isString(fullPath)) {
                        textureCache.put(fullPath, config.getString(fullPath));
                    }
                }
            } else if (config.isString(rootKey)) {
                textureCache.put(rootKey, config.getString(rootKey));
            }
        }
    }

    public String getTexture(String path) {
        return textureCache.getOrDefault(path, "");
    }

    public ItemStack createHead(String path) {
        String tex = getTexture(path);
        if (tex.isEmpty()) {
            return new ItemStack(Material.PLAYER_HEAD);
        }
        return ItemBuilder.base64Head(tex).build();
    }

    public ItemBuilder createBuilder(String path) {
        String tex = getTexture(path);
        if (tex.isEmpty()) {
            return ItemBuilder.skull();
        }
        return ItemBuilder.base64Head(tex);
    }
}
