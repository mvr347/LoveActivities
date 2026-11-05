package dev.lovelace.loveactivities.command;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class BackLabelTest {
    @Test
    void russianAndNamespacedLabelsAreAccepted() {
        for (String ok : new String[] {"настолки", "активности", "loveessential:profile", "profile", "menu-2"})
            assertTrue(PlayerActivityCommand.BACK_LABEL.matcher(ok).matches(), ok);
    }

    @Test
    void anythingThatCouldChainACommandIsRejected() {
        for (String bad : new String[] {"", "a b", "a;b", "/profile", "a\nb", "x".repeat(65)})
            assertFalse(PlayerActivityCommand.BACK_LABEL.matcher(bad).matches(), bad);
    }
}
