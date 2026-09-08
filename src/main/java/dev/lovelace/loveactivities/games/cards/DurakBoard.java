package dev.lovelace.loveactivities.games.cards;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DurakBoard {

    public record TablePair(PlayingCard attack, PlayingCard defense) {}

    private final List<PlayingCard> deck = new ArrayList<>();
    private PlayingCard trumpCard;
    private final List<PlayingCard> discardPile = new ArrayList<>();
    private final List<TablePair> table = new ArrayList<>();

    public DurakBoard() {
        setupDeck();
    }

    public void setupDeck() {
        deck.clear();
        discardPile.clear();
        table.clear();

        for (PlayingCard.Suit s : PlayingCard.Suit.values()) {
            for (PlayingCard.Rank r : PlayingCard.Rank.values()) {
                if (r.getValue() >= 6) {
                    deck.add(new PlayingCard(s, r));
                }
            }
        }
        Collections.shuffle(deck);

        // Last card is trump
        trumpCard = deck.get(deck.size() - 1);
    }

    public PlayingCard getTrumpCard() {
        return trumpCard;
    }

    public PlayingCard.Suit getTrumpSuit() {
        return trumpCard != null ? trumpCard.getSuit() : PlayingCard.Suit.SPADES;
    }

    public List<PlayingCard> getDeck() {
        return deck;
    }

    public List<TablePair> getTable() {
        return table;
    }

    public int getDeckRemaining() {
        return deck.size();
    }

    public PlayingCard drawCard() {
        if (deck.isEmpty()) return null;
        return deck.remove(0);
    }

    public List<PlayingCard> refillHand(List<PlayingCard> hand) {
        List<PlayingCard> drawn = new ArrayList<>();
        while (hand.size() < 6 && !deck.isEmpty()) {
            PlayingCard c = drawCard();
            if (c != null) {
                hand.add(c);
                drawn.add(c);
            }
        }
        return drawn;
    }

    public int getUndefendedCount() {
        int count = 0;
        for (TablePair pair : table) {
            if (pair.defense() == null) count++;
        }
        return count;
    }

    public boolean canAttackWith(PlayingCard card, int defenderHandSize) {
        if (table.size() >= 6) return false;
        if (getUndefendedCount() >= defenderHandSize) return false;
        if (table.isEmpty()) return defenderHandSize > 0;

        for (TablePair pair : table) {
            if (pair.attack().getRank() == card.getRank()) return true;
            if (pair.defense() != null && pair.defense().getRank() == card.getRank()) return true;
        }
        return false;
    }

    public boolean attack(PlayingCard card, int defenderHandSize) {
        if (!canAttackWith(card, defenderHandSize)) return false;
        table.add(new TablePair(card, null));
        return true;
    }

    public boolean defend(int tableIndex, PlayingCard defCard) {
        if (tableIndex < 0 || tableIndex >= table.size()) return false;
        TablePair pair = table.get(tableIndex);
        if (pair.defense() != null) return false;

        if (defCard.canBeat(pair.attack(), getTrumpSuit())) {
            table.set(tableIndex, new TablePair(pair.attack(), defCard));
            return true;
        }
        return false;
    }

    public boolean isAllDefended() {
        if (table.isEmpty()) return false;
        for (TablePair pair : table) {
            if (pair.defense() == null) return false;
        }
        return true;
    }

    public void bito() {
        for (TablePair pair : table) {
            discardPile.add(pair.attack());
            if (pair.defense() != null) discardPile.add(pair.defense());
        }
        table.clear();
    }

    public List<PlayingCard> take(List<PlayingCard> defenderHand) {
        List<PlayingCard> taken = new ArrayList<>();
        for (TablePair pair : table) {
            defenderHand.add(pair.attack());
            taken.add(pair.attack());
            if (pair.defense() != null) {
                defenderHand.add(pair.defense());
                taken.add(pair.defense());
            }
        }
        table.clear();
        return taken;
    }
}
