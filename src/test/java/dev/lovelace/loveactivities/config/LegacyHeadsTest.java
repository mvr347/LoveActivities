package dev.lovelace.loveactivities.config;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LegacyHeadsTest {

    private static String base64(String hash) {
        String json = "{\"textures\":{\"SKIN\":{\"url\":\"http://textures.minecraft.net/texture/" + hash + "\"}}}";
        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    private static final String OLD = base64("64960113073b52044d7079965419621922318a9e99d767170b1428ded48c7ce5");
    private static final String NEW = base64("c245036782c6a7e25ff84c5087a65966d650c033ba37e00965e8b99021b3ecae");

    @Test
    void theOldShippedMoveHeadIsReplaced() {
        assertEquals(NEW, LegacyHeads.upgrade("chess.valid_move", OLD, NEW));
    }

    @Test
    void anOwnerHeadStays() {
        String own = base64("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
        assertEquals(own, LegacyHeads.upgrade("chess.valid_move", own, NEW));
    }

    @Test
    void otherKeysAreUntouched() {
        assertEquals(OLD, LegacyHeads.upgrade("chess.tile_white", OLD, NEW));
        assertEquals(OLD, LegacyHeads.upgrade("chess.valid_move", OLD, null));
    }
}
