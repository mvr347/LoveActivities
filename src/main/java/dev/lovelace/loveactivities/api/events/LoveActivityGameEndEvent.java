package dev.lovelace.loveactivities.api.events;

import dev.lovelace.loveactivities.api.GameSession;
import dev.lovelace.loveactivities.api.GameType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class LoveActivityGameEndEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final GameSession session;
    private final GameType gameType;
    private final UUID player1;
    private final UUID player2;
    private final UUID winnerUuid;
    private final long bet;
    private final boolean isNpcMatch;
    private final String npcName;

    public LoveActivityGameEndEvent(GameSession session, GameType gameType, UUID player1, UUID player2,
                                    @Nullable UUID winnerUuid, long bet, boolean isNpcMatch, @Nullable String npcName) {
        this.session = session;
        this.gameType = gameType;
        this.player1 = player1;
        this.player2 = player2;
        this.winnerUuid = winnerUuid;
        this.bet = bet;
        this.isNpcMatch = isNpcMatch;
        this.npcName = npcName;
    }

    public GameSession getSession() {
        return session;
    }

    public GameType getGameType() {
        return gameType;
    }

    public UUID getPlayer1() {
        return player1;
    }

    public UUID getPlayer2() {
        return player2;
    }

    public @Nullable UUID getWinnerUuid() {
        return winnerUuid;
    }

    public boolean isDraw() {
        return winnerUuid == null;
    }

    public long getBet() {
        return bet;
    }

    public boolean isNpcMatch() {
        return isNpcMatch;
    }

    public @Nullable String getNpcName() {
        return npcName;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
