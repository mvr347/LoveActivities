package dev.lovelace.loveactivities.database;

import dev.lovelace.loveactivities.api.GameType;

import java.util.UUID;

public class PlayerStats {

    private final UUID uuid;
    private final GameType game;
    private int wins;
    private int losses;
    private long wonMoney;
    private long lostMoney;

    public PlayerStats(UUID uuid, GameType game) {
        this.uuid = uuid;
        this.game = game;
        this.wins = 0;
        this.losses = 0;
        this.wonMoney = 0;
        this.lostMoney = 0;
    }

    public PlayerStats(UUID uuid, GameType game, int wins, int losses, long wonMoney, long lostMoney) {
        this.uuid = uuid;
        this.game = game;
        this.wins = wins;
        this.losses = losses;
        this.wonMoney = wonMoney;
        this.lostMoney = lostMoney;
    }

    public UUID getUuid() {
        return uuid;
    }

    public GameType getGame() {
        return game;
    }

    public int getWins() {
        return wins;
    }

    public void addWin(long money) {
        this.wins++;
        this.wonMoney += Math.max(0, money);
    }

    public int getLosses() {
        return losses;
    }

    public void addLoss(long money) {
        this.losses++;
        this.lostMoney += Math.max(0, money);
    }

    public long getWonMoney() {
        return wonMoney;
    }

    public long getLostMoney() {
        return lostMoney;
    }

    public int getTotalGames() {
        return wins + losses;
    }

    public double getWinrate() {
        int total = getTotalGames();
        if (total == 0) return 0.0;
        return ((double) wins / total) * 100.0;
    }
}
