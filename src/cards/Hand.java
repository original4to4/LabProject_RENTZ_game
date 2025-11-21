package cards;

import java.util.ArrayList;
import java.util.List;

/**
 * Hand - small convenience wrappers to match project expectations.
 */
public class Hand {
    public ArrayList<Card> hand = new ArrayList<Card>();

    public void addCard(Card card) {
        hand.add(card);
    }

    public void removeCard(Card card) {
        hand.remove(card);
    }

    /**
     * Clear the hand (convenience wrapper).
     */
    public void clear() {
        hand.clear();
    }

    /**
     * Return a defensive copy of the cards in hand.
     */
    public List<Card> getCards() {
        return new ArrayList<>(hand);
    }

    @Override
    public String toString() {
        return hand.toString();
    }
}
