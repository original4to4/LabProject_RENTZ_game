package gameEngine;

import user.InGamePlayer;
import user.Player;
import cards.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class GameSession {
    private int sessionId;
    private GameType selectedGame;
    private ArrayList<InGamePlayer> players;
    private int currentPlayerIndex;
    private ArrayList<Card> currentRound;
    private Map<InGamePlayer, Card> currentRoundCards;
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
    private Map<String, Boolean> playerReadyStatus;
    private boolean isNetworkGame;

    // --- Constructors ---

    public GameSession(ArrayList<Player> players) {
        if (players == null || players.size() < 2 || players.size() > 6) {
            throw new IllegalArgumentException("Number of players must be between 2 and 6");
        }

        this.players = new ArrayList<>();
        for (Player player : players) {
            InGamePlayer inGamePlayer = new InGamePlayer(player);
            this.players.add(inGamePlayer);
        }

        this.playerNumber = this.players.size();
        this.deck = new DeckOfCards(playerNumber);
        initCommon();
    }

    public GameSession() {
        this.players = new ArrayList<>();
        this.playerNumber = 0;
        this.deck = null;
        initCommon();
    }

    private void initCommon() {
        this.currentRound = new ArrayList<>();
        this.currentRoundCards = new LinkedHashMap<>();
        this.scores = new HashMap<>();
        this.listeners = new CopyOnWriteArrayList<>();
        this.totalRounds = 8;
        this.playerReadyStatus = new HashMap<>();
        this.isNetworkGame = false;
        this.currentPlayerIndex = 0;
        this.leadingSuit = 0;
        this.gameStarted = false;
        this.roundInProgress = false;
        this.gameCompleted = false;
        this.roundsPlayed = 0;

        for (InGamePlayer igp : this.players) {
            scores.put(igp, 0);
            playerReadyStatus.put(igp.getPlayer().getName(), false);
        }
    }

    // --- Getters/Setters ---
    public int getSessionId() { return sessionId; }
    public void setSessionId(int sessionId) { this.sessionId = sessionId; }

    public GameType getSelectedGame() { return selectedGame; }
    public void setSelectedGame(GameType selectedGame) {
        this.selectedGame = selectedGame;
        notifyGameSelected(selectedGame);
    }

    public boolean isNetworkGame() { return isNetworkGame; }
    public void setNetworkGame(boolean networkGame) { isNetworkGame = networkGame; }

    public List<InGamePlayer> getPlayers() { return Collections.unmodifiableList(players); }

    public InGamePlayer getCurrentPlayer() {
        if (players.isEmpty()) return null;
        return players.get(currentPlayerIndex);
    }

    public char getLeadingSuit() { return leadingSuit; }

    public Map<InGamePlayer, Card> getCurrentRoundCards() {
        return Collections.unmodifiableMap(currentRoundCards);
    }

    public ArrayList<Card> getCurrentRound() { return currentRound; }

    public int getRoundsPlayed() { return roundsPlayed; }

    public int getTotalRounds() { return totalRounds; }

    public Map<InGamePlayer, Integer> getScores() { return Collections.unmodifiableMap(scores); }

    public boolean isRoundInProgress() { return roundInProgress; }

    public boolean isGameOver() { return gameCompleted; }

    public InGamePlayer getGameWinner() { return gameWinner; }

    // --- Player management ---

    public synchronized boolean addPlayer(Player player) {
        if (player == null) return false;
        if (players.size() >= 6) {
            System.out.println("Cannot add player - maximum players reached");
            return false;
        }
        for (InGamePlayer p : players) {
            if (p.getPlayer().getName().equals(player.getName())) {
                System.out.println("Player already exists in session: " + player.getName());
                return false;
            }
        }

        InGamePlayer newPlayer = new InGamePlayer(player);
        players.add(newPlayer);
        scores.put(newPlayer, 0);
        playerReadyStatus.put(player.getName(), false);

        this.playerNumber = players.size();
        if (!gameStarted) this.deck = new DeckOfCards(playerNumber);

        System.out.println("Added player to session: " + player.getName());
        return true;
    }

    public synchronized boolean removePlayerByName(String playerName) {
        Iterator<InGamePlayer> it = players.iterator();
        boolean removed = false;
        while (it.hasNext()) {
            InGamePlayer igp = it.next();
            if (igp.getPlayer().getName().equals(playerName)) {
                it.remove();
                scores.remove(igp);
                playerReadyStatus.remove(playerName);
                removed = true;
                break;
            }
        }
        if (removed) {
            this.playerNumber = players.size();
            this.deck = playerNumber > 0 ? new DeckOfCards(playerNumber) : null;
        }
        return removed;
    }

    public InGamePlayer getPlayerByName(String name) {
        for (InGamePlayer igp : players) {
            if (igp.getPlayer().getName().equals(name)) return igp;
        }
        return null;
    }

    // --- Ready tracking ---
    public synchronized void setPlayerReady(String playerName, boolean ready) {
        playerReadyStatus.put(playerName, ready);
        if (ready) notifyPlayerReady(playerName);
    }

    public void setPlayerReady(String playerName) {
        setPlayerReady(playerName, true);
    }

    public synchronized boolean areAllJoinedPlayersReady() {
        if (players.isEmpty()) return false;
        for (InGamePlayer igp : players) {
            Boolean r = playerReadyStatus.get(igp.getPlayer().getName());
            if (r == null || !r) return false;
        }
        return true;
    }

    public boolean allPlayersReady() {
        return areAllJoinedPlayersReady();
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
        this.totalRounds = 8;

        this.deck = new DeckOfCards(players.size());
        this.deck.shuffleDeck();

        for (InGamePlayer igp : players) {
            if (igp.getHand() == null) igp.setHand(new Hand());
            igp.getHand().clear();
            igp.getTakenCards().clear();
        }

        for (int r = 0; r < totalRounds; r++) {
            for (InGamePlayer igp : players) {
                Card c = deck.drawCard();
                if (c != null) igp.getHand().addCard(c);
            }
        }

        scores.clear();
        for (InGamePlayer igp : players) scores.put(igp, 0);

        roundInProgress = true;
        notifyGameStarted();
        System.out.println("GameSession " + sessionId + " started with " + players.size() + " players");
        return true;
    }

    public boolean isGameStarted() { return gameStarted; }

    // --- Game actions ---

    public void selectGame(GameType gameType, InGamePlayer player) {
        this.selectedGame = gameType;
        notifyGameSelected(gameType);
    }

    public void selectGame(GameType gameType) {
        this.selectedGame = gameType;
        notifyGameSelected(gameType);
    }

    public synchronized boolean playCard(InGamePlayer player, Card card) {
        return playCard(player, card, true);
    }

    public synchronized boolean playCard(InGamePlayer player, Card card, boolean notifyListeners) {
        if (!isValidMove(player, card)) {
            System.out.println("Invalid move by " + player.getPlayer().getName());
            return false;
        }

        // Remove card from player's hand
        player.getHand().removeCard(card);

        // Add to current round
        currentRound.add(card);
        currentRoundCards.put(player, card);

        // Set leading suit if this is first card
        if (currentRound.size() == 1) {
            leadingSuit = card.getCardSuit();
        }

        if (notifyListeners) {
            notifyCardPlayed(card);
        }

        // Check if round is complete
        if (currentRound.size() == players.size()) {
            completeRound();
            return true;
        }

        // Move to next player
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        notifyPlayerChanged(getCurrentPlayer());

        return false;
    }

    private void completeRound() {
        // Determine winner
        InGamePlayer winner = determineRoundWinner();
        roundWinner = winner;

        // Give cards to winner
        for (Card card : currentRound) {
            winner.getTakenCards().add(card);
        }

        // Calculate and update scores
        if (selectedGame != null) {
            PointsCalculator calculator = new PointsCalculator();
            int points = calculator.calculate(
                    new ArrayList<>(winner.getTakenCards()),
                    selectedGame.getCode(),
                    players.size()
            );
            scores.put(winner, points);
        }

        notifyRoundCompleted(winner);

        // Clear round
        currentRound.clear();
        currentRoundCards.clear();
        leadingSuit = 0;
        roundsPlayed++;

        // Check if game is over
        if (roundsPlayed >= totalRounds || allHandsEmpty()) {
            endGame();
        } else {
            // Winner starts next round
            currentPlayerIndex = players.indexOf(winner);
            notifyPlayerChanged(getCurrentPlayer());
        }
    }

    private InGamePlayer determineRoundWinner() {
        if (currentRound.isEmpty()) return null;

        Card winningCard = null;
        InGamePlayer winner = null;

        for (Map.Entry<InGamePlayer, Card> entry : currentRoundCards.entrySet()) {
            Card card = entry.getValue();

            if (winningCard == null) {
                winningCard = card;
                winner = entry.getKey();
            } else {
                // Card of leading suit with higher value wins
                if (card.getCardSuit() == leadingSuit) {
                    if (winningCard.getCardSuit() != leadingSuit ||
                            card.getCardNumber() > winningCard.getCardNumber()) {
                        winningCard = card;
                        winner = entry.getKey();
                    }
                }
            }
        }

        return winner;
    }

    private void endGame() {
        gameCompleted = true;
        roundInProgress = false;

        // Determine overall winner (highest score)
        InGamePlayer winner = null;
        int highestScore = Integer.MIN_VALUE;

        for (Map.Entry<InGamePlayer, Integer> entry : scores.entrySet()) {
            if (entry.getValue() > highestScore) {
                highestScore = entry.getValue();
                winner = entry.getKey();
            }
        }

        gameWinner = winner;
        notifyGameOver(scores);
    }

    private boolean allHandsEmpty() {
        for (InGamePlayer player : players) {
            if (!player.getHand().hand.isEmpty()) return false;
        }
        return true;
    }

    public boolean isValidMove(InGamePlayer player, Card card) {
        if (player != getCurrentPlayer()) return false;
        if (!player.getHand().hand.contains(card)) return false;
        if (selectedGame == null) return false;

        // If leading suit is set, must follow suit if possible
        if (leadingSuit != 0 && card.getCardSuit() != leadingSuit) {
            return !playerHasSuit(player, leadingSuit);
        }

        return true;
    }

    public boolean playerHasSuit(InGamePlayer player, char suit) {
        for (Card card : player.getHand().hand) {
            if (card.getCardSuit() == suit) return true;
        }
        return false;
    }

    public String getLeadingSuitName() {
        switch (leadingSuit) {
            case 'H': return "Hearts (♥)";
            case 'S': return "Spades (♠)";
            case 'D': return "Diamonds (♦)";
            case 'C': return "Clubs (♣)";
            default: return "None";
        }
    }

    public boolean isAIPlayer(String playerName) {
        // For now, no AI players in network mode
        return false;
    }

    public void playAITurn() {
        // Placeholder for AI logic
        InGamePlayer currentPlayer = getCurrentPlayer();
        if (currentPlayer != null && !currentPlayer.getHand().hand.isEmpty()) {
            Card card = currentPlayer.getHand().hand.get(0);
            playCard(currentPlayer, card);
        }
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
            try { l.onGameStarted(); } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private void notifyCardPlayed(Card c) {
        for (GameSessionListener l : listeners) {
            try { l.onCardPlayed(c); } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private void notifyRoundCompleted(InGamePlayer winner) {
        for (GameSessionListener l : listeners) {
            try { l.onRoundCompleted(winner); } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private void notifyGameOver(Map<InGamePlayer, Integer> scores) {
        for (GameSessionListener l : listeners) {
            try { l.onGameOver(scores); } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private void notifyPlayerChanged(InGamePlayer player) {
        for (GameSessionListener l : listeners) {
            try { l.onPlayerChanged(player); } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private void notifyGameSelected(GameType gameType) {
        for (GameSessionListener l : listeners) {
            try { l.onGameSelected(gameType); } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private void notifyPlayerReady(String playerName) {
        for (GameSessionListener l : listeners) {
            try { l.onPlayerReady(playerName); } catch (Exception e) { e.printStackTrace(); }
        }
    }

    // --- Helpers for server ---
    public Map<String, Integer> getScoresAsMap() {
        Map<String, Integer> map = new HashMap<>();
        for (Map.Entry<InGamePlayer, Integer> e : scores.entrySet()) {
            map.put(e.getKey().getPlayer().getName(), e.getValue());
        }
        return map;
    }

    public Map<String, Boolean> getPlayerReadyStatus() {
        return Collections.unmodifiableMap(playerReadyStatus);
    }
}
