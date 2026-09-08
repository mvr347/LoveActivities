package dev.lovelace.loveactivities.games.cards;

public enum CardsMode {
    DURAK("Дурак подкидной", "Durak"),
    POKER("Техасский Холдем", "Texas Hold'em"),
    WAR("Простая война", "War");

    private final String nameRu;
    private final String nameEn;

    CardsMode(String nameRu, String nameEn) {
        this.nameRu = nameRu;
        this.nameEn = nameEn;
    }

    public String getNameRu() {
        return nameRu;
    }

    public String getNameEn() {
        return nameEn;
    }
}
