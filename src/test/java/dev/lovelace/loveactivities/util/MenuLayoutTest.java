package dev.lovelace.loveactivities.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MenuLayoutTest {

    @Test
    void buttonsAreCentredInSlots2To7() {
        assertArrayEquals(new int[]{4}, MenuLayout.controlSlots(1));
        assertArrayEquals(new int[]{3, 5}, MenuLayout.controlSlots(2));
        assertArrayEquals(new int[]{2, 4, 6}, MenuLayout.controlSlots(3));
        assertArrayEquals(new int[]{2, 3, 5, 6}, MenuLayout.controlSlots(4));
    }

    @Test
    void outOfRangeCountIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> MenuLayout.controlSlots(0));
        assertThrows(IllegalArgumentException.class, () -> MenuLayout.controlSlots(7));
    }
}
