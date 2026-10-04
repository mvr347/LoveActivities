package dev.lovelace.loveactivities.util;

/**
 * Slot arithmetic of the 9-slot confirmation menus (gui_gen v2.1 header only): slot 0 is the description,
 * slots 1 and 8 are glass and the buttons sit in 2-7, centred with equal gaps by their number.
 */
public final class MenuLayout {

    public static final int SIZE = 9;

    private MenuLayout() {}

    /** Slots 2-7 used by {@code count} buttons (1..6), centred. */
    public static int[] controlSlots(int count) {
        return switch (count) {
            case 1 -> new int[]{4};
            case 2 -> new int[]{3, 5};
            case 3 -> new int[]{2, 4, 6};
            case 4 -> new int[]{2, 3, 5, 6};
            case 5 -> new int[]{2, 3, 4, 6, 7};
            case 6 -> new int[]{2, 3, 4, 5, 6, 7};
            default -> throw new IllegalArgumentException("buttons must be 1..6, got " + count);
        };
    }
}
