package dev.lovelace.loveactivities.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BetInputTest {

    private static final long[] COINS = {1, 100, 2000, 20000};

    @Test
    void addsActiveCoinUpToMax() {
        BetInput in = new BetInput(COINS, 250, 0);
        in.startAtUnit(100);
        assertTrue(in.add());
        assertTrue(in.add());
        assertEquals(200, in.bet());
        assertTrue(in.add());
        assertEquals(250, in.bet());
        assertFalse(in.add(), "already at the maximum");
    }

    @Test
    void subtractStopsAtZero() {
        BetInput in = new BetInput(COINS, 1000, 150);
        in.startAtUnit(100);
        assertTrue(in.subtract());
        assertEquals(50, in.bet());
        assertTrue(in.subtract());
        assertEquals(0, in.bet());
        assertFalse(in.subtract());
    }

    @Test
    void cycleWrapsAround() {
        BetInput in = new BetInput(COINS, 100000, 0);
        for (int i = 0; i < COINS.length; i++) in.cycle();
        assertEquals(1, in.activeUnit());
    }

    @Test
    void startIsClampedAndNoBettingMeansMaxZero() {
        assertEquals(500, new BetInput(COINS, 500, 9999).bet());
        BetInput none = new BetInput(COINS, 0, 100);
        assertEquals(0, none.bet());
        assertFalse(none.add());
    }
}
