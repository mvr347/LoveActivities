package dev.lovelace.loveactivities.manager;

import dev.lovelace.loveactivities.api.GameType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class NpcBindingKeyTest {

    @Test
    void bindingIdIsStableForTheSameNpcKey() {
        assertEquals(NpcManager.idForKey("ext-1a2b3c4d"), NpcManager.idForKey("ext-1a2b3c4d"));
    }

    @Test
    void differentNpcKeysGetDifferentIds() {
        assertNotEquals(NpcManager.idForKey("ext-1a2b3c4d"), NpcManager.idForKey("ext-5e6f7a8b"));
        assertNotEquals(NpcManager.idForKey("7"), NpcManager.idForKey("8"));
    }

    @Test
    void bindingIdNeverCollidesWithAnEntityUuidInPractice() {
        UUID entity = UUID.fromString("00000000-0000-0000-0000-000000000001");
        assertNotEquals(entity, NpcManager.idForKey("1"));
    }

    @Test
    void theProviderIsLoveEssential() {
        assertEquals("loveessential", NpcManager.PROVIDER_ID);
    }

    @Test
    void anOrdinaryEntityBindingHasNoNpcKey() {
        NpcActivityConfig config = new NpcActivityConfig(UUID.randomUUID(), "Крупье", GameType.BLACKJACK, 10);
        assertNull(config.getNpcKey());
        config.setNpcKey("ext-1a2b3c4d");
        assertEquals("ext-1a2b3c4d", config.getNpcKey());
    }
}
