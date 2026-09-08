package dev.lovelace.loveactivities.games.dice;

public enum DiceMode {
    CLASSIC("Классика (Сумма очков)", "Classic (Sum)", 2),
    POKER("Покер на костях (5 костей)", "Dice Poker (5 dice)", 5),
    CRAPS("Крэпс (7-11)", "Craps (7-11)", 2);

    private final String nameRu;
    private final String nameEn;
    private final int diceCount;

    DiceMode(String nameRu, String nameEn, int diceCount) {
        this.nameRu = nameRu;
        this.nameEn = nameEn;
        this.diceCount = diceCount;
    }

    public String getNameRu() {
        return nameRu;
    }

    public String getNameEn() {
        return nameEn;
    }

    public int getDiceCount() {
        return diceCount;
    }
}
