package dev.lovelace.loveactivities.util;

import java.util.ArrayList;
import java.util.List;

/** Lore lines for the stakes of a game: "Ставки:" and one line per coin type, like the LoveShop price menus. */
public final class BetLore {

    private BetLore() {}

    /** The stakes lines followed by {@code rest}, as the varargs array of {@code ItemBuilder#lore(String...)}. */
    public static String[] withRest(long total, String... rest) {
        List<String> all = new ArrayList<>(lines(total));
        all.addAll(java.util.Arrays.asList(rest));
        return all.toArray(new String[0]);
    }

    /** Both players' stakes together; no stake gives a single "Ставки: нет" line. */
    public static List<String> lines(long total) {
        List<String> lines = new ArrayList<>();
        if (total <= 0) {
            lines.add("<gray>Ставки: <white>нет</white></gray>");
            return lines;
        }
        lines.add("<gray>Ставки:</gray>");
        lines.addAll(CurrencyUtil.formatCoinLines(total));
        return lines;
    }
}
