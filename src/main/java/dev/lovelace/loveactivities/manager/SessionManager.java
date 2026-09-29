package dev.lovelace.loveactivities.manager;

import dev.lovelace.loveactivities.LoveActivities;
import dev.lovelace.loveactivities.api.GameSession;
import dev.lovelace.loveactivities.api.GameState;
import dev.lovelace.loveactivities.api.GameType;
import dev.lovelace.loveactivities.api.events.LoveActivityGameEndEvent;
import dev.lovelace.loveactivities.api.events.LoveActivityGameStartEvent;
import dev.lovelace.loveactivities.api.events.LoveActivityWinEvent;
import dev.lovelace.loveactivities.games.blackjack.BlackjackGame;
import dev.lovelace.loveactivities.games.cards.CardsGame;
import dev.lovelace.loveactivities.games.cards.CardsMode;
import dev.lovelace.loveactivities.games.dice.DiceGame;
import dev.lovelace.loveactivities.games.dice.DiceMode;
import dev.lovelace.loveactivities.games.gwent.GwentGame;
import dev.lovelace.loveactivities.games.rps.RPSGame;
import dev.lovelace.loveactivities.util.CurrencyUtil;
import dev.lovelace.loveactivities.util.ParticleUtil;
import dev.lovelace.loveactivities.util.SoundUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import dev.lovelace.loveactivities.gui.PreGameTutorialGUI;
import dev.lovelace.loveactivities.gui.TutorialGUI;
import dev.lovelace.loveactivities.gui.TutorialRegistry;
import dev.lovelace.loveactivities.gui.WaitingTutorialGUI;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {

    public static final UUID NPC_UUID = UUID.fromString("00000000-0000-0000-0000-000000001337");
    public static final String NPC_NAME = "Казино Дилер";

    public static boolean isNpc(UUID uuid) {
        return NPC_UUID.equals(uuid);
    }

    private final LoveActivities plugin;
    private final Map<UUID, GameSession> sessionsByPlayer = new ConcurrentHashMap<>();
    private final Map<UUID, GameSession> sessionsById = new ConcurrentHashMap<>();
    private final Map<UUID, Long> escrowP1 = new ConcurrentHashMap<>();
    private final Map<UUID, Long> escrowP2 = new ConcurrentHashMap<>();
    private final Map<UUID, NpcActivityConfig> npcConfigsBySession = new ConcurrentHashMap<>();
    private final Map<UUID, String> npcNamesBySession = new ConcurrentHashMap<>();

    private org.bukkit.scheduler.BukkitTask tickerTask;

    public SessionManager(LoveActivities plugin) {
        this.plugin = plugin;
    }

    public void startTicker() {
        tickerTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickSessions, 10L, 10L);
    }

    public void createAndStartSession(Player p1, Player p2, GameType gameType, long agreedBet) {
        createAndStartSession(p1, p2, gameType, null, agreedBet, agreedBet, agreedBet);
    }

    public void createAndStartSession(Player p1, Player p2, GameType gameType, String subMode, long agreedBet) {
        createAndStartSession(p1, p2, gameType, subMode, agreedBet, agreedBet, agreedBet);
    }

    public void createAndStartSession(Player p1, Player p2, GameType gameType, long agreedBet, long potP1, long potP2) {
        createAndStartSession(p1, p2, gameType, null, agreedBet, potP1, potP2);
    }

    public void createAndStartSession(Player p1, Player p2, GameType gameType, String subMode, long agreedBet, long potP1, long potP2) {
        boolean tutorialEnabled = plugin.getConfigManager().isPreGameTutorial();
        if (tutorialEnabled) {
            boolean p1New = plugin.getStatsManager().getGameStats(p1.getUniqueId(), gameType).getTotalGames() == 0;
            boolean p2New = (p2 != null && !isNpc(p2.getUniqueId())) && plugin.getStatsManager().getGameStats(p2.getUniqueId(), gameType).getTotalGames() == 0;
            if (p1New && p1.isOnline()) {
                promptTutorialFlow(p1, p2, gameType, agreedBet, potP1, potP2, () -> {
                    if (p2New && p2 != null && p2.isOnline()) {
                        promptTutorialFlow(p2, p1, gameType, agreedBet, potP2, potP1, () -> executeDirectStart(p1, p2, gameType, subMode, agreedBet, potP1, potP2));
                    } else {
                        executeDirectStart(p1, p2, gameType, subMode, agreedBet, potP1, potP2);
                    }
                });
                return;
            }
            if (p2New && p2 != null && p2.isOnline()) {
                promptTutorialFlow(p2, p1, gameType, agreedBet, potP2, potP1, () -> executeDirectStart(p1, p2, gameType, subMode, agreedBet, potP1, potP2));
                return;
            }
        }
        executeDirectStart(p1, p2, gameType, subMode, agreedBet, potP1, potP2);
    }

    private void promptTutorialFlow(Player student, Player otherPlayer, GameType gameType, long agreedBet, long potStudent, long potOther, Runnable onStartGame) {
        java.util.concurrent.atomic.AtomicBoolean handled = new java.util.concurrent.atomic.AtomicBoolean(false);
        Runnable cancelAction = () -> {
            if (!handled.compareAndSet(false, true)) return;
            // give(UUID, ...) queues the payout for an offline player, so the refund must not be gated on
            // isOnline(): the stake is already charged, and the one who left is exactly who needs it back.
            if (potStudent > 0) {
                plugin.getLoveCoreBridge().give(student.getUniqueId(), potStudent);
                if (student.isOnline()) {
                    plugin.getLocaleManager().send(student, "bet_refunded", Map.of("amount", String.valueOf(potStudent), "currency", plugin.getLoveCoreBridge().currencyName()));
                }
            }
            if (otherPlayer != null && !isNpc(otherPlayer.getUniqueId()) && potOther > 0) {
                plugin.getLoveCoreBridge().give(otherPlayer.getUniqueId(), potOther);
                if (otherPlayer.isOnline()) {
                    plugin.getLocaleManager().send(otherPlayer, "bet_refunded", Map.of("amount", String.valueOf(potOther), "currency", plugin.getLoveCoreBridge().currencyName()));
                }
            }
            if (student.isOnline()) student.closeInventory();
            if (otherPlayer != null && otherPlayer.isOnline()) otherPlayer.closeInventory();
        };
        Runnable safeStart = () -> { handled.set(true); onStartGame.run(); };
        new PreGameTutorialGUI(student, otherPlayer, gameType, safeStart, () -> {
            handled.set(true);
            List<List<String>> pages = TutorialRegistry.getTutorialPages(gameType);
            if (otherPlayer != null && otherPlayer.isOnline() && !isNpc(otherPlayer.getUniqueId())) {
                new WaitingTutorialGUI(otherPlayer, student, gameType, () -> {
                    new TutorialGUI(otherPlayer, gameType, pages, () -> {}).open();
                }, cancelAction).open();
            }
            new TutorialGUI(student, gameType, pages, () -> {
                if (otherPlayer != null && otherPlayer.isOnline() && !isNpc(otherPlayer.getUniqueId())) otherPlayer.closeInventory();
                safeStart.run();
            }).open();
        }, cancelAction).open();
    }

    private void executeDirectStart(Player p1, Player p2, GameType gameType, String subMode, long agreedBet, long potP1, long potP2) {
        Runnable startLogic = () -> {
            if (!p1.isOnline() || (p2 != null && !isNpc(p2.getUniqueId()) && !p2.isOnline())) {
                // Not gated on isOnline(): give(UUID, ...) queues for an offline player, and the one who
                // disconnected is exactly the one whose already-charged stake would otherwise vanish.
                if (potP1 > 0) plugin.getLoveCoreBridge().give(p1.getUniqueId(), potP1);
                if (potP2 > 0 && p2 != null && !isNpc(p2.getUniqueId())) plugin.getLoveCoreBridge().give(p2.getUniqueId(), potP2);
                return;
            }
            GameSession session = switch (gameType) {
                case BLACKJACK -> new BlackjackGame(p1, p2, agreedBet);
                case DICE -> {
                    DiceMode dMode = "dice_classic".equalsIgnoreCase(subMode) || "classic".equalsIgnoreCase(subMode) ? DiceMode.CLASSIC : DiceMode.POKER;
                    yield new DiceGame(p1, p2, agreedBet, dMode);
                }
                case RPS -> new RPSGame(p1, p2, agreedBet);
                case GWENT -> new GwentGame(p1, p2, agreedBet);
                case CARDS -> {
                    CardsMode cMode = CardsMode.DURAK;
                    if ("poker".equalsIgnoreCase(subMode) || "texasholdem".equalsIgnoreCase(subMode)) cMode = CardsMode.POKER;
                    else if ("war".equalsIgnoreCase(subMode)) cMode = CardsMode.WAR;
                    yield new CardsGame(p1.getUniqueId(), p2 != null ? p2.getUniqueId() : NPC_UUID, agreedBet, cMode);
                }
                case CHESS -> new dev.lovelace.loveactivities.games.chess.MiniChessGame(plugin, UUID.randomUUID(), p1.getUniqueId(), p2 != null ? p2.getUniqueId() : NPC_UUID, agreedBet);
            };
            LoveActivityGameStartEvent startEvent = new LoveActivityGameStartEvent(session, gameType, p1.getUniqueId(), p2.getUniqueId(), agreedBet, false, null);
            Bukkit.getPluginManager().callEvent(startEvent);
            if (startEvent.isCancelled()) {
                if (potP1 > 0) plugin.getLoveCoreBridge().give(p1.getUniqueId(), potP1);
                if (potP2 > 0 && p2 != null && !isNpc(p2.getUniqueId())) plugin.getLoveCoreBridge().give(p2.getUniqueId(), potP2);
                p1.closeInventory();
                if (p2 != null) p2.closeInventory();
                return;
            }
            sessionsById.put(session.getSessionId(), session);
            sessionsByPlayer.put(p1.getUniqueId(), session);
            if (p2 != null && !isNpc(p2.getUniqueId())) sessionsByPlayer.put(p2.getUniqueId(), session);
            escrowP1.put(session.getSessionId(), potP1);
            escrowP2.put(session.getSessionId(), potP2);
            String lang = plugin.getConfigManager().getLanguage();
            String gName = gameType.getDisplayName(lang);
            plugin.getLocaleManager().send(p1, "game_started", Map.of("game", gName));
            if (p2 != null && p2.isOnline() && !isNpc(p2.getUniqueId())) plugin.getLocaleManager().send(p2, "game_started", Map.of("game", gName));
            session.startGame();
        };
        if (Bukkit.isPrimaryThread()) startLogic.run();
        else Bukkit.getScheduler().runTask(plugin, startLogic);
    }

    public void startNpcSession(Player player, GameType gameType, long bet) {
        startNpcSession(player, gameType, null, bet, NPC_NAME, null);
    }

    public void startNpcSession(Player player, GameType gameType, String subMode, long bet) {
        startNpcSession(player, gameType, subMode, bet, NPC_NAME, null);
    }

    public void startNpcSession(Player player, GameType gameType, long bet, String customNpcName, NpcActivityConfig npcConfig) {
        startNpcSession(player, gameType, null, bet, customNpcName, npcConfig);
    }

    public void startNpcSession(Player player, GameType gameType, String subMode, long bet, String customNpcName, NpcActivityConfig npcConfig) {
        String npcName = (customNpcName != null && !customNpcName.isBlank()) ? customNpcName : NPC_NAME;

        // Плохая / конфликтная репутация — NPC не играют
        if (plugin.getLoveCoreBridge().isBadOrConflictReputation(player.getUniqueId())) {
            player.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(
                "<red>[Активности]</red> <gray>С тобой не хотят иметь дел. Улучши репутацию.</gray>"
            ));
            player.closeInventory();
            return;
        }

        if (bet > 0) {
            if (!plugin.getLoveCoreBridge().hasBalance(player, bet) || !plugin.getLoveCoreBridge().charge(player, bet)) {
                plugin.getLocaleManager().send(player, "bet_not_enough_money", Map.of("balance", String.valueOf(plugin.getLoveCoreBridge().getBalance(player))));
                SoundUtil.playError(player);
                return;
            }
        }

        GameSession session = switch (gameType) {
            case BLACKJACK -> new BlackjackGame(player.getUniqueId(), NPC_UUID, bet);
            case CARDS -> {
                CardsMode cMode = CardsMode.DURAK;
                if ("poker".equalsIgnoreCase(subMode) || "texasholdem".equalsIgnoreCase(subMode)) cMode = CardsMode.POKER;
                else if ("war".equalsIgnoreCase(subMode)) cMode = CardsMode.WAR;
                yield new CardsGame(player.getUniqueId(), NPC_UUID, bet, cMode);
            }
            case DICE -> {
                DiceMode dMode = "dice_classic".equalsIgnoreCase(subMode) || "classic".equalsIgnoreCase(subMode) ? DiceMode.CLASSIC : DiceMode.POKER;
                yield new DiceGame(player.getUniqueId(), NPC_UUID, bet, dMode);
            }
            case RPS -> new RPSGame(player.getUniqueId(), NPC_UUID, bet);
            case GWENT -> new GwentGame(player, null, bet);
            case CHESS -> new dev.lovelace.loveactivities.games.chess.MiniChessGame(plugin, UUID.randomUUID(), player.getUniqueId(), NPC_UUID, bet);
        };

        LoveActivityGameStartEvent startEvent = new LoveActivityGameStartEvent(session, gameType, player.getUniqueId(), NPC_UUID, bet, true, npcName);
        Bukkit.getPluginManager().callEvent(startEvent);
        if (startEvent.isCancelled()) {
            if (bet > 0) plugin.getLoveCoreBridge().give(player.getUniqueId(), bet);
            player.closeInventory();
            return;
        }

        sessionsById.put(session.getSessionId(), session);
        sessionsByPlayer.put(player.getUniqueId(), session);
        escrowP1.put(session.getSessionId(), bet);
        escrowP2.put(session.getSessionId(), bet);
        if (npcConfig != null) npcConfigsBySession.put(session.getSessionId(), npcConfig);
        npcNamesBySession.put(session.getSessionId(), npcName);

        String lang = plugin.getConfigManager().getLanguage();
        String gName = gameType.getDisplayName(lang);
        plugin.getLocaleManager().send(player, "game_started", Map.of("game", gName + " (против " + npcName + ")"));
        session.startGame();
    }

    public boolean isInGame(UUID uuid) { return sessionsByPlayer.containsKey(uuid); }
    public GameSession getSession(UUID uuid) { return sessionsByPlayer.get(uuid); }
    public GameSession getSessionById(UUID sessionId) { return sessionsById.get(sessionId); }
    public NpcActivityConfig getNpcConfig(UUID sessionId) { return npcConfigsBySession.get(sessionId); }
    public String getNpcName(UUID sessionId) { return npcNamesBySession.getOrDefault(sessionId, NPC_NAME); }

    public void addEscrow(UUID sessionId, UUID player, long amount) {
        GameSession session = sessionsById.get(sessionId);
        if (session == null) return;
        boolean isP1 = player.equals(session.getPlayer1());
        if (isP1) {
            escrowP1.compute(sessionId, (k, v) -> (v == null ? 0L : v) + amount);
            if (isNpc(session.getPlayer2())) escrowP2.compute(sessionId, (k, v) -> (v == null ? 0L : v) + amount);
        } else {
            escrowP2.compute(sessionId, (k, v) -> (v == null ? 0L : v) + amount);
        }
    }

    private void tickSessions() {
        double maxDist = plugin.getConfigManager().getMaxDistance();
        for (GameSession session : sessionsById.values()) {
            if (session.getState() != GameState.PLAYING) continue;
            Player p1 = Bukkit.getPlayer(session.getPlayer1());
            if (p1 == null || !p1.isOnline()) { session.autoLose(session.getPlayer1(), "autolose_disconnect"); continue; }
            if (!isNpc(session.getPlayer2())) {
                Player p2 = Bukkit.getPlayer(session.getPlayer2());
                if (p2 == null || !p2.isOnline()) { session.autoLose(session.getPlayer2(), "autolose_disconnect"); continue; }
                if (p1.getWorld() != p2.getWorld() || p1.getLocation().distanceSquared(p2.getLocation()) > (maxDist * maxDist)) {
                    session.autoLose(session.getPlayer1(), "autolose_distance");
                    continue;
                }
            }
            int timeout = plugin.getConfigManager().getAfkTurnTimeoutSeconds();
            long elapsed = (System.currentTimeMillis() - session.getLastActionTime()) / 1000L;
            long remaining = timeout - elapsed;
            if (remaining == 15 || remaining == 10 || (remaining >= 1 && remaining <= 5)) {
                SoundUtil.playTimerAlert(p1, (int) remaining);
                if (!isNpc(session.getPlayer2())) {
                    Player p2 = Bukkit.getPlayer(session.getPlayer2());
                    if (p2 != null && p2.isOnline()) SoundUtil.playTimerAlert(p2, (int) remaining);
                }
            }
            session.tickTurnTimer();
        }
    }

    private final Set<UUID> endedSessions = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public void handleAutoLose(GameSession session, UUID loserUuid, String reasonKey) {
        if (endedSessions.contains(session.getSessionId()) || session.getState() == GameState.CANCELLED) return;
        UUID winnerUuid = session.getOpponent(loserUuid);
        Player loser = Bukkit.getPlayer(loserUuid);
        Player winner = Bukkit.getPlayer(winnerUuid);
        if (loser != null && loser.isOnline()) plugin.getLocaleManager().send(loser, reasonKey, Map.of("player", loser.getName()));
        if (winner != null && winner.isOnline() && !isNpc(winnerUuid)) plugin.getLocaleManager().send(winner, reasonKey, Map.of("player", loser != null ? loser.getName() : "Opponent"));
        handleWinner(session, winnerUuid);
    }

    public void handleWinner(GameSession session, UUID winnerUuid) {
        if (!endedSessions.add(session.getSessionId())) return;
        session.setState(GameState.FINISHED);
        UUID loserUuid = session.getOpponent(winnerUuid);
        boolean isNpc = isNpc(session.getPlayer2());
        String npcName = npcNamesBySession.getOrDefault(session.getSessionId(), NPC_NAME);
        NpcActivityConfig npcConfig = npcConfigsBySession.get(session.getSessionId());
        long p1Deposit = escrowP1.getOrDefault(session.getSessionId(), session.getBet());
        long p2Deposit = escrowP2.getOrDefault(session.getSessionId(), session.getBet());
        long totalPot = p1Deposit + p2Deposit;
        if (totalPot > 0 && !isNpc(winnerUuid)) plugin.getLoveCoreBridge().give(winnerUuid, totalPot);
        if (!isNpc(winnerUuid) && !isNpc(loserUuid)) plugin.getStatsManager().recordWin(winnerUuid, loserUuid, session.getGameType(), session.getBet());
        Player winner = Bukkit.getPlayer(winnerUuid);
        Player loser = Bukkit.getPlayer(loserUuid);
        Player p1 = Bukkit.getPlayer(session.getPlayer1());
        Player p2 = Bukkit.getPlayer(session.getPlayer2());
        if (p1 != null && p1.isOnline()) p1.closeInventory();
        if (p2 != null && p2.isOnline()) p2.closeInventory();
        String gName = session.getGameType().getDisplayName(plugin.getConfigManager().getLanguage());
        String currency = plugin.getLoveCoreBridge().currencyName();
        if (winner != null && winner.isOnline() && !isNpc(winnerUuid)) {
            if (totalPot > 0) plugin.getLocaleManager().send(winner, "game_win", Map.of("player", loser != null ? loser.getName() : npcName, "game", gName, "amount", String.valueOf(totalPot), "currency", currency));
            else plugin.getLocaleManager().send(winner, "game_win_no_bets", Map.of("player", loser != null ? loser.getName() : npcName, "game", gName));
            SoundUtil.playWin(winner);
            ParticleUtil.spawnWin(winner);
            if (isNpc && npcConfig != null) {
                String lossPhrase = npcConfig.getRandomDialogue("loss");
                if (lossPhrase != null) plugin.getNpcManager().speak(winner, npcName, lossPhrase);
            }
        }
        if (loser != null && loser.isOnline() && !isNpc(loserUuid)) {
            plugin.getLocaleManager().send(loser, "game_loss", Map.of("player", winner != null ? winner.getName() : npcName, "game", gName));
            SoundUtil.playLoss(loser);
            if (isNpc && npcConfig != null) {
                String winPhrase = npcConfig.getRandomDialogue("win");
                if (winPhrase != null) plugin.getNpcManager().speak(loser, npcName, winPhrase);
            }
        }
        if (!isNpc(winnerUuid)) Bukkit.getPluginManager().callEvent(new LoveActivityWinEvent(session, session.getGameType(), winnerUuid, loserUuid, totalPot, isNpc, isNpc ? npcName : null));
        Bukkit.getPluginManager().callEvent(new LoveActivityGameEndEvent(session, session.getGameType(), session.getPlayer1(), session.getPlayer2(), winnerUuid, session.getBet(), isNpc, isNpc ? npcName : null));
        plugin.getBetLogger().log("Game ended: pot " + totalPot + " " + currency);
        cleanSession(session);
    }

    public void handleDraw(GameSession session) {
        if (!endedSessions.add(session.getSessionId())) return;
        session.setState(GameState.FINISHED);
        UUID p1Id = session.getPlayer1();
        UUID p2Id = session.getPlayer2();
        boolean isNpc = isNpc(p2Id);
        String npcName = npcNamesBySession.getOrDefault(session.getSessionId(), NPC_NAME);
        NpcActivityConfig npcConfig = npcConfigsBySession.get(session.getSessionId());
        long p1Deposit = escrowP1.getOrDefault(session.getSessionId(), session.getBet());
        long p2Deposit = escrowP2.getOrDefault(session.getSessionId(), session.getBet());
        if (p1Deposit > 0 && !isNpc(p1Id)) plugin.getLoveCoreBridge().give(p1Id, p1Deposit);
        if (p2Deposit > 0 && !isNpc(p2Id)) plugin.getLoveCoreBridge().give(p2Id, p2Deposit);
        Player p1 = Bukkit.getPlayer(p1Id);
        Player p2 = Bukkit.getPlayer(p2Id);
        if (p1 != null && p1.isOnline()) p1.closeInventory();
        if (p2 != null && p2.isOnline()) p2.closeInventory();
        String drawMsgKey = (p1Deposit > 0 || p2Deposit > 0) ? "game_draw" : "game_draw_no_bets";
        if (p1 != null && p1.isOnline() && !isNpc(p1Id)) {
            plugin.getLocaleManager().send(p1, drawMsgKey);
            SoundUtil.playDraw(p1);
            if (isNpc && npcConfig != null) {
                String drawPhrase = npcConfig.getRandomDialogue("draw");
                if (drawPhrase != null) plugin.getNpcManager().speak(p1, npcName, drawPhrase);
            }
        }
        if (p2 != null && p2.isOnline() && !isNpc(p2Id)) {
            plugin.getLocaleManager().send(p2, drawMsgKey);
            SoundUtil.playDraw(p2);
        }
        Bukkit.getPluginManager().callEvent(new LoveActivityGameEndEvent(session, session.getGameType(), p1Id, p2Id, null, session.getBet(), isNpc, isNpc ? npcName : null));
        cleanSession(session);
    }

    public void handleCancelAndRefund(GameSession session, String reasonKey) {
        if (!endedSessions.add(session.getSessionId())) return;
        session.setState(GameState.CANCELLED);
        UUID p1Id = session.getPlayer1();
        UUID p2Id = session.getPlayer2();
        Player p1 = Bukkit.getPlayer(p1Id);
        Player p2 = Bukkit.getPlayer(p2Id);
        if (p1 != null && p1.isOnline()) p1.closeInventory();
        if (p2 != null && p2.isOnline()) p2.closeInventory();
        String gName = session.getGameType().getDisplayName(plugin.getConfigManager().getLanguage());
        String cancelMsgKey = (reasonKey != null && !reasonKey.isEmpty() && plugin.getLocaleManager().hasKey(reasonKey)) ? reasonKey : "game_cancelled";
        if (p1 != null && p1.isOnline() && !isNpc(p1Id)) {
            plugin.getLocaleManager().send(p1, cancelMsgKey, Map.of("game", gName, "player", p2 != null ? p2.getName() : "Opponent"));
            SoundUtil.playError(p1);
        }
        if (p2 != null && p2.isOnline() && !isNpc(p2Id)) {
            plugin.getLocaleManager().send(p2, cancelMsgKey, Map.of("game", gName, "player", p1 != null ? p1.getName() : "Opponent"));
            SoundUtil.playError(p2);
        }
        long p1Deposit = escrowP1.getOrDefault(session.getSessionId(), session.getBet());
        long p2Deposit = escrowP2.getOrDefault(session.getSessionId(), session.getBet());
        if (p1Deposit > 0 && !isNpc(p1Id)) {
            plugin.getLoveCoreBridge().give(p1Id, p1Deposit);
            if (p1 != null && p1.isOnline()) plugin.getLocaleManager().send(p1, "bet_refunded", Map.of("amount", String.valueOf(p1Deposit), "currency", plugin.getLoveCoreBridge().currencyName()));
        }
        if (p2Deposit > 0 && !isNpc(p2Id)) {
            plugin.getLoveCoreBridge().give(p2Id, p2Deposit);
            if (p2 != null && p2.isOnline()) plugin.getLocaleManager().send(p2, "bet_refunded", Map.of("amount", String.valueOf(p2Deposit), "currency", plugin.getLoveCoreBridge().currencyName()));
        }
        cleanSession(session);
    }

    private void cleanSession(GameSession session) {
        sessionsById.remove(session.getSessionId());
        sessionsByPlayer.remove(session.getPlayer1());
        sessionsByPlayer.remove(session.getPlayer2());
        escrowP1.remove(session.getSessionId());
        escrowP2.remove(session.getSessionId());
        npcConfigsBySession.remove(session.getSessionId());
        npcNamesBySession.remove(session.getSessionId());
        if (plugin.isEnabled()) Bukkit.getScheduler().runTaskLater(plugin, () -> endedSessions.remove(session.getSessionId()), 200L);
        else endedSessions.remove(session.getSessionId());
    }

    public void shutdown() {
        if (tickerTask != null) { tickerTask.cancel(); tickerTask = null; }
        List<GameSession> active = new ArrayList<>(sessionsById.values());
        for (GameSession session : active) handleCancelAndRefund(session, "plugin_shutdown");
        endedSessions.clear();
    }
}
