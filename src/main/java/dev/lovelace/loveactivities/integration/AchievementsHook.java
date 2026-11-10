package dev.lovelace.loveactivities.integration;

import dev.lovelace.lovecore.api.LoveCore;
import dev.lovelace.lovecore.api.achievements.Achievements;

import java.util.UUID;

/**
 * Feeds LoveCore's shared achievements with finished activity games. Looked up on every call; no LoveCore, an older
 * one without the contract ({@link LinkageError}) or no service means nothing is recorded. Main thread only.
 */
public final class AchievementsHook {

    public static final String PLAYED = "activities.played";
    public static final String WON = "activities.won";

    private AchievementsHook() {
    }

    /** A finished game: every human player played it, the human winner (if any) won it. */
    public static void gameFinished(UUID player1, UUID player2, UUID winner) {
        try {
            LoveCore.service(Achievements.class).ifPresent(service -> Gateway.record(service, player1, player2, winner));
        } catch (LinkageError missingContract) {
            // an older LoveCore: nothing to record
        }
    }

    /** Isolated so the contract class is only resolved inside the try/catch above. */
    static final class Gateway {
        private Gateway() {
        }

        static void record(Achievements service, UUID player1, UUID player2, UUID winner) {
            if (isHuman(player1)) service.progress(player1, PLAYED, 1);
            if (isHuman(player2) && !player2.equals(player1)) service.progress(player2, PLAYED, 1);
            if (isHuman(winner)) service.progress(winner, WON, 1);
        }

        private static boolean isHuman(UUID uuid) {
            return uuid != null && !dev.lovelace.loveactivities.manager.SessionManager.isNpc(uuid);
        }
    }
}
