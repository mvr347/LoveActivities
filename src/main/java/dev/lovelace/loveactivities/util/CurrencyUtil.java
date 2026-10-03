package dev.lovelace.loveactivities.util;

import dev.lovelace.lovecore.api.LoveCore;
import dev.lovelace.lovecore.api.economy.Denomination;
import dev.lovelace.lovecore.api.economy.LoveEconomy;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Method;
import java.util.*;

public class CurrencyUtil {

    /**
     * Last-resort table used only when LoveCore is not installed (it is a soft dependency).
     * With LoveCore present, denominations always come from {@link LoveEconomy} - this is the single
     * source of truth (2026-10-03: the old copy 1000/100/50/10/1 priced deposits by the wrong values).
     */
    private static final Map<String, Long> FALLBACK = new LinkedHashMap<>();

    static {
        FALLBACK.put("netherite_coin", 100_000L);
        FALLBACK.put("diamond_coin", 20_000L);
        FALLBACK.put("gold_coin", 2_000L);
        FALLBACK.put("iron_coin", 100L);
        FALLBACK.put("copper_coin", 1L);
    }

    private static LoveEconomy economy() {
        try {
            return LoveCore.service(LoveEconomy.class).orElse(null);
        } catch (Throwable t) {
            return null; // LoveCore API absent at runtime
        }
    }

    private static String keyOf(Denomination den) {
        String id = den.itemId().toLowerCase();
        int colon = id.indexOf(':');
        return colon >= 0 ? id.substring(colon + 1) : id;
    }

    /** Coin key -> value, highest first. {@code includeHidden}: also denominations hidden from splitting (netherite). */
    private static Map<String, Long> denominations(boolean includeHidden) {
        LoveEconomy eco = economy();
        if (eco == null) {
            Map<String, Long> m = new LinkedHashMap<>(FALLBACK);
            if (!includeHidden) m.remove("netherite_coin");
            return m;
        }
        List<Denomination> list = new ArrayList<>(includeHidden ? eco.allDenominations() : eco.denominations());
        list.sort(Comparator.comparingLong(Denomination::value).reversed());
        Map<String, Long> m = new LinkedHashMap<>();
        for (Denomination d : list) m.put(keyOf(d), d.value());
        return m;
    }

    /** Glyph tag is the denomination id, so a renamed or added coin needs no code change. */
    public static String getCoinFontImage(String coinKey) {
        if (coinKey == null || coinKey.isBlank()) return "<white>%img_copper_coin%</white>";
        return "<white>%img_" + coinKey + "%</white>";
    }

    public static String getCoinNameRu(String coinKey) {
        if (coinKey == null) return "Медная монета";
        return switch (coinKey) {
            case "netherite_coin" -> "Незеритовая монета";
            case "diamond_coin" -> "Алмазная монета";
            case "gold_coin" -> "Золотая монета";
            case "iron_coin" -> "Железная монета";
            default -> "Медная монета";
        };
    }

    public static String getCoinKey(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return null;

        LoveEconomy eco = economy();
        if (eco != null) {
            long unit = eco.valueOf(item);
            if (unit <= 0) return null; // LoveCore decides what is money; no name/material guessing
            for (Map.Entry<String, Long> e : denominations(true).entrySet()) {
                if (e.getValue() == unit) return e.getKey();
            }
            return null;
        }

        Map<String, Long> DENOMINATIONS = denominations(true);

        String iaId = getItemsAdderId(item);
        if (iaId != null) {
            String cleanId = iaId.toLowerCase();
            if (cleanId.contains(":")) {
                cleanId = cleanId.substring(cleanId.indexOf(":") + 1);
            }
            if (DENOMINATIONS.containsKey(cleanId)) {
                return cleanId;
            }
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            String name = PlainTextComponentSerializer.plainText().serialize(meta.displayName()).toLowerCase();
            if (name.contains("незеритов") || name.contains("netherite")) return "netherite_coin";
            if (name.contains("алмазн") || name.contains("diamond")) return "diamond_coin";
            if (name.contains("золот") || name.contains("gold")) return "gold_coin";
            if (name.contains("железн") || name.contains("iron")) return "iron_coin";
            if (name.contains("медн") || name.contains("copper")) return "copper_coin";
        }

        Material mat = item.getType();
        if (mat == Material.NETHERITE_INGOT) return "netherite_coin";
        if (mat == Material.DIAMOND) return "diamond_coin";
        if (mat == Material.GOLD_INGOT) return "gold_coin";
        if (mat == Material.IRON_INGOT) return "iron_coin";
        if (mat == Material.COPPER_INGOT) return "copper_coin";

        return null;
    }

    public static String formatCoinsShort(long amount) {
        if (amount <= 0) return "<white>%img_copper_coin%</white> 0 монет";
        StringBuilder sb = new StringBuilder();
        long remaining = amount;
        for (Map.Entry<String, Long> entry : denominations(false).entrySet()) {
            long count = remaining / entry.getValue();
            if (count > 0) {
                remaining %= entry.getValue();
                if (sb.length() > 0) sb.append(" ");
                sb.append(getCoinFontImage(entry.getKey())).append(" x").append(count);
            }
        }
        return sb.toString();
    }

    public static String formatCoinsWords(long amount) {
        if (amount <= 0) return "0 монет";
        StringBuilder sb = new StringBuilder();
        long remaining = amount;
        for (Map.Entry<String, Long> entry : denominations(false).entrySet()) {
            long count = remaining / entry.getValue();
            if (count > 0) {
                remaining %= entry.getValue();
                if (sb.length() > 0) sb.append(" ");
                sb.append(getCoinFontImage(entry.getKey())).append(" ").append(getCoinNameRu(entry.getKey())).append(" x").append(count);
            }
        }
        return sb.toString();
    }

    public static String formatItemCoin(ItemStack item) {
        if (item == null) return "";
        String key = getCoinKey(item);
        if (key != null) {
            return getCoinFontImage(key) + " " + getCoinNameRu(key) + " x" + item.getAmount();
        }
        return item.getType().name() + " x" + item.getAmount();
    }

    public static long getCoinValue(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return 0L;
        long unitValue = getUnitCoinValue(item);
        return unitValue * item.getAmount();
    }

    public static boolean isCoin(ItemStack item) {
        return getUnitCoinValue(item) > 0L;
    }

    public static long getUnitCoinValue(ItemStack item) {
        LoveEconomy eco = economy();
        if (eco != null && item != null && item.getType() != Material.AIR) {
            return Math.max(0L, eco.valueOf(item));
        }
        String key = getCoinKey(item);
        Long value = key == null ? null : denominations(true).get(key);
        return value == null ? 0L : value;
    }

    private static String getItemsAdderId(ItemStack item) {
        try {
            Class<?> customStackClass = Class.forName("dev.lone.itemsadder.api.CustomStack");
            Method byItemStackMethod = customStackClass.getMethod("byItemStack", ItemStack.class);
            Object customStack = byItemStackMethod.invoke(null, item);
            if (customStack != null) {
                Method getIdMethod = customStackClass.getMethod("getId");
                return (String) getIdMethod.invoke(customStack);
            }
        } catch (Throwable ignored) {}
        return null;
    }

    public static ItemStack createCoinItem(String coinKey, int amount) {
        // Try creating via ItemsAdder
        try {
            Class<?> customStackClass = Class.forName("dev.lone.itemsadder.api.CustomStack");
            Method getInstanceMethod = customStackClass.getMethod("getInstance", String.class);
            Object stack = getInstanceMethod.invoke(null, "voidcore:" + coinKey);
            if (stack == null) {
                stack = getInstanceMethod.invoke(null, coinKey);
            }
            if (stack != null) {
                Method getItemStackMethod = customStackClass.getMethod("getItemStack");
                ItemStack item = (ItemStack) getItemStackMethod.invoke(stack);
                if (item != null) {
                    item.setAmount(amount);
                    return item;
                }
            }
        } catch (Throwable ignored) {}

        // Fallback standard ItemStack
        return switch (coinKey) {
            case "netherite_coin" -> ItemBuilder.from(Material.NETHERITE_INGOT).amount(amount).name("<dark_gray>Незеритовая монета</dark_gray>").build();
            case "diamond_coin" -> ItemBuilder.from(Material.DIAMOND).amount(amount).name("<aqua>Алмазная монета</aqua>").build();
            case "gold_coin" -> ItemBuilder.from(Material.GOLD_INGOT).amount(amount).name("<gold>Золотая монета</gold>").build();
            case "iron_coin" -> ItemBuilder.from(Material.IRON_INGOT).amount(amount).name("<gray>Железная монета</gray>").build();
            default -> ItemBuilder.from(Material.COPPER_INGOT).amount(amount).name("<gold>Медная монета</gold>").build();
        };
    }

    public static List<ItemStack> convertAmountToCoins(long amount) {
        List<ItemStack> list = new ArrayList<>();
        if (amount <= 0) return list;

        long remaining = amount;
        for (Map.Entry<String, Long> entry : denominations(false).entrySet()) {
            String coinKey = entry.getKey();
            long value = entry.getValue();

            long count = remaining / value;
            if (count > 0) {
                remaining %= value;
                while (count > 0) {
                    int stackAmount = (int) Math.min(count, 64);
                    list.add(createCoinItem(coinKey, stackAmount));
                    count -= stackAmount;
                }
            }
        }
        return list;
    }

    public static void giveCoinsToPlayer(Player player, long amount) {
        if (player == null || !player.isOnline() || amount <= 0) return;
        LoveEconomy eco = economy();
        if (eco != null) {
            eco.give(player, amount);
            return;
        }
        List<ItemStack> items = convertAmountToCoins(amount);
        for (ItemStack item : items) {
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
            if (!leftover.isEmpty()) {
                for (ItemStack drop : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
            }
        }
    }
}
