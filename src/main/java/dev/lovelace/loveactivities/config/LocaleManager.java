package dev.lovelace.loveactivities.config;

import dev.lovelace.loveactivities.LoveActivities;
import dev.lovelace.loveactivities.util.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
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
        repairStaleAcceptButtonCommand(file);
    }

    /**
     * 2026-09-26: до фикса accept/decline/cancel-кнопки в этом файле указывали на
     * /loveactivities accept|decline|cancel {id} - англоязычную команду с permission
     * loveactivities.english (по умолчанию op), поэтому у обычного игрока Bukkit тихо
     * отклонял команду ДО того, как выполнение доходило до PlayerActivityCommand - клик по
     * "ПРИНЯТЬ" не делал вообще ничего. Бандловый ресурс messages.yml в jar'е уже поправлен на
     * /активности (permission loveactivities.use, по умолчанию всем), но saveResource() выше
     * НЕ трогает уже существующий на диске файл - сервер, обновлённый поверх старой установки,
     * так и оставался с битыми кнопками навсегда. Чиним on-disk файл один раз при загрузке.
     */
    private void repairStaleAcceptButtonCommand(File file) {
        boolean changed = false;
        for (String lang : messagesConfig.getKeys(false)) {
            for (String key : new String[]{"request_received", "request_sent"}) {
                String path = lang + "." + key;
                String value = messagesConfig.getString(path);
                if (value == null) continue;
                String fixed = value
                        .replace("/loveactivities accept", "/активности accept")
                        .replace("/loveactivities decline", "/активности decline")
                        .replace("/loveactivities cancel", "/активности cancel");
                if (!fixed.equals(value)) {
                    messagesConfig.set(path, fixed);
                    changed = true;
                }
            }
        }
        if (!changed) {
            return;
        }
        try {
            messagesConfig.save(file);
            plugin.getLogger().info("messages.yml: исправлены устаревшие кнопки accept/decline/cancel, указывавшие на op-only команду /loveactivities.");
        } catch (IOException e) {
            plugin.getLogger().warning("Не удалось сохранить исправленный messages.yml: " + e.getMessage());
        }
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
