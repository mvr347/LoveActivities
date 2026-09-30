package dev.lovelace.loveactivities.util;

import java.util.regex.Pattern;

/**
 * The name of a game NPC is always plain green text: {@code &a<name>}, with no gradient, no other
 * colours and no square brackets. Whatever an administrator types (or an old npcs.yml holds) is
 * reduced to that.
 */
public final class NpcNames {

    public static final String PREFIX = "&a";
    public static final String DEFAULT = "NPC Игрок";

    private static final Pattern MINIMESSAGE_TAG = Pattern.compile("</?[a-zA-Z#][^<>]*>");
    private static final Pattern LEGACY_CODE = Pattern.compile("[&§][0-9a-fk-orA-FK-OR]");
    private static final Pattern HEX_CODE = Pattern.compile("[&§]#[0-9a-fA-F]{6}");

    private NpcNames() {
    }

    /** The bare name: no tags, no colour codes, no brackets, single-spaced. Never empty. */
    public static String plain(String raw) {
        String s = raw == null ? "" : raw;
        s = MINIMESSAGE_TAG.matcher(s).replaceAll("");
        s = HEX_CODE.matcher(s).replaceAll("");
        s = LEGACY_CODE.matcher(s).replaceAll("");
        s = s.replace("[", "").replace("]", "");
        s = s.replaceAll("\\s+", " ").trim();
        return s.isEmpty() ? DEFAULT : s;
    }

    /** {@code &a} + the bare name: the form stored in npcs.yml and shown to players. */
    public static String normalize(String raw) {
        return PREFIX + plain(raw);
    }
}
