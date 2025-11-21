package cards;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * DeckOfCards - matches your project's structure but adds drawCard().
 */
public class DeckOfCards {
    int numberOfCards;
    public ArrayList<Card> cards = new ArrayList<>();

    public DeckOfCards(int numberOfPlayers) {
        numberOfCards = 8 * numberOfPlayers;
        for (int i = 0; i <= numberOfPlayers * 2 - 1; i++) {
            cards.add(new Card(14 - i, 'H'));
            cards.add(new Card(14 - i, 'S'));
            cards.add(new Card(14 - i, 'D'));
            cards.add(new Card(14 - i, 'C'));
        }
    }

    public void afisareDeck() {
        for (Card card : cards) {
            System.out.println(card);
        }
    }

    public void shuffleDeck() {
        Collections.shuffle(cards);
    }

    /**
     * Draw top card (index 0) or return null if empty.
     */
    public Card drawCard() {
        if (cards.isEmpty()) return null;
        return cards.remove(0);
    }

    /**
     * Convenience: number of cards left.
     */
    public int remaining() {
        return cards.size();
    }

    /**
     * Defensive copy of cards (rarely needed but helpful).
     */
    public List<Card> getCardsList() {
        return new ArrayList<>(cards);
    }
}
