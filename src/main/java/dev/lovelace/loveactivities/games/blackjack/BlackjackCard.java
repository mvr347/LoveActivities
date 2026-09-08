package dev.lovelace.loveactivities.games.blackjack;

public class BlackjackCard {

    public enum Suit {
        HEARTS("Черви", "♥", "<red>", "</red>"),
        DIAMONDS("Бубны", "♦", "<red>", "</red>"),
        CLUBS("Трефы", "♣", "<gray>", "</gray>"),
        SPADES("Пики", "♠", "<gray>", "</gray>");

        private final String nameRu;
        private final String symbol;
        private final String colorTag;
        private final String closeColorTag;

        Suit(String nameRu, String symbol, String colorTag, String closeColorTag) {
            this.nameRu = nameRu;
            this.symbol = symbol;
            this.colorTag = colorTag;
            this.closeColorTag = closeColorTag;
        }

        public String getNameRu() {
            return nameRu;
        }

        public String getSymbol() {
            return symbol;
        }

        public String getColorTag() {
            return colorTag;
        }

        public String getCloseColorTag() {
            return closeColorTag;
        }
    }

    public enum Rank {
        TWO("2", 2, "rank_2"),
        THREE("3", 3, "rank_3"),
        FOUR("4", 4, "rank_4"),
        FIVE("5", 5, "rank_5"),
        SIX("6", 6, "rank_6"),
        SEVEN("7", 7, "rank_7"),
        EIGHT("8", 8, "rank_8"),
        NINE("9", 9, "rank_9"),
        TEN("10", 10, "rank_10"),
        JACK("Валет", 10, "jack"),
        QUEEN("Дама", 10, "queen"),
        KING("Король", 10, "king"),
        ACE("Туз", 11, "ace");

        private final String nameRu;
        private final int value;
        private final String headKey;

        Rank(String nameRu, int value, String headKey) {
            this.nameRu = nameRu;
            this.value = value;
            this.headKey = headKey;
        }

        public String getNameRu() {
            return nameRu;
        }

        public int getValue() {
            return value;
        }

        public String getHeadKey() {
            return headKey;
        }
    }

    private final Suit suit;
    private final Rank rank;

    public BlackjackCard(Suit suit, Rank rank) {
        this.suit = suit;
        this.rank = rank;
    }

    public Suit getSuit() {
        return suit;
    }

    public Rank getRank() {
        return rank;
    }

    public int getValue() {
        return rank.getValue();
    }

    public String getFormattedName() {
        return suit.getColorTag() + rank.getNameRu() + " " + suit.getSymbol() + suit.getCloseColorTag();
    }
}
