package dev.lovelace.loveactivities.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NpcNamesTest {

    @Test
    void plainNameGetsGreenPrefix() {
        assertEquals("&aКрупье", NpcNames.normalize("Крупье"));
    }

    @Test
    void gradientsAndBracketsAreStripped() {
        assertEquals("&aКрупье", NpcNames.normalize("<gradient:#FF9966:#FF5E62>[Крупье]</gradient>"));
    }

    @Test
    void otherColoursAndFormattingAreReplacedByGreen() {
        assertEquals("&aКрупье Ганс", NpcNames.normalize("&c&lКрупье &6Ганс"));
        assertEquals("&aКрупье", NpcNames.normalize("§4Крупье"));
        assertEquals("&aКрупье", NpcNames.normalize("&#FF5E62Крупье"));
    }

    @Test
    void alreadyNormalizedStaysTheSame() {
        assertEquals("&aКрупье", NpcNames.normalize("&aКрупье"));
        assertEquals("&aКрупье", NpcNames.normalize(NpcNames.normalize("[Крупье]")));
    }

    @Test
    void blankFallsBackToDefault() {
        assertEquals("&a" + NpcNames.DEFAULT, NpcNames.normalize(null));
        assertEquals("&a" + NpcNames.DEFAULT, NpcNames.normalize("  "));
        assertEquals("&a" + NpcNames.DEFAULT, NpcNames.normalize("<red></red>[]"));
    }

    @Test
    void whitespaceIsCollapsed() {
        assertEquals("&aДилер Боб", NpcNames.normalize("  Дилер   Боб \n"));
    }
}
