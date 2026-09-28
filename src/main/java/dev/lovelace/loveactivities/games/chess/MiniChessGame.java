package dev.lovelace.loveactivities.games.chess;

import dev.lovelace.loveactivities.LoveActivities;
import dev.lovelace.loveactivities.api.GameSession;
import dev.lovelace.loveactivities.api.GameState;
import dev.lovelace.loveactivities.api.GameType;
import dev.lovelace.loveactivities.gui.AbstractGUI;
import dev.lovelace.loveactivities.gui.TutorialGUI;
import dev.lovelace.loveactivities.manager.SessionManager;
import dev.lovelace.loveactivities.util.SoundUtil;
import dev.lovelace.loveactivities.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.*;

public class MiniChessGame implements GameSession {

    private final LoveActivities plugin;
    private final UUID sessionId;
    private final UUID player1;
    private final UUID player2;
    private long bet;
    private GameState state = GameState.PLAYING;
    private long lastActionTime;

    private final MiniChessBoard board = new MiniChessBoard();
    private boolean whiteTurn = true; // White moves first (Player 1)
    private UUID drawOfferedBy = null;

    private MiniChessGUI guiP1;
    private MiniChessGUI guiP2;
    private final Set<UUID> viewingTutorial = new HashSet<>();

    public MiniChessGame(LoveActivities plugin, UUID sessionId, UUID player1, UUID player2, long bet) {
        this.plugin = plugin;
        this.sessionId = sessionId;
        this.player1 = player1;
        this.player2 = player2;
        this.bet = bet;
        this.lastActionTime = System.currentTimeMillis();
    }

    public MiniChessBoard getBoard() {
        return board;
    }

    public boolean isWhiteTurn() {
        return whiteTurn;
    }

    public UUID getDrawOfferedBy() {
        return drawOfferedBy;
    }

    public boolean isNpcMatch() {
        return SessionManager.isNpc(player2);
    }

    public String getP2Name() {
        if (isNpcMatch()) {
            return plugin.getSessionManager().getNpcName(sessionId);
        }
        Player p2 = Bukkit.getPlayer(player2);
        return p2 != null ? p2.getName() : "Игрок 2";
    }

    @Override
    public UUID getSessionId() {
        return sessionId;
    }

    @Override
    public GameType getGameType() {
        return GameType.CHESS;
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
        this.state = GameState.PLAYING;
        this.lastActionTime = System.currentTimeMillis();

        Player p1 = Bukkit.getPlayer(player1);
        if (p1 != null) {
            guiP1 = new MiniChessGUI(p1, this, true);
            guiP1.open();
            SoundUtil.playSuccess(p1);
        }

        if (!isNpcMatch()) {
            Player p2 = Bukkit.getPlayer(player2);
            if (p2 != null) {
                guiP2 = new MiniChessGUI(p2, this, false);
                guiP2.open();
                SoundUtil.playSuccess(p2);
            }
        }
    }

    public synchronized void actionMakeMove(Player player, MiniChessBoard.Move move) {
        if (state != GameState.PLAYING) return;
        boolean isP1 = player.getUniqueId().equals(player1);
        if (whiteTurn != isP1) {
            SoundUtil.playError(player);
            return;
        }

        if (move == null) return;
        if (!MiniChessBoard.inBounds(move.fromR(), move.fromC()) || !MiniChessBoard.inBounds(move.toR(), move.toC())) {
            SoundUtil.playError(player);
            return;
        }

        ChessPiece piece = board.getPiece(move.fromR(), move.fromC());
        ChessPiece.Color expectedColor = isP1 ? ChessPiece.Color.WHITE : ChessPiece.Color.BLACK;
        if (piece == null || piece.getColor() != expectedColor) {
            SoundUtil.playError(player);
            return;
        }

        List<MiniChessBoard.Move> legalMoves = board.getLegalMoves(move.fromR(), move.fromC());
        boolean isLegal = false;
        for (MiniChessBoard.Move m : legalMoves) {
            if (m.toR() == move.toR() && m.toC() == move.toC()) {
                isLegal = true;
                break;
            }
        }

        if (!isLegal) {
            SoundUtil.playError(player);
            return;
        }

        updateLastActionTime();
        drawOfferedBy = null;

        boolean success = board.makeMove(move);
        if (!success) {
            SoundUtil.playError(player);
            return;
        }

        drawOfferedBy = null;
        SoundUtil.playMove(player);

        Player opponent = Bukkit.getPlayer(getOpponent(player.getUniqueId()));
        if (opponent != null) SoundUtil.playMove(opponent);

        // Check if King is captured
        if (!board.isKingAlive(ChessPiece.Color.WHITE)) {
            syncViews();
            endWithWinner(player2);
            return;
        } else if (!board.isKingAlive(ChessPiece.Color.BLACK)) {
            syncViews();
            endWithWinner(player1);
            return;
        }

        // Switch Turn
        whiteTurn = !whiteTurn;

        // Check if opponent is under check or checkmate/stalemate
        ChessPiece.Color nextColor = whiteTurn ? ChessPiece.Color.WHITE : ChessPiece.Color.BLACK;
        boolean isCheck = board.isKingInCheck(nextColor);
        List<MiniChessBoard.Move> nextMoves = board.getAllLegalMoves(nextColor);

        if (nextMoves.isEmpty()) {
            syncViews();
            if (isCheck) {
                // Checkmate! Current mover wins
                endWithWinner(player.getUniqueId());
            } else {
                // Stalemate! Draw
                endWithDraw();
            }
            return;
        }

        if (isCheck) {
            Player inCheckPlayer = Bukkit.getPlayer(whiteTurn ? player1 : player2);
            if (inCheckPlayer != null) {
                SoundUtil.playError(inCheckPlayer);
                inCheckPlayer.sendActionBar(TextUtil.parse("<red><bold>⚠ ВАШЕМУ КОРОЛЮ ОБЪЯВЛЕН ШАХ! ⚠</bold></red>"));
            }
            player.sendActionBar(TextUtil.parse("<yellow><bold>⚔ Вы объявили ШАХ королю соперника!</bold></yellow>"));
        }

        syncViews();

        if (!whiteTurn && isNpcMatch()) {
            triggerBotTurn();
        }
    }

    private void triggerBotTurn() {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (state != GameState.PLAYING || whiteTurn) return;

            List<MiniChessBoard.Move> legalMoves = board.getAllLegalMoves(ChessPiece.Color.BLACK);
            if (legalMoves.isEmpty()) {
                if (board.isKingInCheck(ChessPiece.Color.BLACK)) {
                    endWithWinner(player1);
                } else {
                    endWithDraw();
                }
                return;
            }

            // Pick best move (prioritize captures and king safety)
            MiniChessBoard.Move bestMove = legalMoves.get(0);
            int maxScore = -1;

            for (MiniChessBoard.Move m : legalMoves) {
                ChessPiece captured = board.getPiece(m.toR(), m.toC());
                int score = 0;
                if (captured != null) {
                    score = switch (captured.getType()) {
                        case KING -> 1000;
                        case QUEEN -> 90;
                        case ROOK -> 50;
                        case BISHOP -> 30;
                        case KNIGHT -> 30;
                        case PAWN -> 10;
                    };
                }
                if (score > maxScore) {
                    maxScore = score;
                    bestMove = m;
                }
            }

            board.makeMove(bestMove);
            drawOfferedBy = null;

            Player p1 = Bukkit.getPlayer(player1);
            if (p1 != null) SoundUtil.playMove(p1);

            if (!board.isKingAlive(ChessPiece.Color.WHITE)) {
                syncViews();
                endWithWinner(player2);
                return;
            } else if (!board.isKingAlive(ChessPiece.Color.BLACK)) {
                syncViews();
                endWithWinner(player1);
                return;
            }

            whiteTurn = true;

            // Check if player1 is now in check or checkmate
            boolean isCheck = board.isKingInCheck(ChessPiece.Color.WHITE);
            List<MiniChessBoard.Move> p1Moves = board.getAllLegalMoves(ChessPiece.Color.WHITE);
            if (p1Moves.isEmpty()) {
                syncViews();
                if (isCheck) {
                    endWithWinner(player2);
                } else {
                    endWithDraw();
                }
                return;
            }

            if (isCheck && p1 != null) {
                SoundUtil.playError(p1);
                p1.sendActionBar(TextUtil.parse("<red><bold>⚠ БОТ ОБЪЯВИЛ ШАХ ВАШЕМУ КОРОЛЮ! ⚠</bold></red>"));
            }

            syncViews();
        }, 20L);
    }

    public void offerDraw(Player player) {
        if (state != GameState.PLAYING) return;
        UUID sender = player.getUniqueId();

        if (isNpcMatch()) {
            // Evaluate material score
            int whiteScore = 0;
            int blackScore = 0;
            for (int r = 0; r < MiniChessBoard.ROWS; r++) {
                for (int c = 0; c < MiniChessBoard.COLS; c++) {
                    ChessPiece piece = board.getPiece(r, c);
                    if (piece != null) {
                        int val = switch (piece.getType()) {
                            case KING -> 1000;
                            case QUEEN -> 9;
                            case ROOK -> 5;
                            case BISHOP, KNIGHT -> 3;
                            case PAWN -> 1;
                        };
                        if (piece.getColor() == ChessPiece.Color.WHITE) whiteScore += val;
                        else blackScore += val;
                    }
                }
            }

            int diff = blackScore - whiteScore;
            if (diff >= 2) {
                player.sendActionBar(TextUtil.parse("<yellow>Бот отклонил предложение: «У меня позиционное преимущество!»</yellow>"));
                player.sendMessage(TextUtil.parse("<yellow>Бот отклонил предложение ничьей: «У меня позиционное преимущество!»</yellow>"));
                SoundUtil.playError(player);
                return;
            }

            player.sendMessage(TextUtil.parse("<green>Бот согласился на ничью!</green>"));
            player.sendActionBar(TextUtil.parse("<green><bold>🤝 Бот принял предложение ничьей!</bold></green>"));
            SoundUtil.playWin(player);
            Bukkit.getScheduler().runTaskLater(plugin, this::endWithDraw, 20L);
            return;
        }

        if (drawOfferedBy != null && !drawOfferedBy.equals(sender)) {
            Player opp = Bukkit.getPlayer(drawOfferedBy);
            if (opp != null) {
                opp.sendMessage(TextUtil.parse("<green>Соперник согласился на ничью!</green>"));
                SoundUtil.playWin(opp);
            }
            player.sendMessage(TextUtil.parse("<green>Вы согласились на ничью!</green>"));
            SoundUtil.playWin(player);
            endWithDraw();
        } else {
            drawOfferedBy = sender;
            Player opp = Bukkit.getPlayer(getOpponent(sender));
            if (opp != null) {
                opp.sendMessage(TextUtil.parse("<yellow>Соперник предложил ничью! Нажмите кнопку «Принять ничью», чтобы согласиться.</yellow>"));
                opp.sendActionBar(TextUtil.parse("<yellow><bold>🤝 Соперник предложил ничью!</bold></yellow>"));
                SoundUtil.playClick(opp);
            }
            player.sendMessage(TextUtil.parse("<green>Вы предложили ничью сопернику.</green>"));
            player.sendActionBar(TextUtil.parse("<gray>Предложение ничьей отправлено сопернику...</gray>"));
            SoundUtil.playClick(player);
            syncViews();
        }
    }

    public void resign(Player player) {
        if (state != GameState.PLAYING) return;
        UUID loser = player.getUniqueId();
        UUID winner = getOpponent(loser);

        Player p1 = Bukkit.getPlayer(player1);
        Player p2 = Bukkit.getPlayer(player2);
        if (p1 != null) plugin.getLocaleManager().send(p1, "autolose_surrender", Map.of("player", player.getName(), "game", "Мини-шахматы"));
        if (p2 != null) plugin.getLocaleManager().send(p2, "autolose_surrender", Map.of("player", player.getName(), "game", "Мини-шахматы"));

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
                        "<yellow><bold>Мини-шахматы (6x7)</bold></yellow>",
                        "<gray>Динамичная версия шахмат на расширенной доске 6x7 клеток.</gray>",
                        "<gray>У каждого игрока: Король, Ферзь, 2 Слона, Конь, 2 Ладьи и 7 Пешек.</gray>",
                        "",
                        "<white>Цель игры:</white>",
                        "<green>• Захватить Короля противника или поставить ему Мат!</green>",
                        "<yellow>• Пешка, дошедшая до противоположного края, становится Ферзем!</yellow>"
                ),
                List.of(
                        "<yellow><bold>Как ходить фигурами</bold></yellow>",
                        "<gray>1. Нажмите на свою фигуру, чтобы выбрать её.</gray>",
                        "<gray>2. На доске подсветятся доступные ходы:</gray>",
                        "<green>• Зелёные плитки</green> <gray>— свободные клетки для перемещения.</gray>",
                        "<red>• Красные плитки</red> <gray>— вражеские фигуры для взятия.</gray>",
                        "<gray>3. Нажмите на подсвеченную клетку для совершения хода.</gray>"
                )
        );

        new TutorialGUI(player, GameType.CHESS, pages, () -> {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                viewingTutorial.remove(player.getUniqueId());
                lastActionTime = System.currentTimeMillis();
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

    private final long sessionStartTime = System.currentTimeMillis();

    public void reopenGui(Player player) {
        MiniChessGUI gui = player.getUniqueId().equals(player1) ? guiP1 : guiP2;
        if (gui != null) {
            gui.setSwitchingInventory(true);
            gui.open();
        }
    }

    @Override
    public void onPlayerClose(Player player) {
        if (state != GameState.PLAYING || (viewingTutorial != null && viewingTutorial.contains(player.getUniqueId()))) {
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
        }, 15L);
    }

    private void closeAllGuis() {
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
        if (!viewingTutorial.isEmpty()) {
            lastActionTime = System.currentTimeMillis();
            return;
        }
        long elapsed = (System.currentTimeMillis() - lastActionTime) / 1000L;
        if (elapsed > plugin.getConfigManager().getAfkTurnTimeoutSeconds()) {
            UUID afkPlayer = whiteTurn ? player1 : player2;
            autoLose(afkPlayer, "autolose_afk");
            return;
        }
        if (guiP1 != null && !viewingTutorial.contains(player1)) {
            guiP1.updateTimer();
        }
        if (guiP2 != null && !viewingTutorial.contains(player2)) {
            guiP2.updateTimer();
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
