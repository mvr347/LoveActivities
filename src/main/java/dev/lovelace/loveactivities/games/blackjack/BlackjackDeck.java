package dev.lovelace.loveactivities.games.blackjack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BlackjackDeck {

    private final List<BlackjackCard> cards = new ArrayList<>();

    public BlackjackDeck() {
        resetAndShuffle();
    }

    public void resetAndShuffle() {
        cards.clear();
        for (BlackjackCard.Suit s : BlackjackCard.Suit.values()) {
            for (BlackjackCard.Rank r : BlackjackCard.Rank.values()) {
                cards.add(new BlackjackCard(s, r));
            }
        }
        Collections.shuffle(cards);
    }

    public BlackjackCard drawCard() {
        if (cards.isEmpty()) {
            resetAndShuffle();
        }
        return cards.remove(cards.size() - 1);
    }

    public static int calculateScore(List<BlackjackCard> hand) {
        int total = 0;
        int aces = 0;

        for (BlackjackCard card : hand) {
            total += card.getValue();
            if (card.getRank() == BlackjackCard.Rank.ACE) {
                aces++;
            }
        }

        while (total > 21 && aces > 0) {
            total -= 10;
            aces--;
        }

        return total;
    }

    public static boolean isBlackjack(List<BlackjackCard> hand) {
        return hand.size() == 2 && calculateScore(hand) == 21;
    }

    public static boolean isBust(List<BlackjackCard> hand) {
        return calculateScore(hand) > 21;
    }
}
