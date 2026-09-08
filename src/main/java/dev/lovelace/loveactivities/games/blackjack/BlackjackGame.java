package dev.lovelace.loveactivities.games.blackjack;

import dev.lovelace.loveactivities.LoveActivities;
import dev.lovelace.loveactivities.api.GameSession;
import dev.lovelace.loveactivities.api.GameState;
import dev.lovelace.loveactivities.api.GameType;
import dev.lovelace.loveactivities.gui.AbstractGUI;
import dev.lovelace.loveactivities.gui.TutorialGUI;
import dev.lovelace.loveactivities.manager.SessionManager;
import dev.lovelace.loveactivities.util.SoundUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;

import java.util.*;

public class BlackjackGame implements GameSession {

    private final LoveActivities plugin = LoveActivities.getInstance();
    private final UUID sessionId = UUID.randomUUID();
    private final UUID player1;
    private final UUID player2;
    private long bet;
    private GameState state = GameState.PLAYING;
    private long lastActionTime = System.currentTimeMillis();

    private final BlackjackDeck deck = new BlackjackDeck();
    private final List<BlackjackCard> handP1 = new ArrayList<>();
    private final List<BlackjackCard> handP2 = new ArrayList<>();

    private boolean isPlayer1Turn = true;
    private boolean p1Finished = false;
    private boolean p2Finished = false;
    private final Set<UUID> viewingTutorial = new HashSet<>();

    private BlackjackGUI guiP1;
    private BlackjackGUI guiP2;

    public BlackjackGame(Player p1, Player p2, long bet) {
        this(p1.getUniqueId(), p2.getUniqueId(), bet);
    }

    public BlackjackGame(Player p1, UUID player2Uuid, long bet) {
        this(p1.getUniqueId(), player2Uuid, bet);
    }

    public BlackjackGame(UUID p1, UUID p2, long bet) {
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
        return GameType.BLACKJACK;
    }

    @Override
    public UUID getPlayer1() {
        return player1;
    }

    @Override
    public UUID getPlayer2() {
        return player2;
    }

    public boolean isNpcMatch() {
        return SessionManager.isNpc(player2);
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
        Player p2 = isNpcMatch() ? null : Bukkit.getPlayer(player2);

        handP1.add(deck.drawCard());
        handP1.add(deck.drawCard());
        handP2.add(deck.drawCard());
        handP2.add(deck.drawCard());

        if (p1 != null && p1.isOnline()) {
            this.guiP1 = new BlackjackGUI(p1, this);
            this.guiP1.open();
        }
        if (p2 != null && p2.isOnline()) {
            this.guiP2 = new BlackjackGUI(p2, this);
            this.guiP2.open();
        }

        if (BlackjackDeck.isBlackjack(handP1) && !BlackjackDeck.isBlackjack(handP2)) {
            endWithWinner(player1);
        } else if (BlackjackDeck.isBlackjack(handP2) && !BlackjackDeck.isBlackjack(handP1)) {
            endWithWinner(player2);
        } else if (BlackjackDeck.isBlackjack(handP1) && BlackjackDeck.isBlackjack(handP2)) {
            endWithDraw();
        }
    }

    public List<BlackjackCard> getHandP1() {
        return handP1;
    }

    public List<BlackjackCard> getHandP2() {
        return handP2;
    }

    public boolean isPlayer1Turn() {
        return isPlayer1Turn;
    }

    public boolean isP1Finished() {
        return p1Finished;
    }

    public boolean isP2Finished() {
        return p2Finished;
    }

    public boolean isPlayerTurn(Player player) {
        if (state != GameState.PLAYING) return false;
        if (isPlayer1Turn) {
            return player.getUniqueId().equals(player1) && !p1Finished;
        } else {
            return player.getUniqueId().equals(player2) && !p2Finished;
        }
    }

    public synchronized void actionHit(Player player) {
        if (!isPlayerTurn(player)) return;
        updateLastActionTime();

        boolean isP1 = player.getUniqueId().equals(player1);
        List<BlackjackCard> hand = isP1 ? handP1 : handP2;

        hand.add(deck.drawCard());
        SoundUtil.playCardDraw(player);

        int score = BlackjackDeck.calculateScore(hand);
        if (score > 21) {
            if (isP1) {
                p1Finished = true;
                syncViews();
                endWithWinner(player2);
            } else {
                p2Finished = true;
                syncViews();
                endWithWinner(player1);
            }
            return;
        } else if (score == 21 || hand.size() >= 5) {
            actionStand(player);
            return;
        }

        syncViews();
    }

    public synchronized void actionStand(Player player) {
        if (!isPlayerTurn(player)) return;
        updateLastActionTime();

        boolean isP1 = player.getUniqueId().equals(player1);
        if (isP1) {
            p1Finished = true;
            isPlayer1Turn = false;
            SoundUtil.playClick(player);
            syncViews();

            if (isNpcMatch()) {
                startDealerAi();
            } else {
                Player p2 = Bukkit.getPlayer(player2);
                if (p2 != null && p2.isOnline()) {
                    SoundUtil.playChallenge(p2);
                }
            }
        } else {
            p2Finished = true;
            SoundUtil.playClick(player);
            syncViews();
            evaluateShowdown();
        }
    }

    private org.bukkit.scheduler.BukkitTask dealerAiTask = null;

    private void startDealerAi() {
        if (dealerAiTask != null) dealerAiTask.cancel();
        dealerAiTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (state != GameState.PLAYING) {
                if (dealerAiTask != null) dealerAiTask.cancel();
                dealerAiTask = null;
                return;
            }

            int dealerScore = BlackjackDeck.calculateScore(handP2);
            if (dealerScore < 17 && handP2.size() < 5) {
                handP2.add(deck.drawCard());
                Player p1 = Bukkit.getPlayer(player1);
                if (p1 != null) SoundUtil.playCardDraw(p1);
                syncViews();
            } else {
                if (dealerAiTask != null) dealerAiTask.cancel();
                dealerAiTask = null;
                p2Finished = true;
                syncViews();
                evaluateShowdown();
            }
        }, 15L, 20L);
    }

    public synchronized void actionDoubleDown(Player player) {
        if (!isPlayerTurn(player)) return;
        boolean isP1 = player.getUniqueId().equals(player1);
        List<BlackjackCard> hand = isP1 ? handP1 : handP2;

        if (hand.size() != 2) {
            SoundUtil.playError(player);
            return;
        }

        if (!plugin.getLoveCoreBridge().hasBalance(player, bet)) {
            plugin.getLocaleManager().send(player, "bet_not_enough_money", Map.of("balance", String.valueOf(plugin.getLoveCoreBridge().getBalance(player))));
            SoundUtil.playError(player);
            return;
        }

        if (plugin.getLoveCoreBridge().charge(player, bet)) {
            plugin.getSessionManager().addEscrow(sessionId, player.getUniqueId(), bet);
            this.bet += bet;
            SoundUtil.playSuccess(player);
            hand.add(deck.drawCard());
            SoundUtil.playCardDraw(player);

            int score = BlackjackDeck.calculateScore(hand);
            if (score > 21) {
                if (isP1) {
                    p1Finished = true;
                    syncViews();
                    endWithWinner(player2);
                } else {
                    p2Finished = true;
                    syncViews();
                    endWithWinner(player1);
                }
                return;
            }

            if (isP1) {
                p1Finished = true;
                isPlayer1Turn = false;
                syncViews();
                if (isNpcMatch()) {
                    startDealerAi();
                }
            } else {
                p2Finished = true;
                syncViews();
                evaluateShowdown();
            }
        }
    }

    private void evaluateShowdown() {
        int scoreP1 = BlackjackDeck.calculateScore(handP1);
        int scoreP2 = BlackjackDeck.calculateScore(handP2);

        if (scoreP1 > 21 && scoreP2 > 21) {
            endWithDraw();
        } else if (scoreP1 > 21) {
            endWithWinner(player2);
        } else if (scoreP2 > 21) {
            endWithWinner(player1);
        } else if (scoreP1 > scoreP2) {
            endWithWinner(player1);
        } else if (scoreP2 > scoreP1) {
            endWithWinner(player2);
        } else {
            endWithDraw();
        }
    }

    public void resign(Player player) {
        if (state != GameState.PLAYING) return;
        UUID loser = player.getUniqueId();
        UUID winner = getOpponent(loser);

        Player p1 = Bukkit.getPlayer(player1);
        Player p2 = Bukkit.getPlayer(player2);
        if (p1 != null) plugin.getLocaleManager().send(p1, "autolose_surrender", Map.of("player", player.getName(), "game", "Блэкджек"));
        if (p2 != null) plugin.getLocaleManager().send(p2, "autolose_surrender", Map.of("player", player.getName(), "game", "Блэкджек"));

        endWithWinner(winner);
    }

    public void openTutorial(Player player) {
        if (state != GameState.PLAYING) return;
        viewingTutorial.add(player.getUniqueId());

        boolean isP1 = player.getUniqueId().equals(player1);
        if (isP1 && guiP1 != null) guiP1.setSwitchingInventory(true);
        if (!isP1 && guiP2 != null) guiP2.setSwitchingInventory(true);

        List<List<String>> pages = List.of(
                List.of(
                        "<yellow><bold>Основы Блэкджека (21)</bold></yellow>",
                        "<gray>Цель игры — набрать сумму очков как можно ближе к 21,</gray>",
                        "<gray>но не превысить это число (иначе сразу перебор и проигрыш).</gray>",
                        "",
                        "<white>Номиналы карт:</white>",
                        "<gray>• Карты 2–10 дают соответствующее число очков (2–10).</gray>",
                        "<gray>• Валет, Дама, Король дают по 10 очков.</gray>",
                        "<gray>• Туз даёт 11 очков (или 1 очко, если с 11 получается перебор).</gray>"
                ),
                List.of(
                        "<yellow><bold>Действия игрока</bold></yellow>",
                        "<green>▶ Взять карту (+1)</green> <gray>— добрать ещё одну карту из колоды.</gray>",
                        "<yellow>▶ Хватит</yellow> <gray>— зафиксировать свои очки и передать ход сопернику.</gray>",
                        "<gold>▶ Удвоить ставку (x2)</gold> <gray>— удвоить ставку, взять ровно 1 карту</gray>",
                        "<gray>и автоматически завершить свой ход.</gray>",
                        "",
                        "<yellow>Побеждает игрок с наибольшим числом очков ≤ 21!</yellow>"
                )
        );

        new TutorialGUI(player, GameType.BLACKJACK, pages, () -> {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                viewingTutorial.remove(player.getUniqueId());
                if (state == GameState.PLAYING) {
                    if (isP1 && guiP1 != null) {
                        guiP1.setSwitchingInventory(true);
                        guiP1.open();
                    } else if (!isP1 && guiP2 != null) {
                        guiP2.setSwitchingInventory(true);
                        guiP2.open();
                    }
                }
            }, 1L);
        }).open();
    }

    private void syncViews() {
        if (guiP1 != null && !viewingTutorial.contains(player1)) guiP1.initializeItems();
        if (guiP2 != null && !viewingTutorial.contains(player2)) guiP2.initializeItems();
    }

    @Override
    public void onPlayerClick(Player player, int slot, ClickType clickType) {}

    @Override
    public void onPlayerClose(Player player) {
        if (state != GameState.PLAYING || (viewingTutorial != null && viewingTutorial.contains(player.getUniqueId()))) {
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (state != GameState.PLAYING) return;
            if (viewingTutorial != null && viewingTutorial.contains(player.getUniqueId())) return;
            if (!player.isOnline()) {
                autoLose(player.getUniqueId(), "autolose_disconnect");
                return;
            }
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof AbstractGUI) {
                return;
            }
            this.state = GameState.FINISHED;
            autoLose(player.getUniqueId(), "autolose_gui_close");
        }, 3L);
    }

    private void closeAllGuis() {
        if (dealerAiTask != null) { dealerAiTask.cancel(); dealerAiTask = null; }
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
        if (state != GameState.PLAYING) return;
        long elapsed = (System.currentTimeMillis() - lastActionTime) / 1000L;
        if (elapsed > plugin.getConfigManager().getAfkTurnTimeoutSeconds()) {
            UUID afkPlayer = isPlayer1Turn ? player1 : player2;
            autoLose(afkPlayer, "autolose_afk");
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
