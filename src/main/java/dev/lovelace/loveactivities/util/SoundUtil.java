package dev.lovelace.loveactivities.util;

import dev.lovelace.loveactivities.LoveActivities;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class SoundUtil {

    private static boolean shouldPlay(Player player) {
        if (player == null || !player.isOnline()) return false;
        if (!LoveActivities.getInstance().getConfigManager().isSoundsEnabled()) return false;
        return LoveActivities.getInstance().getSettingsManager().hasSounds(player.getUniqueId());
    }

    public static void playClick(Player player) {
        if (shouldPlay(player)) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.35f, 1.2f);
        }
    }

    public static void playSuccess(Player player) {
        if (shouldPlay(player)) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.35f, 1.4f);
        }
    }

    public static void playWin(Player player) {
        playSuccess(player);
    }

    public static void playLoss(Player player) {
        if (shouldPlay(player)) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.35f, 0.9f);
        }
    }

    public static void playDraw(Player player) {
        if (shouldPlay(player)) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.35f, 1.0f);
        }
    }

    public static void playError(Player player) {
        if (shouldPlay(player)) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.35f, 0.6f);
        }
    }

    public static void playChallenge(Player player) {
        if (shouldPlay(player)) {
            player.playSound(player.getLocation(), Sound.BLOCK_BELL_USE, 0.4f, 1.3f);
        }
    }

    public static void playCardDraw(Player player) {
        if (shouldPlay(player)) {
            player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.4f, 1.1f);
        }
    }

    public static void playDiceRoll(Player player) {
        if (shouldPlay(player)) {
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.4f, 0.8f);
        }
    }

    public static void playMove(Player player) {
        if (shouldPlay(player)) {
            player.playSound(player.getLocation(), Sound.BLOCK_WOODEN_BUTTON_CLICK_ON, 0.35f, 1.2f);
        }
    }

    public static void playCountdownTick(Player player) {
        if (shouldPlay(player)) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.35f, 1.2f);
        }
    }

    public static void playCountdownStart(Player player) {
        if (shouldPlay(player)) {
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.4f, 1.2f);
        }
    }

    public static void playTimerWarning15(Player player) {
        if (shouldPlay(player)) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.6f, 1.0f);
        }
    }

    public static void playTimerWarning10(Player player) {
        if (shouldPlay(player)) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.7f, 1.3f);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 1.2f);
        }
    }

    public static void playTimerCountdown(Player player, int seconds) {
        if (shouldPlay(player)) {
            float pitch = switch (seconds) {
                case 5 -> 1.2f;
                case 4 -> 1.4f;
                case 3 -> 1.6f;
                case 2 -> 1.8f;
                case 1 -> 2.0f;
                default -> 1.0f;
            };
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.75f, pitch);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.5f, pitch);
        }
    }

    public static void playTimerAlert(Player player, int seconds) {
        if (player == null || !player.isOnline()) return;
        if (seconds == 15) {
            playTimerWarning15(player);
            player.sendActionBar(TextUtil.parse("<yellow>⏳ До конца хода осталось <gold><bold>15</bold></gold> секунд!</yellow>"));
        } else if (seconds == 10) {
            playTimerWarning10(player);
            player.sendActionBar(TextUtil.parse("<gold>⏳ Внимание: осталось <red><bold>10</bold></red> секунд!</gold>"));
        } else if (seconds >= 1 && seconds <= 5) {
            playTimerCountdown(player, seconds);
            player.sendActionBar(TextUtil.parse("<red><bold>⚠ Ход завершится через " + seconds + " сек! ⚠</bold></red>"));
        }
    }
}
