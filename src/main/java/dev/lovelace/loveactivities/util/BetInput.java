package dev.lovelace.loveactivities.util;

/**
 * State of the stake picker, free of Bukkit so it can be tested. A stake is built from coin values the way the
 * LoveShop price picker does it: Shift cycles the active coin, left click adds one of it, right click takes one
 * away. Unlike a price, a stake of 0 is valid (a friendly game); the maximum is the balance / NPC limit.
 */
public final class BetInput {

    private final long[] units;
    private final long max;
    private int active;
    private long bet;

    /**
     * @param units coin values, smallest first; empty means a plain step of 1
     * @param max   the highest stake that may be set (0 = no betting)
     * @param start the stake to start from, clamped into [0, max]
     */
    public BetInput(long[] units, long max, long start) {
        this.units = units.length == 0 ? new long[]{1L} : units.clone();
        this.max = Math.max(0L, max);
        this.bet = Math.max(0L, Math.min(this.max, start));
    }

    public long bet() { return bet; }

    public long max() { return max; }

    public long activeUnit() { return units[active]; }

    /** Makes the smallest coin worth at least {@code minUnit} active (starting on copper would take hundreds of clicks). */
    public void startAtUnit(long minUnit) {
        for (int i = 0; i < units.length; i++) {
            if (units[i] >= minUnit) {
                active = i;
                return;
            }
        }
    }

    public void cycle() {
        active = (active + 1) % units.length;
    }

    /** @return false when the stake did not change (already at the maximum) */
    public boolean add() {
        long next = Math.min(max, bet + units[active]);
        boolean changed = next != bet;
        bet = next;
        return changed;
    }

    /** @return false when the stake did not change (already 0) */
    public boolean subtract() {
        long next = Math.max(0L, bet - units[active]);
        boolean changed = next != bet;
        bet = next;
        return changed;
    }

    public void reset() {
        bet = 0L;
    }
}
