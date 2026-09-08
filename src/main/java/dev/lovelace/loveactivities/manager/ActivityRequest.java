package dev.lovelace.loveactivities.manager;

import dev.lovelace.loveactivities.api.GameType;

import java.util.UUID;

public class ActivityRequest {

    private final String id;
    private final UUID sender;
    private final UUID receiver;
    private final GameType gameType;
    private final String subMode;
    private final long createdAt;
    private final long expiresAt;

    public ActivityRequest(UUID sender, UUID receiver, GameType gameType, long durationSeconds) {
        this(sender, receiver, gameType, null, durationSeconds);
    }

    public ActivityRequest(UUID sender, UUID receiver, GameType gameType, String subMode, long durationSeconds) {
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.sender = sender;
        this.receiver = receiver;
        this.gameType = gameType;
        this.subMode = subMode;
        this.createdAt = System.currentTimeMillis();
        this.expiresAt = this.createdAt + (durationSeconds * 1000L);
    }

    public String getId() {
        return id;
    }

    public UUID getSender() {
        return sender;
    }

    public UUID getReceiver() {
        return receiver;
    }

    public GameType getGameType() {
        return gameType;
    }

    public String getSubMode() {
        return subMode;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }
}
