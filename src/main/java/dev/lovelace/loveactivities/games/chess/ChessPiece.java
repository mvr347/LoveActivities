package dev.lovelace.loveactivities.games.chess;

public class ChessPiece {

    public enum Color {
        WHITE, BLACK;

        public Color opposite() {
            return this == WHITE ? BLACK : WHITE;
        }
    }

    public enum Type {
        PAWN("Пешка", "pawn"),
        KNIGHT("Конь", "knight"),
        BISHOP("Слон", "bishop"),
        ROOK("Ладья", "rook"),
        QUEEN("Ферзь", "queen"),
        KING("Король", "king");

        private final String nameRu;
        private final String textureKey;

        Type(String nameRu, String textureKey) {
            this.nameRu = nameRu;
            this.textureKey = textureKey;
        }

        public String getNameRu() {
            return nameRu;
        }

        public String getTextureKey() {
            return textureKey;
        }
    }

    private final Color color;
    private Type type;

    public ChessPiece(Color color, Type type) {
        this.color = color;
        this.type = type;
    }

    public Color getColor() {
        return color;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getHeadTextureKey() {
        return "chess." + (color == Color.WHITE ? "white_" : "black_") + type.getTextureKey();
    }

    public String getDisplayName() {
        String colStr = color == Color.WHITE ? "<white>Белый</white>" : "<dark_gray>Чёрный</dark_gray>";
        return colStr + " " + "<yellow>" + type.getNameRu() + "</yellow>";
    }
}
