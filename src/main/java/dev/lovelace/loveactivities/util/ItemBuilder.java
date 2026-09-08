package dev.lovelace.loveactivities.util;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class ItemBuilder {

    private final ItemStack item;
    private final ItemMeta meta;

    public ItemBuilder(Material material) {
        this.item = new ItemStack(material);
        this.meta = item.getItemMeta();
    }

    public ItemBuilder(ItemStack item) {
        this.item = item.clone();
        this.meta = this.item.getItemMeta();
    }

    public static ItemBuilder from(Material material) {
        return new ItemBuilder(material);
    }

    public static ItemBuilder from(ItemStack item) {
        return new ItemBuilder(item);
    }

    public static ItemBuilder skull() {
        return new ItemBuilder(Material.PLAYER_HEAD);
    }

    public static ItemBuilder base64Head(String base64) {
        ItemBuilder builder = new ItemBuilder(Material.PLAYER_HEAD);
        return builder.texture(base64);
    }

    public ItemBuilder amount(int amount) {
        item.setAmount(Math.max(1, amount));
        return this;
    }

    public ItemBuilder name(Component name) {
        if (meta != null && name != null) {
            meta.displayName(name.decoration(TextDecoration.ITALIC, false));
        }
        return this;
    }

    public ItemBuilder name(String miniMessageName) {
        return name(TextUtil.parse(miniMessageName));
    }

    public ItemBuilder lore(List<Component> lore) {
        if (meta != null && lore != null) {
            List<Component> cleanLore = new ArrayList<>();
            for (Component line : lore) {
                cleanLore.add(line.decoration(TextDecoration.ITALIC, false));
            }
            meta.lore(cleanLore);
        }
        return this;
    }

    public ItemBuilder lore(Component... lore) {
        return lore(Arrays.asList(lore));
    }

    public ItemBuilder lore(String... miniMessageLore) {
        List<Component> components = new ArrayList<>();
        for (String line : miniMessageLore) {
            components.add(TextUtil.parse(line));
        }
        return lore(components);
    }

    public ItemBuilder flags(ItemFlag... flags) {
        if (meta != null) {
            meta.addItemFlags(flags);
        }
        return this;
    }

    public ItemBuilder hideAllFlags() {
        if (meta != null) {
            meta.addItemFlags(ItemFlag.values());
        }
        return this;
    }

    public ItemBuilder glow(boolean glow) {
        if (meta != null && glow) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        return this;
    }

    public ItemBuilder customModelData(Integer data) {
        if (meta != null && data != null) {
            meta.setCustomModelData(data);
        }
        return this;
    }

    private static final java.util.Map<String, com.destroystokyo.paper.profile.PlayerProfile> PROFILE_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    public ItemBuilder texture(String base64Texture) {
        if (meta instanceof SkullMeta skullMeta && base64Texture != null && !base64Texture.trim().isEmpty()) {
            try {
                String clean = base64Texture.trim().replaceAll("\\s+", "");
                com.destroystokyo.paper.profile.PlayerProfile profile = PROFILE_CACHE.computeIfAbsent(clean, key -> {
                    try {
                        UUID profileUuid = UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
                        com.destroystokyo.paper.profile.PlayerProfile p = Bukkit.createProfile(profileUuid, "ActivityHead");
                        p.setProperty(new com.destroystokyo.paper.profile.ProfileProperty("textures", key));
                        return p;
                    } catch (Exception e) {
                        return null;
                    }
                });
                if (profile != null) {
                    skullMeta.setPlayerProfile(profile);
                }
            } catch (Exception ignored) {
            }
        }
        return this;
    }

    public ItemBuilder playerHead(UUID playerUuid) {
        if (meta instanceof SkullMeta skullMeta && playerUuid != null) {
            try {
                PlayerProfile profile = Bukkit.createProfile(playerUuid);
                skullMeta.setPlayerProfile(profile);
            } catch (Exception ignored) {
            }
        }
        return this;
    }

    public ItemStack build() {
        if (meta != null) {
            item.setItemMeta(meta);
        }
        return item;
    }
}
