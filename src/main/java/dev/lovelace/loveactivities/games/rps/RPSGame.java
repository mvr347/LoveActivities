package dev.lovelace.loveactivities.games.rps;

import dev.lovelace.loveactivities.LoveActivities;
import dev.lovelace.loveactivities.api.GameSession;
import dev.lovelace.loveactivities.api.GameState;
import dev.lovelace.loveactivities.api.GameType;
import dev.lovelace.loveactivities.gui.AbstractGUI;
import dev.lovelace.loveactivities.util.SoundUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class RPSGame implements GameSession {

    private final LoveActivities plugin = LoveActivities.getInstance();
    private final UUID sessionId = UUID.randomUUID();
    private final UUID player1;
    private final UUID player2;
    private long bet;
    private GameState state = GameState.PLAYING;
    private long lastActionTime = System.currentTimeMillis();

    private int scoreP1 = 0;
    private int scoreP2 = 0;
    private final int targetScore = 2;
    private int currentRound = 1;

    private RPSChoice choiceP1 = null;
    private RPSChoice choiceP2 = null;

    private boolean countingDown = false;
    private boolean revealing = false;
    private int countdown = 3;
    private BukkitTask countdownTask = null;
    private String lastRoundResultText = "";

    private RPSGUI guiP1;
    private RPSGUI guiP2;

    public RPSGame(Player p1, Player p2, long bet) {
        this(p1 != null ? p1.getUniqueId() : null, p2 != null ? p2.getUniqueId() : null, bet);
    }

    public RPSGame(UUID p1, UUID p2, long bet) {
        this.player1 = p1;
        this.player2 = p2;
        this.bet = bet;
    }

    @Override
    public UUID getSessionId() {
        return sessionId;
    }

    @Override
    public GameType getGameType() {
        return GameType.RPS;
    }

    @Override
    public UUID getPlayer1() {
        return player1;
    }

    @Override
    public UUID getPlayer2() {
        return player2;
    }

    @Override
    public long getBet() {
        return bet;
    }

    @Override
    public void setBet(long bet) {
        this.bet = bet;
    }

    @Override
    public GameState getState() {
        return state;
    }

    @Override
    public void setState(GameState state) {
        this.state = state;
    }

    @Override
    public void startGame() {
        Player p1 = Bukkit.getPlayer(player1);
        Player p2 = (player2 != null && !dev.lovelace.loveactivities.manager.SessionManager.isNpc(player2)) ? Bukkit.getPlayer(player2) : null;

        if (p1 != null && p1.isOnline()) {
            this.guiP1 = new RPSGUI(p1, this);
            this.guiP1.open();
        }
        if (p2 != null && p2.isOnline()) {
            this.guiP2 = new RPSGUI(p2, this);
            this.guiP2.open();
        }
    }

    public int getScoreP1() {
        return scoreP1;
    }

    public int getScoreP2() {
        return scoreP2;
    }

    public int getTargetScore() {
        return targetScore;
    }

    public int getCurrentRound() {
        return currentRound;
    }

    public RPSChoice getChoiceP1() {
        return choiceP1;
    }

    public RPSChoice getChoiceP2() {
        return choiceP2;
    }

    public boolean isCountingDown() {
        return countingDown;
    }

    public boolean isRevealing() {
        return revealing;
    }

    public int getCountdown() {
        return countdown;
    }

    public String getLastRoundResultText() {
        return lastRoundResultText;
    }

    public synchronized void makeChoice(Player player, RPSChoice choice) {
        if (state != GameState.PLAYING || countingDown || revealing) return;
        updateLastActionTime();

        boolean isP1 = player.getUniqueId().equals(player1);
        if (isP1) {
            choiceP1 = choice;
            if (dev.lovelace.loveactivities.manager.SessionManager.isNpc(player2)) {
                RPSChoice[] choices = RPSChoice.values();
                choiceP2 = choices[new Random().nextInt(choices.length)];
            }
        } else {
            choiceP2 = choice;
        }

        SoundUtil.playClick(player);
        syncViews();

        if (choiceP1 != null && choiceP2 != null) {
            startCountdown();
        }
    }

    private void startCountdown() {
        countingDown = true;
        countdown = 3;

        countdownTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (state != GameState.PLAYING) {
                if (countdownTask != null) countdownTask.cancel();
                return;
            }

            Player p1 = Bukkit.getPlayer(player1);
            Player p2 = Bukkit.getPlayer(player2);

            if (countdown > 0) {
                SoundUtil.playCountdownTick(p1);
                SoundUtil.playCountdownTick(p2);
                syncViews();
                countdown--;
            } else {
                if (countdownTask != null) {
                    countdownTask.cancel();
                    countdownTask = null;
                }
                countingDown = false;
                evaluateRound();
            }
        }, 20L, 20L);
    }

    private void evaluateRound() {
        revealing = true;
        Player p1 = Bukkit.getPlayer(player1);
        Player p2 = Bukkit.getPlayer(player2);

        int cmp = choiceP1.compareChoice(choiceP2);
        if (cmp > 0) {
            scoreP1++;
            lastRoundResultText = "Победа " + (p1 != null ? p1.getName() : "P1") + "!";
            SoundUtil.playSuccess(p1);
            SoundUtil.playLoss(p2);
        } else if (cmp < 0) {
            scoreP2++;
            lastRoundResultText = "Победа " + (p2 != null ? p2.getName() : "P2") + "!";
            SoundUtil.playSuccess(p2);
            SoundUtil.playLoss(p1);
        } else {
            lastRoundResultText = "Ничья в раунде!";
            SoundUtil.playDraw(p1);
            SoundUtil.playDraw(p2);
        }

        syncViews();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (state != GameState.PLAYING) return;

            if (scoreP1 >= targetScore) {
                endWithWinner(player1);
            } else if (scoreP2 >= targetScore) {
                endWithWinner(player2);
            } else {
                currentRound++;
                choiceP1 = null;
                choiceP2 = null;
                revealing = false;
                syncViews();
            }
        }, 50L);
    }

    public void resign(Player player) {
        if (state != GameState.PLAYING) return;
        UUID loser = player.getUniqueId();
        UUID winner = getOpponent(loser);

        Player p1 = Bukkit.getPlayer(player1);
        Player p2 = Bukkit.getPlayer(player2);
        if (p1 != null) plugin.getLocaleManager().send(p1, "autolose_surrender", Map.of("player", player.getName(), "game", "КНБ"));
        if (p2 != null) plugin.getLocaleManager().send(p2, "autolose_surrender", Map.of("player", player.getName(), "game", "КНБ"));

        endWithWinner(winner);
    }

    private void syncViews() {
        if (guiP1 != null) guiP1.initializeItems();
        if (guiP2 != null) guiP2.initializeItems();
    }

    @Override
    public void onPlayerClick(Player player, int slot, ClickType clickType) {}

    private final long sessionStartTime = System.currentTimeMillis();

    public void reopenGui(Player player) {
        RPSGUI gui = player.getUniqueId().equals(player1) ? guiP1 : guiP2;
        if (gui != null) {
            gui.setSwitchingInventory(true);
            gui.open();
        }
    }

    @Override
    public void onPlayerClose(Player player) {
        if (state != GameState.PLAYING) {
            return;
        }
        if (System.currentTimeMillis() - sessionStartTime < 2000L) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (state == GameState.PLAYING && player.isOnline()) {
                    if (!(player.getOpenInventory().getTopInventory().getHolder() instanceof AbstractGUI)) {
                        reopenGui(player);
                    }
                }
            }, 2L);
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (state != GameState.PLAYING) return;
            if (!player.isOnline()) {
                autoLose(player.getUniqueId(), "autolose_disconnect");
                return;
            }
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof AbstractGUI) {
                return;
            }
            this.state = GameState.FINISHED;
            autoLose(player.getUniqueId(), "autolose_gui_close");
        }, 15L);
    }

    private void closeAllGuis() {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        if (guiP1 != null) {
            guiP1.setSwitchingInventory(true);
            guiP1.setClosed(true);
            Player p = guiP1.getPlayer();
            if (p != null && p.isOnline()) p.closeInventory();
        }
        if (guiP2 != null) {
            guiP2.setSwitchingInventory(true);
            guiP2.setClosed(true);
            Player p = guiP2.getPlayer();
            if (p != null && p.isOnline()) p.closeInventory();
        }
    }

    @Override
    public void autoLose(UUID loser, String reasonKey) {
        if (state == GameState.CANCELLED) return;
        this.state = GameState.FINISHED;
        closeAllGuis();
        plugin.getSessionManager().handleAutoLose(this, loser, reasonKey);
    }

    @Override
    public void endWithWinner(UUID winner) {
        if (state == GameState.CANCELLED) return;
        this.state = GameState.FINISHED;
        closeAllGuis();
        plugin.getSessionManager().handleWinner(this, winner);
    }

    @Override
    public void endWithDraw() {
        if (state == GameState.CANCELLED) return;
        this.state = GameState.FINISHED;
        closeAllGuis();
        plugin.getSessionManager().handleDraw(this);
    }

    @Override
    public void cancelAndRefund(String reasonKey) {
        if (state == GameState.CANCELLED) return;
        this.state = GameState.CANCELLED;
        closeAllGuis();
        plugin.getSessionManager().handleCancelAndRefund(this, reasonKey);
    }

    @Override
    public void tickTurnTimer() {
        if (state != GameState.PLAYING || countingDown || revealing) return;
        long elapsed = (System.currentTimeMillis() - lastActionTime) / 1000L;
        if (elapsed > plugin.getConfigManager().getAfkTurnTimeoutSeconds()) {
            if (choiceP1 == null && choiceP2 != null) {
                autoLose(player1, "autolose_afk");
            } else if (choiceP2 == null && choiceP1 != null) {
                autoLose(player2, "autolose_afk");
            } else {
                autoLose(player1, "autolose_afk");
            }
        }
    }

    @Override
    public long getLastActionTime() {
        return lastActionTime;
    }

    @Override
    public void updateLastActionTime() {
        this.lastActionTime = System.currentTimeMillis();
    }

    @Override
    public boolean containsPlayer(UUID uuid) {
        return player1.equals(uuid) || player2.equals(uuid);
    }

    @Override
    public UUID getOpponent(UUID uuid) {
        return player1.equals(uuid) ? player2 : player1;
    }
}
