package dev.lovelace.loveactivities.games.cards;

import java.util.*;

public class PokerHandEvaluator {

    public enum HandRank {
        HIGH_CARD("Старшая карта", 1),
        ONE_PAIR("Пара", 2),
        TWO_PAIR("Две пары", 3),
        THREE_OF_A_KIND("Сет / Тройка", 4),
        STRAIGHT("Стрит", 5),
        FLUSH("Флеш", 6),
        FULL_HOUSE("Фулл-Хаус", 7),
        FOUR_OF_A_KIND("Каре", 8),
        STRAIGHT_FLUSH("Стрит-Флеш", 9),
        ROYAL_FLUSH("Роял-Флеш", 10);

        private final String nameRu;
        private final int score;

        HandRank(String nameRu, int score) {
            this.nameRu = nameRu;
            this.score = score;
        }

        public String getNameRu() {
            return nameRu;
        }

        public int getScore() {
            return score;
        }
    }

    public record PokerScore(HandRank rank, long tieBreaker, String description) implements Comparable<PokerScore> {
        @Override
        public int compareTo(PokerScore o) {
            if (this.rank.getScore() != o.rank.getScore()) {
                return Integer.compare(this.rank.getScore(), o.rank.getScore());
            }
            return Long.compare(this.tieBreaker, o.tieBreaker);
        }
    }

    /**
     * Кодирует значения карт в порядке значимости в одно сравнимое число (по основанию 15,
     * т.к. максимальное значение карты - 14/туз). Без этого два разных набора карт одного ранга
     * (например, пара тузов с разными кикерами) считались бы равными - см. историю бага.
     */
    private static long encodeTiebreaker(List<Integer> valuesInPriorityOrder) {
        long tie = 0;
        for (int v : valuesInPriorityOrder) {
            tie = tie * 15 + v;
        }
        return tie;
    }

    public static PokerScore evaluate7Cards(List<PlayingCard> cards) {
        if (cards == null || cards.isEmpty()) {
            return new PokerScore(HandRank.HIGH_CARD, 0, "Нет карт");
        }

        // Generate all 5-card subsets from 7 cards
        List<List<PlayingCard>> combos5 = new ArrayList<>();
        generateCombinations(cards, 5, 0, new ArrayList<>(), combos5);

        PokerScore best = null;
        for (List<PlayingCard> combo : combos5) {
            PokerScore score = evaluate5Cards(combo);
            if (best == null || score.compareTo(best) > 0) {
                best = score;
            }
        }
        return best != null ? best : new PokerScore(HandRank.HIGH_CARD, 0, "Старшая карта");
    }

    private static void generateCombinations(List<PlayingCard> input, int k, int start, List<PlayingCard> current, List<List<PlayingCard>> result) {
        if (current.size() == k) {
            result.add(new ArrayList<>(current));
            return;
        }
        for (int i = start; i < input.size(); i++) {
            current.add(input.get(i));
            generateCombinations(input, k, i + 1, current, result);
            current.remove(current.size() - 1);
        }
    }

    public static PokerScore evaluate5Cards(List<PlayingCard> cards) {
        if (cards.size() != 5) return new PokerScore(HandRank.HIGH_CARD, 0, "Неполная рука");

        List<PlayingCard> sorted = new ArrayList<>(cards);
        sorted.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

        boolean isFlush = isFlush(sorted);
        boolean isStraight = isStraight(sorted);
        int straightHigh = getStraightHigh(sorted);

        // Value frequency map
        Map<Integer, Integer> freq = new HashMap<>();
        for (PlayingCard c : sorted) {
            freq.put(c.getValue(), freq.getOrDefault(c.getValue(), 0) + 1);
        }

        // Royal & Straight Flush
        if (isFlush && isStraight) {
            if (straightHigh == 14) {
                return new PokerScore(HandRank.ROYAL_FLUSH, 14, "Роял-Флеш");
            }
            return new PokerScore(HandRank.STRAIGHT_FLUSH, straightHigh, "Стрит-Флеш (до " + straightHigh + ")");
        }

        List<Integer> allValuesDesc = new ArrayList<>();
        for (PlayingCard c : sorted) allValuesDesc.add(c.getValue());

        // Four of a Kind (4)
        for (Map.Entry<Integer, Integer> e : freq.entrySet()) {
            if (e.getValue() == 4) {
                int quadVal = e.getKey();
                int kicker = allValuesDesc.stream().filter(v -> v != quadVal).findFirst().orElse(0);
                return new PokerScore(HandRank.FOUR_OF_A_KIND, encodeTiebreaker(List.of(quadVal, kicker)), "Каре (" + quadVal + ")");
            }
        }

        // Full House (3 + 2)
        int threeVal = -1;
        int pairVal = -1;
        for (Map.Entry<Integer, Integer> e : freq.entrySet()) {
            if (e.getValue() == 3) threeVal = e.getKey();
            else if (e.getValue() == 2) pairVal = e.getKey();
        }
        if (threeVal != -1 && pairVal != -1) {
            return new PokerScore(HandRank.FULL_HOUSE, encodeTiebreaker(List.of(threeVal, pairVal)), "Фулл-Хаус (" + threeVal + " и " + pairVal + ")");
        }

        // Flush (все 5 карт значимы для кикеров)
        if (isFlush) {
            return new PokerScore(HandRank.FLUSH, encodeTiebreaker(allValuesDesc), "Флеш (" + sorted.get(0).getSuit().getNameRu() + ")");
        }

        // Straight
        if (isStraight) {
            return new PokerScore(HandRank.STRAIGHT, straightHigh, "Стрит (до " + straightHigh + ")");
        }

        // Three of a Kind
        if (threeVal != -1) {
            final int tv = threeVal;
            List<Integer> kickers = allValuesDesc.stream().filter(v -> v != tv).toList();
            List<Integer> tieValues = new ArrayList<>();
            tieValues.add(tv);
            tieValues.addAll(kickers);
            return new PokerScore(HandRank.THREE_OF_A_KIND, encodeTiebreaker(tieValues), "Сет / Тройка (" + tv + ")");
        }

        // Two Pair & One Pair
        List<Integer> pairs = new ArrayList<>();
        for (Map.Entry<Integer, Integer> e : freq.entrySet()) {
            if (e.getValue() == 2) pairs.add(e.getKey());
        }
        pairs.sort(Collections.reverseOrder());

        if (pairs.size() >= 2) {
            int highPair = pairs.get(0);
            int lowPair = pairs.get(1);
            int kicker = allValuesDesc.stream().filter(v -> v != highPair && v != lowPair).findFirst().orElse(0);
            return new PokerScore(HandRank.TWO_PAIR, encodeTiebreaker(List.of(highPair, lowPair, kicker)), "Две пары (" + highPair + " и " + lowPair + ")");
        } else if (pairs.size() == 1) {
            int pv = pairs.get(0);
            List<Integer> kickers = allValuesDesc.stream().filter(v -> v != pv).toList();
            List<Integer> tieValues = new ArrayList<>();
            tieValues.add(pv);
            tieValues.addAll(kickers);
            return new PokerScore(HandRank.ONE_PAIR, encodeTiebreaker(tieValues), "Пара (" + pv + ")");
        }

        return new PokerScore(HandRank.HIGH_CARD, encodeTiebreaker(allValuesDesc), "Старшая карта (" + sorted.get(0).getRank().getNameRu() + ")");
    }

    private static boolean isFlush(List<PlayingCard> sorted) {
        PlayingCard.Suit suit = sorted.get(0).getSuit();
        for (PlayingCard c : sorted) {
            if (c.getSuit() != suit) return false;
        }
        return true;
    }

    private static boolean isStraight(List<PlayingCard> sorted) {
        return getStraightHigh(sorted) > 0;
    }

    private static int getStraightHigh(List<PlayingCard> sorted) {
        int v0 = sorted.get(0).getValue();
        int v1 = sorted.get(1).getValue();
        int v2 = sorted.get(2).getValue();
        int v3 = sorted.get(3).getValue();
        int v4 = sorted.get(4).getValue();

        if (v0 == v1 + 1 && v1 == v2 + 1 && v2 == v3 + 1 && v3 == v4 + 1) {
            return v0;
        }
        // Wheel straight: Ace-2-3-4-5 (A=14, 5, 4, 3, 2)
        if (v0 == 14 && v1 == 5 && v2 == 4 && v3 == 3 && v4 == 2) {
            return 5;
        }
        return 0;
    }
}
