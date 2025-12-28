package gameEngine;

import user.InGamePlayer;
import user.Player;
import cards.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * GameSession for local multiplayer
 * Simplified: No network support, only local play
 */
public class GameSession {
    private int sessionId;
    private GameType selectedGame;
    private ArrayList<InGamePlayer> players;
    private int currentPlayerIndex;
    private ArrayList<Card> currentRound;
    private char leadingSuit;
    private boolean gameStarted;
    private InGamePlayer roundWinner;
    private DeckOfCards deck;
    private int playerNumber;
    private int roundsPlayed;
    private int totalRounds;
    private Map<InGamePlayer, Integer> scores;
    private boolean roundInProgress;
    private List<GameSessionListener> listeners;
    private boolean gameCompleted;
    private InGamePlayer gameWinner;
    private int currentRoundStartPlayer; // Track which player started the current round
    private PointsCalculator pointsCalculator; // Points calculator instance

    // --- Constructors ---

    public GameSession(ArrayList<Player> players) {
        if (players == null || players.size() < 2 || players.size() > 6) {
            throw new IllegalArgumentException("Number of players must be between 2 and 6");
        }

        for (Player player : players) {
            if (player.getName() == null || player.getName().trim().isEmpty()) {
                throw new IllegalArgumentException("All players must have valid names");
            }
        }

        this.players = new ArrayList<>();
        for (Player player : players) {
            InGamePlayer inGamePlayer = new InGamePlayer(player);
            this.players.add(inGamePlayer);
        }

        this.playerNumber = this.players.size();
        this.deck = new DeckOfCards(playerNumber);
        this.pointsCalculator = new PointsCalculator(); // Initialize points calculator
        initCommon();
    }

    private void initCommon() {
        this.currentRound = new ArrayList<>();
        this.scores = new HashMap<>();
        this.listeners = new CopyOnWriteArrayList<>();
        this.totalRounds = 8;
        this.currentPlayerIndex = 0;
        this.currentRoundStartPlayer = 0; // Start with first player
        this.leadingSuit = 0;
        this.gameStarted = false;
        this.roundInProgress = false;
        this.gameCompleted = false;
        this.roundsPlayed = 0;

        for (InGamePlayer igp : this.players) {
            scores.put(igp, 0);
        }
    }

    // --- Meta ---
    public int getSessionId() { return sessionId; }
    public void setSessionId(int sessionId) { this.sessionId = sessionId; }

    public GameType getSelectedGame() { return selectedGame; }
    public void setSelectedGame(GameType selectedGame) {
        this.selectedGame = selectedGame;
        notifyGameSelected();
    }

    public List<InGamePlayer> getPlayers() {
        return Collections.unmodifiableList(players);
    }

    public InGamePlayer getCurrentPlayer() {
        if (players.isEmpty()) return null;
        return players.get(currentPlayerIndex);
    }

    public int getCurrentPlayerIndex() {
        return currentPlayerIndex;
    }

    public boolean isGameStarted() {
        return gameStarted;
    }

    public boolean isGameOver() {
        return gameCompleted;
    }

    public InGamePlayer getGameWinner() {
        return gameWinner;
    }

    public int getRoundsPlayed() {
        return roundsPlayed;
    }

    public int getTotalRounds() {
        return totalRounds;
    }

    public boolean isRoundInProgress() {
        return roundInProgress;
    }

    public Map<InGamePlayer, Integer> getScores() {
        return Collections.unmodifiableMap(scores);
    }

    public char getLeadingSuit() {
        return leadingSuit;
    }

    public String getLeadingSuitName() {
        switch(leadingSuit) {
            case 'H': return "Hearts ♥";
            case 'S': return "Spades ♠";
            case 'D': return "Diamonds ♦";
            case 'C': return "Clubs ♣";
            default: return "None";
        }
    }

    public ArrayList<Card> getCurrentRoundCards() {
        return new ArrayList<>(currentRound);
    }

    public Map<InGamePlayer, Card> getCurrentRoundCardsByPlayer() {
        Map<InGamePlayer, Card> result = new HashMap<>();

        for (int i = 0; i < currentRound.size(); i++) {
            // Calculate which player played this card (based on round start)
            int playerIndex = (currentRoundStartPlayer + i) % players.size();
            result.put(players.get(playerIndex), currentRound.get(i));
        }

        return result;
    }

    // --- Game lifecycle ---
    public synchronized boolean startGame() {
        if (gameStarted) return false;
        if (players.size() < 2) {
            System.out.println("Cannot start game: not enough players");
            return false;
        }

        this.gameStarted = true;
        this.roundsPlayed = 0;
        this.currentPlayerIndex = 0;
        this.currentRoundStartPlayer = 0; // First player starts
        this.totalRounds = 8;

        this.deck = new DeckOfCards(players.size());
        this.deck.shuffleDeck();

        for (InGamePlayer igp : players) {
            if (igp.getHand() == null) igp.setHand(new Hand());
            igp.getHand().clear();
            igp.getTakenCards().clear();
        }

        // Deal 8 cards to each player
        for (int r = 0; r < totalRounds; r++) {
            for (InGamePlayer igp : players) {
                Card c = deck.drawCard();
                if (c != null) igp.getHand().addCard(c);
            }
        }

        scores.clear();
        for (InGamePlayer igp : players) scores.put(igp, 0);

        notifyGameStarted();
        System.out.println("GameSession " + sessionId + " started with " + players.size() + " players");
        return true;
    }

    // --- Gameplay methods ---
    public synchronized boolean playCard(InGamePlayer player, Card card) {
        if (!gameStarted || gameCompleted) {
            return false;
        }

        if (!players.contains(player)) {
            return false;
        }

        // Check if it's this player's turn
        if (players.get(currentPlayerIndex) != player) {
            return false;
        }

        // Validate the move
        if (!isValidMove(player, card)) {
            return false;
        }

        // Remove card from player's hand
        player.getHand().removeCard(card);

        // Add to current round
        currentRound.add(card);

        // Set leading suit if first card of round
        if (currentRound.size() == 1) {
            leadingSuit = card.getCardSuit();
            roundInProgress = true;
            // Track who started this round
            currentRoundStartPlayer = currentPlayerIndex;
        }

        // Notify card played
        notifyCardPlayed(card);

        // Move to next player
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();

        // Check if round is complete
        if (currentRound.size() == players.size()) {
            completeRound();
        } else {
            // Notify player change
            notifyPlayerChanged(players.get(currentPlayerIndex));
        }

        return true;
    }

    private void completeRound() {
        // Determine round winner (highest card of leading suit)
        int winningCardIndex = 0;
        Card winningCard = currentRound.get(0);

        for (int i = 1; i < currentRound.size(); i++) {
            Card currentCard = currentRound.get(i);
            if (currentCard.getCardSuit() == leadingSuit) {
                if (currentCard.getCardNumber() > winningCard.getCardNumber()) {
                    winningCard = currentCard;
                    winningCardIndex = i;
                }
            }
        }

        // Calculate the actual winner player index
        int winnerPlayerIndex = (currentRoundStartPlayer + winningCardIndex) % players.size();
        roundWinner = players.get(winnerPlayerIndex);

        // Add cards to winner's taken cards
        for (Card card : currentRound) {
            roundWinner.getTakenCards().add(card);
        }

        // Calculate points for winner using the PointsCalculator
        if (selectedGame != null && pointsCalculator != null) {
            // Calculate points based on cards won THIS ROUND
            int roundPoints = pointsCalculator.calculate(currentRound,
                    selectedGame.getCode(),
                    players.size());

            // Update score
            int currentScore = scores.getOrDefault(roundWinner, 0);
            scores.put(roundWinner, currentScore + roundPoints);

            System.out.println("Round " + (roundsPlayed + 1) + " - " +
                    roundWinner.getPlayer().getName() + " won with " +
                    winningCard.description() + ". Points: " + roundPoints +
                    " (Total: " + (currentScore + roundPoints) + ")");
        } else {
            System.out.println("Warning: Game type not selected or points calculator not available");
        }

        // Notify round completed
        notifyRoundCompleted(roundWinner);

        // Reset for next round
        currentRound.clear();
        leadingSuit = 0;
        roundInProgress = false;
        roundsPlayed++;

        // Check if game is over
        if (roundsPlayed >= totalRounds) {
            endGame();
        } else {
            // Set next round's starting player to round winner
            currentPlayerIndex = winnerPlayerIndex;
            currentRoundStartPlayer = winnerPlayerIndex; // Winner starts next round
            notifyPlayerChanged(players.get(currentPlayerIndex));
        }
    }

    private void endGame() {
        gameCompleted = true;

        // Determine game winner (highest score)
        InGamePlayer winner = null;
        int highestScore = Integer.MIN_VALUE;
        boolean tie = false;
        List<InGamePlayer> tiedWinners = new ArrayList<>();

        for (Map.Entry<InGamePlayer, Integer> entry : scores.entrySet()) {
            int score = entry.getValue();
            if (score > highestScore) {
                highestScore = score;
                winner = entry.getKey();
                tiedWinners.clear();
                tiedWinners.add(entry.getKey());
                tie = false;
            } else if (score == highestScore) {
                tie = true;
                tiedWinners.add(entry.getKey());
            }
        }

        if (tie && tiedWinners.size() > 1) {
            // If there's a tie, winner is null (no single winner)
            gameWinner = null;
            System.out.println("Game over! Tie between: " +
                    tiedWinners.stream()
                            .map(p -> p.getPlayer().getName())
                            .reduce((a, b) -> a + ", " + b)
                            .orElse("") +
                    " with score: " + highestScore);
        } else {
            gameWinner = winner;
            System.out.println("Game over! Winner: " +
                    (winner != null ? winner.getPlayer().getName() : "None") +
                    " with score: " + highestScore);
        }

        // Calculate final game bonus (if any)
        if (selectedGame != null && pointsCalculator != null) {
            // Calculate any final game bonuses
            // This might include points for remaining cards or special conditions
            // For now, just use the accumulated scores
        }

        // Notify game over
        notifyGameOver(scores);
    }

    // --- Validation methods ---
    public boolean isValidMove(InGamePlayer player, Card card) {
        if (!player.getHand().hand.contains(card)) {
            return false;
        }

        // If no leading suit yet, any card is valid
        if (leadingSuit == 0) {
            return true;
        }

        // If player has cards of leading suit, must follow suit
        boolean hasLeadingSuit = false;
        for (Card c : player.getHand().hand) {
            if (c.getCardSuit() == leadingSuit) {
                hasLeadingSuit = true;
                break;
            }
        }

        if (hasLeadingSuit) {
            return card.getCardSuit() == leadingSuit;
        }

        // Player doesn't have leading suit, can play any card
        return true;
    }

    public boolean playerHasSuit(InGamePlayer player, char suit) {
        for (Card c : player.getHand().hand) {
            if (c.getCardSuit() == suit) {
                return true;
            }
        }
        return false;
    }

    // --- Listener management ---
    public void addListener(GameSessionListener l) {
        if (l != null) listeners.add(l);
    }

    public void removeListener(GameSessionListener l) {
        listeners.remove(l);
    }

    private void notifyGameStarted() {
        for (GameSessionListener l : listeners) {
            try { l.onGameStarted(); } catch (Exception ignored) {}
        }
    }

    private void notifyCardPlayed(Card c) {
        for (GameSessionListener l : listeners) {
            try { l.onCardPlayed(c); } catch (Exception ignored) {}
        }
    }

    private void notifyRoundCompleted(InGamePlayer winner) {
        for (GameSessionListener l : listeners) {
            try { l.onRoundCompleted(winner); } catch (Exception ignored) {}
        }
    }

    private void notifyGameOver(Map<InGamePlayer, Integer> scores) {
        for (GameSessionListener l : listeners) {
            try { l.onGameOver(scores); } catch (Exception ignored) {}
        }
    }

    private void notifyPlayerChanged(InGamePlayer currentPlayer) {
        for (GameSessionListener l : listeners) {
            try { l.onPlayerChanged(currentPlayer); } catch (Exception ignored) {}
        }
    }

    private void notifyGameSelected() {
        for (GameSessionListener l : listeners) {
            try { l.onGameSelected(selectedGame); } catch (Exception ignored) {}
        }
    }

    // --- Utility methods ---
    public Map<String, Integer> getScoresAsMap() {
        Map<String, Integer> map = new HashMap<>();
        for (Map.Entry<InGamePlayer, Integer> e : scores.entrySet()) {
            map.put(e.getKey().getPlayer().getName(), e.getValue());
        }
        return map;
    }

    public InGamePlayer getPlayerByName(String name) {
        for (InGamePlayer igp : players) {
            if (igp.getPlayer().getName().equals(name)) return igp;
        }
        return null;
    }

    public boolean isAIPlayer(String playerName) {
        // In local multiplayer, no AI players by default
        // This can be extended if AI support is added later
        return false;
    }

    // Get the winner's name for display (handles ties)
    public String getWinnerDisplayName() {
        if (gameWinner != null) {
            return gameWinner.getPlayer().getName();
        } else {
            // Find tied winners
            int highestScore = Integer.MIN_VALUE;
            List<String> tiedWinners = new ArrayList<>();

            for (Map.Entry<InGamePlayer, Integer> entry : scores.entrySet()) {
                int score = entry.getValue();
                if (score > highestScore) {
                    highestScore = score;
                    tiedWinners.clear();
                    tiedWinners.add(entry.getKey().getPlayer().getName());
                } else if (score == highestScore) {
                    tiedWinners.add(entry.getKey().getPlayer().getName());
                }
            }

            if (tiedWinners.size() == 1) {
                return tiedWinners.get(0);
            } else if (tiedWinners.size() > 1) {
                return String.join(", ", tiedWinners) + " (Tie)";
            }
        }
        return "No winner";
    }

    // For AI turn simulation (if needed in future)
    public void playAITurn() {
        // Not implemented for local multiplayer without AI
        // Could be added later
    }

    // Method to manually advance turn (for testing or special cases)
    public synchronized void advanceTurn() {
        if (!gameStarted || gameCompleted) return;

        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        notifyPlayerChanged(players.get(currentPlayerIndex));
    }

    // Get final scores for popup display
    public String getFinalScoresForDisplay() {
        StringBuilder sb = new StringBuilder("<html><b>Game Over! Final Scores:</b><br><br>");

        // Sort players by score (highest first)
        List<Map.Entry<InGamePlayer, Integer>> sortedEntries = new ArrayList<>(scores.entrySet());
        sortedEntries.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        for (Map.Entry<InGamePlayer, Integer> entry : sortedEntries) {
            sb.append("<b>").append(entry.getKey().getPlayer().getName()).append(":</b> ")
                    .append(entry.getValue()).append(" points<br>");
        }

        sb.append("<br><b>Winner: ").append(getWinnerDisplayName()).append("</b></html>");
        return sb.toString();
    }
}