package dev.lovelace.loveactivities.games.dice;

import java.util.*;

public class DiceCombination implements Comparable<DiceCombination> {

    public enum HandType {
        HIGH_DIE("Старшая кость", 1),
        ONE_PAIR("Пара", 2),
        TWO_PAIR("Две пары", 3),
        THREE_OF_A_KIND("Сет (Тройка)", 4),
        SMALL_STRAIGHT("Малый стрит (1-2-3-4-5)", 5),
        LARGE_STRAIGHT("Большой стрит (2-3-4-5-6)", 6),
        FULL_HOUSE("Фулл-Хаус", 7),
        FOUR_OF_A_KIND("Каре", 8),
        FIVE_OF_A_KIND("Покер (5 одинаковых)", 9);

        private final String nameRu;
        private final int rank;

        HandType(String nameRu, int rank) {
            this.nameRu = nameRu;
            this.rank = rank;
        }

        public String getNameRu() {
            return nameRu;
        }

        public int getRank() {
            return rank;
        }
    }

    private final HandType type;
    private final int primaryRank;
    private final int secondaryRank;
    private final int sum;

    public DiceCombination(HandType type, int primaryRank, int secondaryRank, int sum) {
        this.type = type;
        this.primaryRank = primaryRank;
        this.secondaryRank = secondaryRank;
        this.sum = sum;
    }

    public HandType getType() {
        return type;
    }

    public int getSum() {
        return sum;
    }

    public static DiceCombination evaluate(int[] dice) {
        if (dice == null || dice.length == 0) {
            return new DiceCombination(HandType.HIGH_DIE, 0, 0, 0);
        }

        Map<Integer, Integer> counts = new HashMap<>();
        int sum = 0;
        for (int d : dice) {
            counts.put(d, counts.getOrDefault(d, 0) + 1);
            sum += d;
        }

        // Sort counts by frequency descending, then by die value descending
        List<Map.Entry<Integer, Integer>> sorted = new ArrayList<>(counts.entrySet());
        sorted.sort((a, b) -> {
            int cmp = Integer.compare(b.getValue(), a.getValue());
            if (cmp != 0) return cmp;
            return Integer.compare(b.getKey(), a.getKey());
        });

        int maxFreq = sorted.get(0).getValue();
        int maxVal = sorted.get(0).getKey();

        if (maxFreq == 5) {
            return new DiceCombination(HandType.FIVE_OF_A_KIND, maxVal, 0, sum);
        }
        if (maxFreq == 4) {
            return new DiceCombination(HandType.FOUR_OF_A_KIND, maxVal, 0, sum);
        }
        if (maxFreq == 3 && sorted.size() >= 2 && sorted.get(1).getValue() == 2) {
            return new DiceCombination(HandType.FULL_HOUSE, maxVal, sorted.get(1).getKey(), sum);
        }

        // Straights
        int[] sortedDice = dice.clone();
        Arrays.sort(sortedDice);
        if (Arrays.equals(sortedDice, new int[]{1, 2, 3, 4, 5})) {
            return new DiceCombination(HandType.SMALL_STRAIGHT, 5, 0, sum);
        }
        if (Arrays.equals(sortedDice, new int[]{2, 3, 4, 5, 6})) {
            return new DiceCombination(HandType.LARGE_STRAIGHT, 6, 0, sum);
        }

        if (maxFreq == 3) {
            return new DiceCombination(HandType.THREE_OF_A_KIND, maxVal, 0, sum);
        }
        if (maxFreq == 2 && sorted.size() >= 2 && sorted.get(1).getValue() == 2) {
            return new DiceCombination(HandType.TWO_PAIR, Math.max(maxVal, sorted.get(1).getKey()), Math.min(maxVal, sorted.get(1).getKey()), sum);
        }
        if (maxFreq == 2) {
            return new DiceCombination(HandType.ONE_PAIR, maxVal, 0, sum);
        }

        return new DiceCombination(HandType.HIGH_DIE, sortedDice[sortedDice.length - 1], 0, sum);
    }

    @Override
    public int compareTo(DiceCombination o) {
        if (this.type.getRank() != o.type.getRank()) {
            return Integer.compare(this.type.getRank(), o.type.getRank());
        }
        if (this.primaryRank != o.primaryRank) {
            return Integer.compare(this.primaryRank, o.primaryRank);
        }
        if (this.secondaryRank != o.secondaryRank) {
            return Integer.compare(this.secondaryRank, o.secondaryRank);
        }
        return Integer.compare(this.sum, o.sum);
    }
}
