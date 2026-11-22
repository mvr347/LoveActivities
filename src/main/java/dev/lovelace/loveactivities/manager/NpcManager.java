package dev.lovelace.loveactivities.manager;

import dev.lovelace.loveactivities.LoveActivities;
import dev.lovelace.loveactivities.api.GameType;
import dev.lovelace.loveactivities.util.SoundUtil;
import dev.lovelace.loveactivities.config.ConfigManager;
import dev.lovelace.loveactivities.util.NpcNames;
import dev.lovelace.loveactivities.util.TextUtil;
import dev.lovelace.lovecore.api.LoveCore;
import dev.lovelace.lovecore.api.npc.NpcHandle;
import dev.lovelace.lovecore.api.npc.NpcProvider;
import dev.lovelace.lovecore.api.provider.Providers;
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
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;

public class NpcManager {

    /** The only supported NPC provider. */
    public static final String PROVIDER_ID = Providers.LOVE_ESSENTIAL;

    private final LoveActivities plugin;
    private final File file;
    private YamlConfiguration config;
    private final Map<UUID, NpcActivityConfig> npcMap = new HashMap<>();
    private final Map<String, List<String>> defaultDialogues = new HashMap<>();

    /**
     * Stable id for a binding to a LoveEssential NPC. The body of such an NPC is respawned (new entity uuid) when
     * players come near, so the binding is keyed by the NPC key; this uuid is derived from the key so the rest of the
     * plugin, which keys everything by uuid, needs no changes.
     */
    public static UUID idForKey(String npcKey) {
        return UUID.nameUUIDFromBytes((PROVIDER_ID + ":" + npcKey).getBytes(StandardCharsets.UTF_8));
    }

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
                    String npcKey = section.getString(key + ".npc-key");
                    if (npcKey != null && !npcKey.isBlank()) npc.setNpcKey(npcKey.trim());
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
            if (npc.getNpcKey() != null) config.set(path + ".npc-key", npc.getNpcKey());
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

    /** The binding of a LoveEssential NPC clicked through LoveCore's NpcClickEvent, or null. */
    public NpcActivityConfig getNpcConfig(NpcHandle npc) {
        if (npc == null || npc.key() == null || !PROVIDER_ID.equalsIgnoreCase(npc.providerId())) return null;
        return npcMap.get(idForKey(npc.key()));
    }

    /** The LoveEssential NPC an entity is the body of, or empty for an ordinary entity (or without LoveEssential). */
    private Optional<NpcHandle> npcOf(Entity entity) {
        try {
            return LoveCore.loveEssential(NpcProvider.class).flatMap(provider -> provider.byEntity(entity));
        } catch (LinkageError coreMissingOrOld) {
            return Optional.empty();
        }
    }

    public Map<UUID, NpcActivityConfig> getAllNpcs() {
        return Collections.unmodifiableMap(npcMap);
    }

    public void bindNpc(Entity entity, GameType gameType, long bet, long maxBet, boolean playsBets, String customName) {
        if (entity == null) return;
        String name = (customName != null && !customName.isBlank()) ? customName : entity.getName();
        if (name == null || name.isBlank()) name = "NPC " + gameType.getNameRu();

        Optional<NpcHandle> handle = npcOf(entity);
        if (handle.isPresent()) {
            // A LoveEssential NPC: bound by its key (the body gets a new uuid on respawn); its name belongs to LoveEssential.
            String key = handle.get().key();
            NpcActivityConfig npc = new NpcActivityConfig(idForKey(key), name, gameType, bet, maxBet, playsBets);
            npc.setNpcKey(key);
            npcMap.put(npc.getEntityUuid(), npc);
        } else {
            NpcActivityConfig npc = new NpcActivityConfig(entity.getUniqueId(), name, gameType, bet, maxBet, playsBets);
            npcMap.put(entity.getUniqueId(), npc);
            applyVisibleName(entity, npc.getCustomName());
        }
        save();
    }

    public void bindNpc(Entity entity, GameType gameType, long bet, String customName) {
        bindNpc(entity, gameType, bet, 0L, true, customName);
    }

    /**
     * Shows the green name above an ordinary entity. LoveEssential NPCs never get here: their name belongs to
     * LoveEssential and would be overwritten anyway.
     */
    private void applyVisibleName(Entity entity, String storedName) {
        if (entity instanceof Player) return;
        try {
            entity.customName(TextUtil.parse(storedName));
            entity.setCustomNameVisible(true);
        } catch (Throwable t) {
            plugin.getLogger().warning("Не удалось показать имя NPC над сущностью: " + t.getMessage());
        }
    }

    /** Unbinds the binding of this entity: by NPC key for a LoveEssential NPC, by uuid for an ordinary entity. */
    public boolean unbindNpc(Entity entity) {
        if (entity == null) return false;
        Optional<NpcHandle> handle = npcOf(entity);
        if (handle.isPresent() && unbindNpc(idForKey(handle.get().key()))) return true;
        return unbindNpc(entity.getUniqueId());
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

        String name = NpcNames.normalize(npcName != null ? npcName : SessionManager.NPC_NAME);
        String format = plugin.getConfigManager().getNpcDialogueFormat();
        if (format == null || format.isBlank()) {
            format = ConfigManager.DEFAULT_DIALOGUE_FORMAT;
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
