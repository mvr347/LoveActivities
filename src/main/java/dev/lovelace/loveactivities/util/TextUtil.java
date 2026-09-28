package dev.lovelace.loveactivities.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextUtil {

    private static final Logger LOGGER = Logger.getLogger(TextUtil.class.getName());
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private static final Pattern FONT_TAG_PATTERN = Pattern.compile(":([a-zA-Z0-9_:]+):");
    private static final Pattern IMG_TAG_PATTERN = Pattern.compile("%img_([a-zA-Z0-9_:]+)%");
    private static final Pattern IA_TAG_PATTERN = Pattern.compile("%ia_([a-zA-Z0-9_:]+)%");

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

    public static Component parse(String input) {
        return parse((Player) null, input);
    }

    public static Component parse(Player player, String input) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }

        // 1. Resolve placeholders and ItemsAdder font images before deserialization (LoveBrew standard)
        String text = replaceFontImages(player, input);

        // Clean up any stray or invalid </color> closing tags
        if (text.contains("</color>")) {
            text = text.replace("</color>", "<reset>");
        }

        // 2. Parse into Adventure Component
        if (text.contains("<") && text.contains(">")) {
            // MiniMessage format: convert any legacy section/ampersand codes into MiniMessage tags
            String mmClean = convertLegacyToMiniMessage(text);
            return MINI_MESSAGE.deserialize(mmClean);
        } else if (text.contains("&") || text.contains("§")) {
            // Pure legacy formatting
            return LEGACY_SERIALIZER.deserialize(text.replace('§', '&'));
        } else {
            return MINI_MESSAGE.deserialize(text);
        }
    }

    /**
     * Resolves font image placeholders (%img_tag%, %ia_tag%, :tag:) using PlaceholderAPI
     * and ItemsAdder FontImages API, following LoveBrew's reference implementation.
     */
    public static String replaceFontImages(Player player, String text) {
        if (text == null || text.isBlank()) return text;

        // 1. Try PlaceholderAPI first if available
        if (text.contains("%") && Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
                text = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, text);
            } catch (Throwable t) {
                LOGGER.log(Level.FINEST, "PlaceholderAPI error: " + t.getMessage());
            }
        }

        // 2. Normalize aliases without underscore
        if (text.contains("%img_") || text.contains("%ia_")) {
            text = text.replace("%img_coppercoin%", "%img_copper_coin%")
                    .replace("%img_ironcoin%", "%img_iron_coin%")
                    .replace("%img_goldcoin%", "%img_gold_coin%")
                    .replace("%img_diamondcoin%", "%img_diamond_coin%")
                    .replace("%img_netheritecoin%", "%img_netherite_coin%");

            // Convert %img_tag% and %ia_tag% to :tag: for ItemsAdder
            text = text.replaceAll("%img_([a-zA-Z0-9_:]+)%", ":$1:");
            text = text.replaceAll("%ia_([a-zA-Z0-9_:]+)%", ":$1:");
        }

        // 3. If ItemsAdder is available and text contains font tags (:...:)
        if (isItemsAdderAvailable() && text.contains(":")) {
            try {
                Class<?> fontImagesClass = Class.forName("dev.lone.itemsadder.api.FontImages");
                try {
                    Method m = fontImagesClass.getMethod("replacePlaceholders", Player.class, String.class);
                    Object res = m.invoke(null, player, text);
                    if (res instanceof String str) text = str;
                } catch (NoSuchMethodException e) {
                    Method m = fontImagesClass.getMethod("replacePlaceholders", String.class);
                    Object res = m.invoke(null, text);
                    if (res instanceof String str) text = str;
                }
            } catch (Throwable t) {
                LOGGER.log(Level.FINEST, "ItemsAdder FontImages note: " + t.getMessage());
            }

            // 4. Try FontImageWrapper for any remaining :tag:
            if (text.contains(":")) {
                try {
                    Class<?> wrapperClass = Class.forName("dev.lone.itemsadder.api.FontImageWrapper");
                    Matcher m = FONT_TAG_PATTERN.matcher(text);
                    StringBuilder sb = new StringBuilder();
                    while (m.find()) {
                        String fontName = m.group(1);
                        String replacement = null;

                        // Try direct name and fallback namespaces
                        String[] attempts = {
                                fontName,
                                "voidcore:" + fontName,
                                "currency:" + fontName,
                                "loveactivities:" + fontName,
                                "lovebrew:" + fontName,
                                "lovelace:" + fontName
                        };
                        for (String attempt : attempts) {
                            try {
                                Object wrapper = wrapperClass.getConstructor(String.class).newInstance(attempt);
                                Boolean exists = (Boolean) wrapperClass.getMethod("exists").invoke(wrapper);
                                if (exists != null && exists) {
                                    String fontChar = (String) wrapperClass.getMethod("getString").invoke(wrapper);
                                    if (fontChar != null && !fontChar.isEmpty()) {
                                        replacement = fontChar;
                                        break;
                                    }
                                }
                            } catch (Throwable t) {
                                LOGGER.log(Level.FINEST, "FontImageWrapper lookup: " + t.getMessage());
                            }
                        }

                        if (replacement != null) {
                            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
                        } else {
                            m.appendReplacement(sb, Matcher.quoteReplacement(m.group(0)));
                        }
                    }
                    m.appendTail(sb);
                    text = sb.toString();
                } catch (Throwable t) {
                    LOGGER.log(Level.FINEST, "FontImageWrapper error: " + t.getMessage());
                }
            }
        }

        // 5. Fallback for coin font tags if ItemsAdder was not available or tag unknown
        if (text.contains(":")) {
            text = text.replace(":copper_coin:", "🪙")
                    .replace(":iron_coin:", "🪙")
                    .replace(":gold_coin:", "🪙")
                    .replace(":diamond_coin:", "🪙")
                    .replace(":netherite_coin:", "🪙");
        }

        return text;
    }

    public static boolean isItemsAdderAvailable() {
        return Bukkit.getPluginManager().isPluginEnabled("ItemsAdder");
    }

    /**
     * Converts legacy color codes (e.g. §a, §f, &e) inside a MiniMessage-compatible string.
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

    public static String colorize(String legacyText) {
        if (legacyText == null) return "";
        return legacyText.replace('&', '§');
    }
}
