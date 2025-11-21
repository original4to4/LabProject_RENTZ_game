package gameEngine;
import cards.*;
import user.*;
import java.util.Map;

import java.util.Map;

// Simplified Listener interface without onGameSelected
public interface GameSessionListener {
    void onGameStarted();
    void onCardPlayed(Card card);
    void onRoundCompleted(InGamePlayer winner);
    void onGameOver(Map<InGamePlayer, Integer> scores);
    void onPlayerChanged(InGamePlayer currentPlayer);
    void onGameSelected(GameType gameType);
    void onPlayerReady(String playerName);  // Make sure this method is declared
}