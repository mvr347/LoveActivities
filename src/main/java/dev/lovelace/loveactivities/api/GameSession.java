package dev.lovelace.loveactivities.api;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.UUID;

public interface GameSession {

    UUID getSessionId();

    GameType getGameType();

    UUID getPlayer1();

    UUID getPlayer2();

    long getBet();

    void setBet(long bet);

    GameState getState();

    void setState(GameState state);

    void startGame();

    void onPlayerClick(Player player, int slot, ClickType clickType);

    void onPlayerClose(Player player);

    void autoLose(UUID loser, String reasonKey);

    void endWithWinner(UUID winner);

    void endWithDraw();

    void cancelAndRefund(String reasonKey);

    void tickTurnTimer();

    long getLastActionTime();

    void updateLastActionTime();

    boolean containsPlayer(UUID uuid);

    UUID getOpponent(UUID uuid);
}
