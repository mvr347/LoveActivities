package dev.lovelace.loveactivities.config;

import dev.lovelace.loveactivities.LoveActivities;
import dev.lovelace.loveactivities.util.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.Collections;
import java.util.Map;

public class LocaleManager {

    private final LoveActivities plugin;
    private FileConfiguration messagesConfig;

    public LocaleManager(LoveActivities plugin) {
        this.plugin = plugin;
    }

    public void load() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        this.messagesConfig = YamlConfiguration.loadConfiguration(file);
    }

    public boolean hasKey(String key) {
        if (key == null || messagesConfig == null) return false;
        String lang = plugin.getConfigManager().getLanguage();
        return messagesConfig.contains(lang + "." + key) || messagesConfig.contains("ru." + key);
    }

    public String getRaw(String key) {
        String lang = plugin.getConfigManager().getLanguage();
        String path = lang + "." + key;
        String val = messagesConfig != null ? messagesConfig.getString(path) : null;
        if (val == null && messagesConfig != null) {
            // fallback to ru
            val = messagesConfig.getString("ru." + key);
        }
        return val != null ? val : "<red>Missing message: " + key + "</red>";
    }

    public Component get(String key) {
        return get(null, key, Collections.emptyMap(), true);
    }

    public Component get(String key, Map<String, String> placeholders) {
        return get(null, key, placeholders, true);
    }

    public Component get(String key, Map<String, String> placeholders, boolean withPrefix) {
        return get(null, key, placeholders, withPrefix);
    }

    public Component get(CommandSender sender, String key, Map<String, String> placeholders, boolean withPrefix) {
        String raw = getRaw(key);
        String prefix = withPrefix ? getRaw("prefix") : "";
        Player player = sender instanceof Player p ? p : null;
        return TextUtil.parse(player, prefix + raw, placeholders);
    }

    public Component getWithoutPrefix(String key, Map<String, String> placeholders) {
        return get(null, key, placeholders, false);
    }

    public Component getWithoutPrefix(CommandSender sender, String key, Map<String, String> placeholders) {
        return get(sender, key, placeholders, false);
    }

    public void send(CommandSender sender, String key) {
        send(sender, key, Collections.emptyMap());
    }

    public void send(CommandSender sender, String key, Map<String, String> placeholders) {
        if (sender == null) return;
        sender.sendMessage(get(sender, key, placeholders, true));
    }

    public void sendRaw(Player player, String key, Map<String, String> placeholders) {
        if (player == null) return;
        player.sendMessage(get(player, key, placeholders, false));
    }
}
