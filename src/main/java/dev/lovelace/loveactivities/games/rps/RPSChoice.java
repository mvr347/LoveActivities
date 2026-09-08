package dev.lovelace.loveactivities.games.rps;

public enum RPSChoice {
    ROCK("Камень", "rps.rock"),
    PAPER("Бумага", "rps.paper"),
    SCISSORS("Ножницы", "rps.scissors");

    private final String nameRu;
    private final String textureKey;

    RPSChoice(String nameRu, String textureKey) {
        this.nameRu = nameRu;
        this.textureKey = textureKey;
    }

    public String getNameRu() {
        return nameRu;
    }

    public String getTextureKey() {
        return textureKey;
    }

    public int compareChoice(RPSChoice other) {
        if (this == other) return 0;
        return switch (this) {
            case ROCK -> (other == SCISSORS) ? 1 : -1;
            case PAPER -> (other == ROCK) ? 1 : -1;
            case SCISSORS -> (other == PAPER) ? 1 : -1;
        };
    }
}
