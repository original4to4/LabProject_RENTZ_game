/*
package network;

import gameEngine.GameType;
import cards.Card;
import user.InGamePlayer;
import data_base_connection.GameSessionInfo;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class GameMessage implements Serializable {
    private String type;
    private String sender;
    private Object data;
    private long timestamp;

    public GameMessage(String type, String sender, Object data) {
        this.type = type;
        this.sender = sender;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters
    public String getType() { return type; }
    public String getSender() { return sender; }
    public Object getData() { return data; }
    public long getTimestamp() { return timestamp; }

    // === MESSAGE TYPES ===

    // Connection & Authentication
    public static final String WELCOME = "WELCOME";
    public static final String PLAYER_JOIN = "PLAYER_JOIN";
    public static final String PLAYER_JOINED = "PLAYER_JOINED";
    public static final String PLAYER_LEAVE = "PLAYER_LEAVE";
    public static final String AUTHENTICATION = "AUTHENTICATION";
    public static final String AUTH_SUCCESS = "AUTH_SUCCESS";
    public static final String AUTH_FAILED = "AUTH_FAILED";

    // Session Management
    public static final String REQUEST_SESSIONS = "REQUEST_SESSIONS";
    public static final String SESSION_LIST = "SESSION_LIST";
    public static final String CREATE_SESSION = "CREATE_SESSION";
    public static final String SESSION_CREATED = "SESSION_CREATED";
    public static final String JOIN_SESSION = "JOIN_SESSION";
    public static final String JOIN_SUCCESS = "JOIN_SUCCESS";
    public static final String JOIN_FAILED = "JOIN_FAILED";
    public static final String SESSION_UPDATE = "SESSION_UPDATE";
    public static final String SESSION_CLOSED = "SESSION_CLOSED";

    // Game Flow
    public static final String START_GAME = "START_GAME";
    public static final String GAME_STARTED = "GAME_STARTED";
    public static final String SELECT_GAME_TYPE = "SELECT_GAME_TYPE";
    public static final String GAME_TYPE_SELECTED = "GAME_TYPE_SELECTED";
    public static final String PLAY_CARD = "PLAY_CARD";
    public static final String CARD_PLAYED = "CARD_PLAYED";
    public static final String ROUND_COMPLETED = "ROUND_COMPLETED";
    public static final String GAME_OVER = "GAME_OVER";
    public static final String GAME_STATE_UPDATE = "GAME_STATE_UPDATE";

    // Player Actions
    public static final String PLAYER_READY = "PLAYER_READY";
    public static final String PLAYER_TURN = "PLAYER_TURN";
    public static final String PLAYER_HAND = "PLAYER_HAND";
    public static final String PLAYER_SCORE_UPDATE = "PLAYER_SCORE_UPDATE";

    // Real-time Updates
    public static final String CURRENT_ROUND_UPDATE = "CURRENT_ROUND_UPDATE";
    public static final String LEADING_SUIT_UPDATE = "LEADING_SUIT_UPDATE";
    public static final String PLAYER_CONNECTED = "PLAYER_CONNECTED";
    public static final String PLAYER_DISCONNECTED = "PLAYER_DISCONNECTED";

    // Chat & System
    public static final String CHAT_MESSAGE = "CHAT_MESSAGE";
    public static final String SYSTEM_MESSAGE = "SYSTEM_MESSAGE";
    public static final String ERROR = "ERROR";
    public static final String PING = "PING";
    public static final String PONG = "PONG";

    @Override
    public String toString() {
        return "GameMessage{" +
                "type='" + type + '\'' +
                ", sender='" + sender + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }

    // === DATA CLASSES ===

    public static class SessionData implements Serializable {
        public final int sessionId;
        public final String hostName;
        public final int maxPlayers;
        public final int currentPlayers;
        public final String status;
        public final String gameType;

        public SessionData(int sessionId, String hostName, int maxPlayers,
                           int currentPlayers, String status, String gameType) {
            this.sessionId = sessionId;
            this.hostName = hostName;
            this.maxPlayers = maxPlayers;
            this.currentPlayers = currentPlayers;
            this.status = status;
            this.gameType = gameType;
        }

        @Override
        public String toString() {
            return String.format("Session %d: %s (%d/%d) - %s",
                    sessionId, hostName, currentPlayers, maxPlayers, status);
        }
    }

    public static class PlayerJoinData implements Serializable {
        public final int sessionId;
        public final String playerName;
        public final int currentPlayers;
        public final List<String> playersInSession;

        public PlayerJoinData(int sessionId, String playerName, int currentPlayers, List<String> playersInSession) {
            this.sessionId = sessionId;
            this.playerName = playerName;
            this.currentPlayers = currentPlayers;
            this.playersInSession = playersInSession;
        }
    }

    public static class GameStartData implements Serializable {
        public final int sessionId;
        public final List<String> players;
        public final Map<String, Integer> initialScores;

        public GameStartData(int sessionId, List<String> players, Map<String, Integer> initialScores) {
            this.sessionId = sessionId;
            this.players = players;
            this.initialScores = initialScores;
        }
    }

    public static class GameTypeData implements Serializable {
        public final int sessionId;
        public final GameType gameType;

        public GameTypeData(int sessionId, GameType gameType) {
            this.sessionId = sessionId;
            this.gameType = gameType;
        }
    }

    public static class CardPlayData implements Serializable {
        public final int sessionId;
        public final String playerName;
        public final Card card;
        public final boolean roundComplete;

        public CardPlayData(int sessionId, String playerName, Card card, boolean roundComplete) {
            this.sessionId = sessionId;
            this.playerName = playerName;
            this.card = card;
            this.roundComplete = roundComplete;
        }
    }

    public static class RoundCompleteData implements Serializable {
        public final int sessionId;
        public final String winnerName;
        public final Card winningCard;
        public final Map<String, Integer> scores;

        public RoundCompleteData(int sessionId, String winnerName, Card winningCard, Map<String, Integer> scores) {
            this.sessionId = sessionId;
            this.winnerName = winnerName;
            this.winningCard = winningCard;
            this.scores = scores;
        }
    }

    public static class GameOverData implements Serializable {
        public final int sessionId;
        public final Map<String, Integer> finalScores;
        public final String winnerName;

        public GameOverData(int sessionId, Map<String, Integer> finalScores, String winnerName) {
            this.sessionId = sessionId;
            this.finalScores = finalScores;
            this.winnerName = winnerName;
        }
    }

    public static class PlayerHandData implements Serializable {
        public final int sessionId;
        public final List<Card> hand;

        public PlayerHandData(int sessionId, List<Card> hand) {
            this.sessionId = sessionId;
            this.hand = hand;
        }
    }

    public static class GameStateData implements Serializable {
        public final int sessionId;
        public final String currentPlayer;
        public final int roundsPlayed;
        public final int totalRounds;
        public final char leadingSuit;
        public final List<Card> currentRound;
        public final Map<String, Integer> scores;
        public final boolean gameStarted;
        public final boolean roundInProgress;

        public GameStateData(int sessionId, String currentPlayer, int roundsPlayed,
                             int totalRounds, char leadingSuit, List<Card> currentRound,
                             Map<String, Integer> scores, boolean gameStarted, boolean roundInProgress) {
            this.sessionId = sessionId;
            this.currentPlayer = currentPlayer;
            this.roundsPlayed = roundsPlayed;
            this.totalRounds = totalRounds;
            this.leadingSuit = leadingSuit;
            this.currentRound = currentRound;
            this.scores = scores;
            this.gameStarted = gameStarted;
            this.roundInProgress = roundInProgress;
        }
    }

    public static class CreateSessionData implements Serializable {
        public final String hostName;
        public final int playerCount;
        public final List<String> playerNames;

        public CreateSessionData(String hostName, int playerCount, List<String> playerNames) {
            this.hostName = hostName;
            this.playerCount = playerCount;
            this.playerNames = playerNames;
        }
    }

    public static class ChatData implements Serializable {
        public final int sessionId;
        public final String playerName;
        public final String message;

        public ChatData(int sessionId, String playerName, String message) {
            this.sessionId = sessionId;
            this.playerName = playerName;
            this.message = message;
        }
    }
}*/
