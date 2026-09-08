package dev.lovelace.loveactivities.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.Map;

public class TextUtil {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    // Cached ItemsAdder FontImageWrapper reflection
    private static boolean iaChecked = false;
    private static Method iaReplaceComponentMethod = null;
    private static Method iaReplacePermComponentMethod = null;
    private static Method iaReplaceStringMethod = null;
    private static Method iaReplacePermStringMethod = null;

    private static void initItemsAdder() {
        if (iaChecked) return;
        iaChecked = true;
        try {
            Class<?> fontWrapperClass;
            try {
                // ItemsAdder 4.x
                fontWrapperClass = Class.forName("dev.lone.itemsadder.api.FontImages.FontImageWrapper");
            } catch (ClassNotFoundException e) {
                // ItemsAdder 3.x
                fontWrapperClass = Class.forName("dev.lone.itemsadder.api.FontImageWrapper");
            }

            try {
                iaReplacePermComponentMethod = fontWrapperClass.getMethod("replaceFontImages",
                        org.bukkit.permissions.Permissible.class, Component.class);
            } catch (Throwable ignored) {}

            try {
                iaReplaceComponentMethod = fontWrapperClass.getMethod("replaceFontImages", Component.class);
            } catch (Throwable ignored) {}

            try {
                iaReplacePermStringMethod = fontWrapperClass.getMethod("replaceFontImages",
                        org.bukkit.permissions.Permissible.class, String.class);
            } catch (Throwable ignored) {}

            try {
                iaReplaceStringMethod = fontWrapperClass.getMethod("replaceFontImages", String.class);
            } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}
    }

    public static Component parse(Player player, String input, Map<String, String> placeholders) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        String formatted = input;
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                formatted = formatted.replace("{" + entry.getKey() + "}", entry.getValue() != null ? entry.getValue() : "");
            }
        }
        return parse(player, formatted);
    }

    public static Component parse(String input, Map<String, String> placeholders) {
        return parse((Player) null, input, placeholders);
    }

    public static Component parse(Player player, String input) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }

        String text = input;

        // 1. Ensure white color before any %img_ placeholder so ItemsAdder renders untinted textures
        text = ensureWhiteBeforeImages(text);

        // 2. PlaceholderAPI integration
        if (text.contains("%")) {
            try {
                if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                    text = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, text);
                }
            } catch (Throwable ignored) {}
        }

        // 3. ItemsAdder string-level replacement
        initItemsAdder();
        if (text.contains("%img_") || text.contains(":")) {
            try {
                if (iaReplacePermStringMethod != null && player != null) {
                    text = (String) iaReplacePermStringMethod.invoke(null, player, text);
                } else if (iaReplaceStringMethod != null) {
                    text = (String) iaReplaceStringMethod.invoke(null, text);
                }
            } catch (Throwable ignored) {}
        }

        // 3. Fallback for coin font images if neither PAPI nor ItemsAdder resolved them
        if (text.contains("%img_")) {
            text = text.replace("%img_coppercoin%", "🪙")
                    .replace("%img_ironcoin%", "🪙")
                    .replace("%img_goldcoin%", "🪙")
                    .replace("%img_diamondcoin%", "🪙")
                    .replace("%img_netheritecoin%", "🪙");
            text = text.replaceAll("%img_[a-zA-Z0-9_]+%", "🪙");
        }

        // Clean up any stray or invalid </color> closing tags
        if (text.contains("</color>")) {
            text = text.replace("</color>", "<reset>");
        }

        // 4. Parse into Adventure Component
        Component component;
        if (text.contains("<") && text.contains(">")) {
            // MiniMessage format: convert any legacy section codes into MiniMessage tags
            String mmClean = convertLegacyToMiniMessage(text);
            component = MINI_MESSAGE.deserialize(mmClean);
        } else if (text.contains("&") || text.contains("§")) {
            // Pure legacy formatting
            component = LEGACY_SERIALIZER.deserialize(text.replace('§', '&'));
        } else {
            component = MINI_MESSAGE.deserialize(text);
        }

        // 5. ItemsAdder Component-level replacement (ensures custom font characters are preserved)
        if (iaReplacePermComponentMethod != null && player != null) {
            try {
                component = (Component) iaReplacePermComponentMethod.invoke(null, player, component);
            } catch (Throwable ignored) {}
        } else if (iaReplaceComponentMethod != null) {
            try {
                component = (Component) iaReplaceComponentMethod.invoke(null, component);
            } catch (Throwable ignored) {}
        }

        return component;
    }

    public static Component parse(String input) {
        return parse((Player) null, input);
    }

    /**
     * Converts legacy color codes (e.g. §a, §f, &e) inside a MiniMessage-compatible string
     * to avoid MiniMessage parsing issues with legacy color sequences.
     */
    public static String convertLegacyToMiniMessage(String text) {
        if (text == null || (!text.contains("§") && !text.contains("&"))) {
            return text;
        }

        StringBuilder sb = new StringBuilder(text.length() + 32);
        char[] chars = text.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if ((c == '§' || c == '&') && i + 1 < chars.length) {
                char code = Character.toLowerCase(chars[i + 1]);
                String tag = getMiniMessageTagForLegacy(code);
                if (tag != null) {
                    sb.append(tag);
                    i++; // skip code char
                    continue;
                }
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private static String getMiniMessageTagForLegacy(char code) {
        return switch (code) {
            case '0' -> "<black>";
            case '1' -> "<dark_blue>";
            case '2' -> "<dark_green>";
            case '3' -> "<dark_aqua>";
            case '4' -> "<dark_red>";
            case '5' -> "<dark_purple>";
            case '6' -> "<gold>";
            case '7' -> "<gray>";
            case '8' -> "<dark_gray>";
            case '9' -> "<blue>";
            case 'a' -> "<green>";
            case 'b' -> "<aqua>";
            case 'c' -> "<red>";
            case 'd' -> "<light_purple>";
            case 'e' -> "<yellow>";
            case 'f' -> "<white>";
            case 'k' -> "<obfuscated>";
            case 'l' -> "<bold>";
            case 'm' -> "<strikethrough>";
            case 'n' -> "<underlined>";
            case 'o' -> "<italic>";
            case 'r' -> "<reset>";
            default -> null;
        };
    }

    /**
     * Ensures that any font image placeholder (%img_...%) is preceded by <white>
     * so that Minecraft renders the custom texture in full untinted color.
     */
    public static String ensureWhiteBeforeImages(String text) {
        if (text == null || !text.contains("%img_")) {
            return text;
        }

        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("%img_([a-zA-Z0-9_]+)%");
        java.util.regex.Matcher matcher = pattern.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            int start = matcher.start();
            boolean hasWhitePrefix = false;
            if (start >= 7 && text.substring(start - 7, start).equalsIgnoreCase("<white>")) {
                hasWhitePrefix = true;
            } else if (start >= 2 && (text.startsWith("&f", start - 2) || text.startsWith("§f", start - 2)
                    || text.startsWith("&F", start - 2) || text.startsWith("§F", start - 2))) {
                hasWhitePrefix = true;
            }

            int end = matcher.end();
            boolean hasWhiteSuffix = false;
            if (end + 8 <= text.length() && text.substring(end, end + 8).equalsIgnoreCase("</white>")) {
                hasWhiteSuffix = true;
            }

            String imgTag = matcher.group(0);
            String replacement = (hasWhitePrefix ? "" : "<white>") + imgTag + (hasWhiteSuffix ? "" : "</white>");
            matcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    public static String colorize(String legacyText) {
        if (legacyText == null) return "";
        return legacyText.replace('&', '§');
    }
}
