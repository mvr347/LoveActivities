package dev.lovelace.loveactivities.api.events;

import dev.lovelace.loveactivities.api.GameSession;
import dev.lovelace.loveactivities.api.GameType;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class LoveActivityWinEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final GameSession session;
    private final GameType gameType;
    private final UUID winnerUuid;
    private final UUID loserUuid;
    private final long totalWinnings;
    private final boolean againstNpc;
    private final String npcName;

    public LoveActivityWinEvent(GameSession session, GameType gameType, UUID winnerUuid, UUID loserUuid,
                                long totalWinnings, boolean againstNpc, @Nullable String npcName) {
        this.session = session;
        this.gameType = gameType;
        this.winnerUuid = winnerUuid;
        this.loserUuid = loserUuid;
        this.totalWinnings = totalWinnings;
        this.againstNpc = againstNpc;
        this.npcName = npcName;
    }

    public GameSession getSession() {
        return session;
    }

    public GameType getGameType() {
        return gameType;
    }

    public UUID getWinnerUuid() {
        return winnerUuid;
    }

    public UUID getLoserUuid() {
        return loserUuid;
    }

    public long getTotalWinnings() {
        return totalWinnings;
    }

    public boolean isAgainstNpc() {
        return againstNpc;
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
