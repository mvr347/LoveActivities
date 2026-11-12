package dev.lovelace.loveactivities.config;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Swaps a head that an installed {@code heads.yml} still holds from an older release for the current one. The file
 * on disk beats the jar defaults, so without this a new texture never reaches a running server. Only a value equal
 * to the OLD shipped texture is replaced: a head the owner set themselves stays.
 */
final class LegacyHeads {

    private LegacyHeads() {
    }

    /** Key to {old hash to new hash}. */
    private static final Map<String, Map<String, String>> LEGACY = Map.of(
            "chess.valid_move", Map.of(
                    "64960113073b52044d7079965419621922318a9e99d767170b1428ded48c7ce5",
                    "c245036782c6a7e25ff84c5087a65966d650c033ba37e00965e8b99021b3ecae"));

    private static final Pattern HASH = Pattern.compile("texture/([0-9a-f]+)");

    /** The head to use for a key: the new default if the configured value is the old shipped one. */
    static String upgrade(String key, String configured, String currentDefault) {
        Map<String, String> table = LEGACY.get(key);
        if (table == null || configured == null || currentDefault == null) {
            return configured;
        }
        String hash = hashOf(configured);
        return hash != null && table.containsKey(hash) ? currentDefault : configured;
    }

    private static String hashOf(String value) {
        String v = value.strip();
        var direct = HASH.matcher(v);
        if (direct.find()) {
            return direct.group(1);
        }
        try {
            var decoded = new String(Base64.getDecoder().decode(v), StandardCharsets.UTF_8);
            var m = HASH.matcher(decoded);
            return m.find() ? m.group(1) : null;
        } catch (IllegalArgumentException notBase64) {
            return v.matches("[0-9a-f]{20,}") ? v : null;
        }
    }
}
