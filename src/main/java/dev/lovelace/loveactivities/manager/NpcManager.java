package dev.lovelace.loveactivities.manager;

import dev.lovelace.loveactivities.LoveActivities;
import dev.lovelace.loveactivities.api.GameType;
import dev.lovelace.loveactivities.util.SoundUtil;
import dev.lovelace.loveactivities.util.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class NpcManager {

    private final LoveActivities plugin;
    private final File file;
    private YamlConfiguration config;
    private final Map<UUID, NpcActivityConfig> npcMap = new HashMap<>();
    private final Map<String, List<String>> defaultDialogues = new HashMap<>();

    public NpcManager(LoveActivities plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "npcs.yml");
    }

    public void load() {
        npcMap.clear();
        defaultDialogues.clear();

        if (!file.exists()) {
            try {
                plugin.saveResource("npcs.yml", false);
            } catch (Throwable e) {
                try {
                    file.createNewFile();
                } catch (IOException ex) {
                    plugin.getLogger().log(Level.SEVERE, "Could not create npcs.yml", ex);
                }
            }
        }

        config = YamlConfiguration.loadConfiguration(file);

        // 1. Load default dialogues
        ConfigurationSection defSec = config.getConfigurationSection("default_dialogues");
        if (defSec != null) {
            for (String cat : defSec.getKeys(false)) {
                defaultDialogues.put(cat, defSec.getStringList(cat));
            }
        }

        // 2. Load bound NPCs
        ConfigurationSection section = config.getConfigurationSection("npcs");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    String name = section.getString(key + ".name", "NPC Игрок");
                    GameType type = GameType.fromString(section.getString(key + ".game", "BLACKJACK"));
                    long bet = section.getLong(key + ".bet", 0L);
                    long maxBet = section.getLong(key + ".max_bet", 0L);
                    boolean playsBets = section.getBoolean(key + ".plays_bets", true);
                    double acceptChance = section.getDouble(key + ".accept_chance", 0.85);
                    int refusalCooldown = section.getInt(key + ".refusal_cooldown_minutes", 5);
                    long refusedUntil = section.getLong(key + ".refused_until", 0L);

                    NpcActivityConfig npc = new NpcActivityConfig(uuid, name, type, bet, maxBet, playsBets);
                    npc.setAcceptChance(acceptChance);
                    npc.setRefusalCooldownMinutes(refusalCooldown);
                    npc.setRefusedUntil(refusedUntil);

                    ConfigurationSection dSec = section.getConfigurationSection(key + ".dialogues");
                    if (dSec != null) {
                        for (String cat : dSec.getKeys(false)) {
                            npc.getDialogues().put(cat, dSec.getStringList(cat));
                        }
                    }
                    npcMap.put(uuid, npc);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Failed to load NPC config for key " + key, e);
                }
            }
        }
    }

    public void save() {
        if (config == null) config = new YamlConfiguration();
        config.set("npcs", null); // Clear old

        for (Map.Entry<UUID, NpcActivityConfig> entry : npcMap.entrySet()) {
            String path = "npcs." + entry.getKey().toString();
            NpcActivityConfig npc = entry.getValue();
            config.set(path + ".name", npc.getCustomName());
            config.set(path + ".game", npc.getGameType().name());
            config.set(path + ".bet", npc.getDefaultBet());
            config.set(path + ".max_bet", npc.getMaxBet());
            config.set(path + ".plays_bets", npc.isPlaysBets());
            config.set(path + ".accept_chance", npc.getAcceptChance());
            config.set(path + ".refusal_cooldown_minutes", npc.getRefusalCooldownMinutes());
            config.set(path + ".refused_until", npc.getRefusedUntil());

            for (Map.Entry<String, List<String>> d : npc.getDialogues().entrySet()) {
                config.set(path + ".dialogues." + d.getKey(), d.getValue());
            }
        }

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save npcs.yml", e);
        }
    }

    public boolean isNpcBound(UUID entityUuid) {
        return npcMap.containsKey(entityUuid);
    }

    public NpcActivityConfig getNpcConfig(UUID entityUuid) {
        return npcMap.get(entityUuid);
    }

    public Map<UUID, NpcActivityConfig> getAllNpcs() {
        return Collections.unmodifiableMap(npcMap);
    }

    public void bindNpc(Entity entity, GameType gameType, long bet, long maxBet, boolean playsBets, String customName) {
        if (entity == null) return;
        String name = (customName != null && !customName.isBlank()) ? customName : entity.getName();
        if (name == null || name.isBlank()) name = "NPC " + gameType.getNameRu();

        NpcActivityConfig npc = new NpcActivityConfig(entity.getUniqueId(), name, gameType, bet, maxBet, playsBets);
        npcMap.put(entity.getUniqueId(), npc);
        save();
    }

    public void bindNpc(Entity entity, GameType gameType, long bet, String customName) {
        bindNpc(entity, gameType, bet, 0L, true, customName);
    }

    public boolean unbindNpc(UUID entityUuid) {
        if (npcMap.remove(entityUuid) != null) {
            save();
            return true;
        }
        return false;
    }

    public String getDefaultDialogue(String category) {
        List<String> list = defaultDialogues.get(category);
        if (list == null || list.isEmpty()) return null;
        return list.get(new Random().nextInt(list.size()));
    }

    public void speak(Player player, String npcName, String phrase) {
        if (player == null || !player.isOnline() || phrase == null || phrase.isBlank()) return;
        if (!plugin.getConfigManager().isNpcDialoguesEnabled()) return;

        String name = npcName != null ? npcName : SessionManager.NPC_NAME;
        String format = plugin.getConfigManager().getNpcDialogueFormat();
        if (format == null || format.isBlank()) {
            format = "<gradient:#FF9966:#FF5E62>[{npc}]</gradient> <dark_gray>»</dark_gray> <gray>{text}</gray>";
        }
        String formatted = format.replace("{npc}", name).replace("{text}", phrase);
        player.sendMessage(TextUtil.parse(formatted));
        SoundUtil.playClick(player);
    }

    public Entity findTargetEntity(Player player) {
        if (player == null) return null;
        // 1. Native Paper getTargetEntity
        try {
            Entity target = player.getTargetEntity(6);
            if (target != null && target != player) return target;
        } catch (Throwable ignored) {}

        // 2. Raytrace fallback
        Location eye = player.getEyeLocation();
        Vector dir = eye.getDirection();
        RayTraceResult result = player.getWorld().rayTraceEntities(eye, dir, 6.0, 0.5, e -> e != player && (e instanceof LivingEntity || e.getType().name().contains("INTERACTION") || e.getType().name().contains("ARMOR_STAND")));
        return result != null ? result.getHitEntity() : null;
    }
}
