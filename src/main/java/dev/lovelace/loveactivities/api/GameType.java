package dev.lovelace.loveactivities.api;

public enum GameType {
    BLACKJACK("blackjack", "Блэкджек", "Blackjack", "blackjack"),
    DICE("dice", "Кости", "Dice", "dice"),
    RPS("rps", "Камень-Ножницы-Бумага", "Rock-Paper-Scissors", "rps"),
    GWENT("gwent", "Гвинт", "Gwent", "gwent"),
    CARDS("cards", "Карты", "Cards", "cards"),
    CHESS("chess", "Мини-шахматы", "Mini Chess", "chess");

    private final String id;
    private final String nameRu;
    private final String nameEn;
    private final String iconKey;

    GameType(String id, String nameRu, String nameEn, String iconKey) {
        this.id = id;
        this.nameRu = nameRu;
        this.nameEn = nameEn;
        this.iconKey = iconKey;
    }

    public String getId() {
        return id;
    }

    public String getNameRu() {
        return nameRu;
    }

    public String getNameEn() {
        return nameEn;
    }

    public String getDisplayName(String lang) {
        return "ru".equalsIgnoreCase(lang) ? nameRu : nameEn;
    }

    public String getIconKey() {
        return iconKey;
    }

    public static GameType fromString(String input) {
        if (input == null) return null;
        String lower = input.trim().toLowerCase();
        for (GameType type : values()) {
            if (type.id.equalsIgnoreCase(lower) || type.nameRu.equalsIgnoreCase(lower) || type.nameEn.equalsIgnoreCase(lower)) {
                return type;
            }
        }
        if (lower.equals("блэкджек") || lower.equals("21")) return BLACKJACK;
        if (lower.equals("кости")) return DICE;
        if (lower.equals("кнб") || lower.equals("rock-paper-scissors")) return RPS;
        if (lower.equals("гвинт")) return GWENT;
        if (lower.equals("карты") || lower.equals("дурак") || lower.equals("война") || lower.equals("покер")) return CARDS;
        if (lower.equals("шахматы") || lower.equals("минишахматы") || lower.equals("minichess") || lower.equals("chess")) return CHESS;
        return null;
    }
}
