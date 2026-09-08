package dev.lovelace.loveactivities.games.chess;

import java.util.ArrayList;
import java.util.List;

public class MiniChessBoard {

    public static final int ROWS = 6;
    public static final int COLS = 7;
    public static final int SIZE = 6;
    private final ChessPiece[][] board = new ChessPiece[ROWS][COLS];

    public MiniChessBoard() {
        reset();
    }

    public void reset() {
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                board[r][c] = null;
            }
        }

        // 6x7 MiniChess Layout:
        // Rank 0 (Black pieces): Rook, Knight, Bishop, Queen, King, Bishop, Rook
        board[0][0] = new ChessPiece(ChessPiece.Color.BLACK, ChessPiece.Type.ROOK);
        board[0][1] = new ChessPiece(ChessPiece.Color.BLACK, ChessPiece.Type.KNIGHT);
        board[0][2] = new ChessPiece(ChessPiece.Color.BLACK, ChessPiece.Type.BISHOP);
        board[0][3] = new ChessPiece(ChessPiece.Color.BLACK, ChessPiece.Type.QUEEN);
        board[0][4] = new ChessPiece(ChessPiece.Color.BLACK, ChessPiece.Type.KING);
        board[0][5] = new ChessPiece(ChessPiece.Color.BLACK, ChessPiece.Type.BISHOP);
        board[0][6] = new ChessPiece(ChessPiece.Color.BLACK, ChessPiece.Type.ROOK);

        // Rank 1 (Black pawns)
        for (int c = 0; c < COLS; c++) {
            board[1][c] = new ChessPiece(ChessPiece.Color.BLACK, ChessPiece.Type.PAWN);
        }

        // Rank 4 (White pawns)
        for (int c = 0; c < COLS; c++) {
            board[4][c] = new ChessPiece(ChessPiece.Color.WHITE, ChessPiece.Type.PAWN);
        }

        // Rank 5 (White pieces): Rook, Knight, Bishop, Queen, King, Bishop, Rook
        board[5][0] = new ChessPiece(ChessPiece.Color.WHITE, ChessPiece.Type.ROOK);
        board[5][1] = new ChessPiece(ChessPiece.Color.WHITE, ChessPiece.Type.KNIGHT);
        board[5][2] = new ChessPiece(ChessPiece.Color.WHITE, ChessPiece.Type.BISHOP);
        board[5][3] = new ChessPiece(ChessPiece.Color.WHITE, ChessPiece.Type.QUEEN);
        board[5][4] = new ChessPiece(ChessPiece.Color.WHITE, ChessPiece.Type.KING);
        board[5][5] = new ChessPiece(ChessPiece.Color.WHITE, ChessPiece.Type.BISHOP);
        board[5][6] = new ChessPiece(ChessPiece.Color.WHITE, ChessPiece.Type.ROOK);
    }

    public ChessPiece getPiece(int row, int col) {
        if (!inBounds(row, col)) return null;
        return board[row][col];
    }

    public void setPiece(int row, int col, ChessPiece piece) {
        if (inBounds(row, col)) {
            board[row][col] = piece;
        }
    }

    public static boolean inBounds(int r, int c) {
        return r >= 0 && r < ROWS && c >= 0 && c < COLS;
    }

    public record Move(int fromR, int fromC, int toR, int toC) {}

    public List<Move> getLegalMoves(int r, int c) {
        List<Move> moves = new ArrayList<>();
        ChessPiece piece = getPiece(r, c);
        if (piece == null) return moves;

        ChessPiece.Color color = piece.getColor();

        switch (piece.getType()) {
            case PAWN -> {
                int forward = (color == ChessPiece.Color.WHITE) ? -1 : 1;
                int nextR = r + forward;
                // Move 1 forward
                if (inBounds(nextR, c) && getPiece(nextR, c) == null) {
                    moves.add(new Move(r, c, nextR, c));
                }
                // Diagonal capture left
                if (inBounds(nextR, c - 1)) {
                    ChessPiece target = getPiece(nextR, c - 1);
                    if (target != null && target.getColor() != color) {
                        moves.add(new Move(r, c, nextR, c - 1));
                    }
                }
                // Diagonal capture right
                if (inBounds(nextR, c + 1)) {
                    ChessPiece target = getPiece(nextR, c + 1);
                    if (target != null && target.getColor() != color) {
                        moves.add(new Move(r, c, nextR, c + 1));
                    }
                }
            }
            case KNIGHT -> {
                int[][] offsets = {{-2, -1}, {-2, 1}, {-1, -2}, {-1, 2}, {1, -2}, {1, 2}, {2, -1}, {2, 1}};
                for (int[] off : offsets) {
                    int nr = r + off[0];
                    int nc = c + off[1];
                    if (inBounds(nr, nc)) {
                        ChessPiece target = getPiece(nr, nc);
                        if (target == null || target.getColor() != color) {
                            moves.add(new Move(r, c, nr, nc));
                        }
                    }
                }
            }
            case BISHOP -> addRangedMoves(moves, r, c, color, new int[][]{{-1, -1}, {-1, 1}, {1, -1}, {1, 1}});
            case ROOK -> addRangedMoves(moves, r, c, color, new int[][]{{-1, 0}, {1, 0}, {0, -1}, {0, 1}});
            case QUEEN -> addRangedMoves(moves, r, c, color, new int[][]{{-1, -1}, {-1, 1}, {1, -1}, {1, 1}, {-1, 0}, {1, 0}, {0, -1}, {0, 1}});
            case KING -> {
                int[][] offsets = {{-1, -1}, {-1, 0}, {-1, 1}, {0, -1}, {0, 1}, {1, -1}, {1, 0}, {1, 1}};
                for (int[] off : offsets) {
                    int nr = r + off[0];
                    int nc = c + off[1];
                    if (inBounds(nr, nc)) {
                        ChessPiece target = getPiece(nr, nc);
                        if (target == null || target.getColor() != color) {
                            moves.add(new Move(r, c, nr, nc));
                        }
                    }
                }
            }
        }

        return moves;
    }

    private void addRangedMoves(List<Move> moves, int r, int c, ChessPiece.Color color, int[][] directions) {
        for (int[] dir : directions) {
            int nr = r + dir[0];
            int nc = c + dir[1];
            while (inBounds(nr, nc)) {
                ChessPiece target = getPiece(nr, nc);
                if (target == null) {
                    moves.add(new Move(r, c, nr, nc));
                } else {
                    if (target.getColor() != color) {
                        moves.add(new Move(r, c, nr, nc));
                    }
                    break;
                }
                nr += dir[0];
                nc += dir[1];
            }
        }
    }

    public List<Move> getAllLegalMoves(ChessPiece.Color color) {
        List<Move> all = new ArrayList<>();
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                ChessPiece p = getPiece(r, c);
                if (p != null && p.getColor() == color) {
                    all.addAll(getLegalMoves(r, c));
                }
            }
        }
        return all;
    }

    public boolean isKingAlive(ChessPiece.Color color) {
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                ChessPiece p = getPiece(r, c);
                if (p != null && p.getColor() == color && p.getType() == ChessPiece.Type.KING) {
                    return true;
                }
            }
        }
        return false;
    }

    public int[] getKingPos(ChessPiece.Color kingColor) {
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                ChessPiece p = getPiece(r, c);
                if (p != null && p.getColor() == kingColor && p.getType() == ChessPiece.Type.KING) {
                    return new int[]{r, c};
                }
            }
        }
        return null;
    }

    public boolean isKingInCheck(ChessPiece.Color kingColor) {
        int[] pos = getKingPos(kingColor);
        if (pos == null) return false;

        ChessPiece.Color oppColor = kingColor.opposite();
        List<Move> oppMoves = getAllLegalMoves(oppColor);
        for (Move m : oppMoves) {
            if (m.toR() == pos[0] && m.toC() == pos[1]) {
                return true;
            }
        }
        return false;
    }

    public boolean makeMove(Move move) {
        ChessPiece p = getPiece(move.fromR(), move.fromC());
        if (p == null) return false;

        board[move.toR()][move.toC()] = p;
        board[move.fromR()][move.fromC()] = null;

        // Pawn promotion to Queen
        if (p.getType() == ChessPiece.Type.PAWN) {
            if ((p.getColor() == ChessPiece.Color.WHITE && move.toR() == 0) ||
                (p.getColor() == ChessPiece.Color.BLACK && move.toR() == ROWS - 1)) {
                p.setType(ChessPiece.Type.QUEEN);
            }
        }

        return true;
    }
}
