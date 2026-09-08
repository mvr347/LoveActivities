package dev.lovelace.loveactivities.util;

import dev.lovelace.loveactivities.LoveActivities;
import dev.lovelace.loveactivities.games.blackjack.BlackjackCard;
import dev.lovelace.loveactivities.games.cards.PlayingCard;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility for building playing card ItemStacks with ItemsAdder support.
 *
 * User custom names:
 * - Suits: bubna, chirva, chresta, pika
 * - Number cards (2..10): stack amount = card number (e.g., 5 of Hearts = 5 pcs of chirva)
 * - Face cards (1 pcs):
 *   - Jack (Валет) -> <suit>joker (e.g., bubnajoker, chirvajoker, chrestajoker, pikajoker)
 *   - Queen (Дама) -> <suit>queen (e.g., bubnaqueen, chirvaqueen, chrestaqueen, pikaqueen)
 *   - King (Король) -> <suit>king (e.g., bubnaking, chirvaking, chrestaking, pikaking)
 *   - Ace (Туз) -> <suit>ace (e.g., bubnaace, chirvaace, chrestaace, pikaace)
 * - Card back: cardback (also card_back, rubashka)
 */
public class CardItemBuilder {

    private static boolean reflectionChecked = false;
    private static Method getInstanceMethod = null;
    private static Method getItemStackMethod = null;

    private static void initReflection() {
        if (reflectionChecked) return;
        reflectionChecked = true;
        try {
            Class<?> customStackClass = Class.forName("dev.lone.itemsadder.api.CustomStack");
            getInstanceMethod = customStackClass.getMethod("getInstance", String.class);
            getItemStackMethod = customStackClass.getMethod("getItemStack");
        } catch (Throwable ignored) {
            getInstanceMethod = null;
            getItemStackMethod = null;
        }
    }

    public static ItemStack getCustomStack(String id) {
        initReflection();
        if (getInstanceMethod == null || getItemStackMethod == null || id == null || id.isBlank()) {
            return null;
        }
        try {
            Object stack = getInstanceMethod.invoke(null, id);
            if (stack != null) {
                ItemStack item = (ItemStack) getItemStackMethod.invoke(stack);
                if (item != null) {
                    return item.clone();
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    public static ItemStack findCustomStack(String id, String userNamespace) {
        if (id == null || id.isBlank()) return null;

        if (id.contains(":")) {
            ItemStack stack = getCustomStack(id);
            if (stack != null) return stack;

            String rawId = id.substring(id.indexOf(':') + 1);
            stack = getCustomStack(rawId);
            if (stack != null) return stack;
        } else {
            // 1. Raw id (e.g. bubna, bubnajoker, bubnaace)
            ItemStack stack = getCustomStack(id);
            if (stack != null) return stack;

            // 2. User configured namespace (e.g. mypack:bubna)
            if (userNamespace != null && !userNamespace.isBlank()) {
                stack = getCustomStack(userNamespace + ":" + id);
                if (stack != null) return stack;
            }

            // 3. Built-in common namespaces
            stack = getCustomStack("loveactivities:" + id);
            if (stack != null) return stack;

            stack = getCustomStack("itemsadder:" + id);
            if (stack != null) return stack;

            stack = getCustomStack("ia:" + id);
            if (stack != null) return stack;
        }
        return null;
    }

    public static String getSuitKey(PlayingCard.Suit suit) {
        return switch (suit) {
            case DIAMONDS -> "bubna";
            case HEARTS -> "chirva";
            case CLUBS -> "chresta";
            case SPADES -> "pika";
        };
    }

    public static String getSuitKey(BlackjackCard.Suit suit) {
        return switch (suit) {
            case DIAMONDS -> "bubna";
            case HEARTS -> "chirva";
            case CLUBS -> "chresta";
            case SPADES -> "pika";
        };
    }

    public static boolean isFace(PlayingCard.Rank rank) {
        return rank == PlayingCard.Rank.JACK
                || rank == PlayingCard.Rank.QUEEN
                || rank == PlayingCard.Rank.KING
                || rank == PlayingCard.Rank.ACE;
    }

    public static boolean isFace(BlackjackCard.Rank rank) {
        return rank == BlackjackCard.Rank.JACK
                || rank == BlackjackCard.Rank.QUEEN
                || rank == BlackjackCard.Rank.KING
                || rank == BlackjackCard.Rank.ACE;
    }

    public static List<String> getRankSuffixes(PlayingCard.Rank rank) {
        return switch (rank) {
            case JACK -> List.of("joker", "jack", "valet");
            case QUEEN -> List.of("queen", "dama");
            case KING -> List.of("king", "korol");
            case ACE -> List.of("ace", "tuz");
            default -> List.of();
        };
    }

    public static List<String> getRankSuffixes(BlackjackCard.Rank rank) {
        return switch (rank) {
            case JACK -> List.of("joker", "jack", "valet");
            case QUEEN -> List.of("queen", "dama");
            case KING -> List.of("king", "korol");
            case ACE -> List.of("ace", "tuz");
            default -> List.of();
        };
    }

    public static int getNumberAmount(PlayingCard.Rank rank) {
        return switch (rank) {
            case TWO -> 2;
            case THREE -> 3;
            case FOUR -> 4;
            case FIVE -> 5;
            case SIX -> 6;
            case SEVEN -> 7;
            case EIGHT -> 8;
            case NINE -> 9;
            case TEN -> 10;
            default -> 1;
        };
    }

    public static int getNumberAmount(BlackjackCard.Rank rank) {
        return switch (rank) {
            case TWO -> 2;
            case THREE -> 3;
            case FOUR -> 4;
            case FIVE -> 5;
            case SIX -> 6;
            case SEVEN -> 7;
            case EIGHT -> 8;
            case NINE -> 9;
            case TEN -> 10;
            default -> 1;
        };
    }

    public static ItemBuilder build(LoveActivities plugin, PlayingCard card) {
        if (card == null) {
            return buildBack(plugin);
        }
        boolean face = isFace(card.getRank());
        int amount = face ? 1 : getNumberAmount(card.getRank());
        String suitKey = getSuitKey(card.getSuit());
        List<String> rankSuffixes = getRankSuffixes(card.getRank());

        ItemStack item = resolveCard(plugin, suitKey, rankSuffixes, face, amount);
        ItemBuilder builder;
        if (item != null) {
            builder = ItemBuilder.from(item);
        } else {
            String b64 = plugin.getHeadManager().getTexture("cards." + card.getRank().getHeadKey());
            if (b64 != null && !b64.isEmpty()) {
                builder = ItemBuilder.base64Head(b64).amount(amount);
            } else {
                builder = ItemBuilder.from(Material.PAPER).amount(amount);
            }
        }
        return builder.name(card.getFormattedName());
    }

    public static ItemBuilder build(LoveActivities plugin, BlackjackCard card) {
        if (card == null) {
            return buildBack(plugin);
        }
        boolean face = isFace(card.getRank());
        int amount = face ? 1 : getNumberAmount(card.getRank());
        String suitKey = getSuitKey(card.getSuit());
        List<String> rankSuffixes = getRankSuffixes(card.getRank());

        ItemStack item = resolveCard(plugin, suitKey, rankSuffixes, face, amount);
        ItemBuilder builder;
        if (item != null) {
            builder = ItemBuilder.from(item);
        } else {
            String b64 = plugin.getHeadManager().getTexture("cards." + card.getRank().getHeadKey());
            if (b64 != null && !b64.isEmpty()) {
                builder = ItemBuilder.base64Head(b64).amount(amount);
            } else {
                builder = ItemBuilder.from(Material.PAPER).amount(amount);
            }
        }
        return builder.name(card.getFormattedName());
    }

    public static ItemBuilder buildBack(LoveActivities plugin) {
        return buildBack(plugin, 1);
    }

    public static ItemBuilder buildBack(LoveActivities plugin, int amount) {
        String userNamespace = plugin != null ? plugin.getConfig().getString("itemsadder.namespace", "") : "";
        String configured = plugin != null ? plugin.getConfig().getString("itemsadder.cards.back", "cardback") : "cardback";

        List<String> backCandidates = List.of(configured, "cardback", "card_back", "rubashka", "back");
        for (String id : backCandidates) {
            ItemStack item = findCustomStack(id, userNamespace);
            if (item != null) {
                item.setAmount(Math.max(1, amount));
                return ItemBuilder.from(item);
            }
        }
        return plugin.getHeadManager().createBuilder("cards.card_back").amount(amount);
    }

    private static ItemStack resolveCard(LoveActivities plugin, String suitKey, List<String> rankSuffixes, boolean isFace, int amount) {
        if (plugin != null && !plugin.getConfig().getBoolean("itemsadder.enabled", true)) {
            return null;
        }

        String userNamespace = plugin != null ? plugin.getConfig().getString("itemsadder.namespace", "") : "";

        if (!isFace) {
            // Numbered card (2..10): item is the suit name itself (bubna, chirva, chresta, pika)
            // with amount set to the card's number!
            String configKey = "itemsadder.cards.suits." + suitKey;
            String configuredId = plugin != null ? plugin.getConfig().getString(configKey, suitKey) : suitKey;

            List<String> candidates = new ArrayList<>();
            candidates.add(configuredId);
            if (!configuredId.equalsIgnoreCase(suitKey)) {
                candidates.add(suitKey);
            }
            candidates.add("card_" + suitKey);

            for (String candidate : candidates) {
                ItemStack item = findCustomStack(candidate, userNamespace);
                if (item != null) {
                    item.setAmount(Math.max(1, amount));
                    return item;
                }
            }
        } else {
            // Face card: 1 pcs. Names like bubnajoker, bubnaqueen, bubnaking, bubnaace
            List<String> candidates = new ArrayList<>();

            String primaryRank = rankSuffixes.isEmpty() ? "" : rankSuffixes.get(0);
            String configRankKey = switch (primaryRank) {
                case "joker", "jack", "valet" -> "jack";
                case "queen", "dama" -> "queen";
                case "king", "korol" -> "king";
                case "ace", "tuz" -> "ace";
                default -> primaryRank;
            };

            String configKey = "itemsadder.cards.faces." + configRankKey + "." + suitKey;
            if (plugin != null && plugin.getConfig().contains(configKey)) {
                candidates.add(plugin.getConfig().getString(configKey));
            }

            // User patterns: e.g. bubnajoker, bubnaqueen, bubnaace, bubnaking
            for (String suffix : rankSuffixes) {
                candidates.add(suitKey + suffix);           // bubnajoker, bubnaace
                candidates.add(suitKey + "_" + suffix);     // bubna_joker
                candidates.add(suffix + "_" + suitKey);     // joker_bubna
                candidates.add("card_" + suitKey + "_" + suffix); // card_bubna_joker
                candidates.add("card_" + suffix + "_" + suitKey); // card_joker_bubna
                candidates.add(suffix);                     // joker
            }

            for (String candidate : candidates) {
                ItemStack item = findCustomStack(candidate, userNamespace);
                if (item != null) {
                    item.setAmount(1);
                    return item;
                }
            }
        }

        return null;
    }
}
