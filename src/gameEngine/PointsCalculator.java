package gameEngine;
import cards.*;
import java.util.ArrayList;
public class PointsCalculator {
    public int calculate(ArrayList<Card> cards, String gameType, int numberOfPlayers) {
        int totalSum = 0;
        switch (gameType) {

            case "TOTALE+": {

                totalSum += cards.size() / numberOfPlayers;
                for (Card card : cards) {
                    if (card.getCardSuit() == 'D') {
                        totalSum += 10;
                    }
                }

                for (Card card : cards) {
                    if (card.getCardNumber() == 12) {
                        totalSum += 30;
                    }
                }

                for (Card card : cards) {
                    if (card.getCardSuit() == 'H' && card.getCardNumber() == 14) {
                        totalSum += 150;
                    }
                }
                break;
            }
            case "TOTALE-": {

                totalSum -= cards.size() / numberOfPlayers;
                for (Card card : cards) {
                    if (card.getCardSuit() == 'D') {
                        totalSum -= 10;
                    }
                }

                for (Card card : cards) {
                    if (card.getCardNumber() == 12) {
                        totalSum -= 30;
                    }
                }

                for (Card card : cards) {
                    if (card.getCardSuit() == 'H' && card.getCardNumber() == 14) {
                        totalSum -= 150;
                    }
                }
                break;
            }
            case "DIAMONDS": {

                for (Card card : cards) {
                    if (card.getCardSuit() == 'D') {
                        totalSum -= 10;
                    }
                }
                break;
            }
            case "QUINS": {

                for (Card card : cards) {
                    if (card.getCardNumber() == 12) {
                        totalSum -= 30;
                    }
                }
                break;
            }
            case "HEARTS_KING": {

                for (Card card : cards) {
                    if (card.getCardSuit() == 'H' && card.getCardNumber() == 14) {
                        totalSum -= 150;
                    }
                }
                break;
            }
            case "LEVATE": {

                totalSum -= cards.size() / numberOfPlayers;
                break;
            }
            case "WHIST": {

                totalSum += cards.size() / numberOfPlayers;
                break;
            }
            default:{break;}
        }
        return totalSum;
    }
}