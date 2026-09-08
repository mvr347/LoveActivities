package dev.lovelace.loveactivities.database;

import dev.lovelace.loveactivities.api.GameType;

import java.util.*;

public class PlayerSettings {

    private final UUID uuid;
    private boolean dnd;
    private boolean sounds;
    private boolean particles;
    private final Set<GameType> blacklistedGames = EnumSet.noneOf(GameType.class);

    public PlayerSettings(UUID uuid) {
        this.uuid = uuid;
        this.dnd = false;
        this.sounds = true;
        this.particles = true;
    }

    public PlayerSettings(UUID uuid, boolean dnd, boolean sounds, boolean particles, Set<GameType> blacklisted) {
        this.uuid = uuid;
        this.dnd = dnd;
        this.sounds = sounds;
        this.particles = particles;
        if (blacklisted != null) {
            this.blacklistedGames.addAll(blacklisted);
        }
    }

    public UUID getUuid() {
        return uuid;
    }

    public boolean isDnd() {
        return dnd;
    }

    public void setDnd(boolean dnd) {
        this.dnd = dnd;
    }

    public boolean isSounds() {
        return sounds;
    }

    public void setSounds(boolean sounds) {
        this.sounds = sounds;
    }

    public boolean isParticles() {
        return particles;
    }

    public void setParticles(boolean particles) {
        this.particles = particles;
    }

    public Set<GameType> getBlacklistedGames() {
        return blacklistedGames;
    }

    public boolean isGameBlacklisted(GameType game) {
        return blacklistedGames.contains(game);
    }

    public void toggleGameBlacklist(GameType game) {
        if (blacklistedGames.contains(game)) {
            blacklistedGames.remove(game);
        } else {
            blacklistedGames.add(game);
        }
    }

    public void setAllGamesBlacklisted(boolean blacklisted) {
        if (blacklisted) {
            blacklistedGames.addAll(EnumSet.allOf(GameType.class));
        } else {
            blacklistedGames.clear();
        }
    }

    public String serializeBlacklist() {
        StringBuilder sb = new StringBuilder();
        for (GameType game : blacklistedGames) {
            if (!sb.isEmpty()) sb.append(",");
            sb.append(game.getId());
        }
        return sb.toString();
    }

    public static Set<GameType> deserializeBlacklist(String data) {
        Set<GameType> set = EnumSet.noneOf(GameType.class);
        if (data == null || data.trim().isEmpty()) return set;
        String[] parts = data.split(",");
        for (String part : parts) {
            GameType game = GameType.fromString(part.trim());
            if (game != null) {
                set.add(game);
            }
        }
        return set;
    }
}
