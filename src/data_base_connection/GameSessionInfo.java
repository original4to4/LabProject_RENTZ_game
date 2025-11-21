package data_base_connection;

import java.sql.Timestamp;

public class GameSessionInfo {
    private int sessionId;
    private String hostName;
    private int playerCount;
    private int joinedPlayers;
    private String gameState;
    private Timestamp createdAt;
    private String gameType;

    public GameSessionInfo(int sessionId, String hostName, int playerCount,
                           int joinedPlayers, String gameState, Timestamp createdAt) {
        this.sessionId = sessionId;
        this.hostName = hostName;
        this.playerCount = playerCount;
        this.joinedPlayers = joinedPlayers;
        this.gameState = gameState;
        this.createdAt = createdAt;
    }

    public GameSessionInfo(int sessionId, String hostName, int playerCount,
                           int joinedPlayers, String gameState, Timestamp createdAt, String gameType) {
        this(sessionId, hostName, playerCount, joinedPlayers, gameState, createdAt);
        this.gameType = gameType;
    }

    // Getters
    public int getSessionId() { return sessionId; }
    public String getHostName() { return hostName; }
    public int getPlayerCount() { return playerCount; }
    public int getJoinedPlayers() { return joinedPlayers; }
    public String getGameState() { return gameState; }
    public Timestamp getCreatedAt() { return createdAt; }
    public String getGameType() { return gameType; }

    @Override
    public String toString() {
        String baseString = String.format("Session %d: %s (%d/%d players) - %s",
                sessionId, hostName, joinedPlayers, playerCount, gameState);

        if (gameType != null && !gameType.isEmpty() && !gameType.equals("Not selected")) {
            baseString += " - " + gameType;
        }

        return baseString;
    }

    public String getFormattedDate() {
        if (createdAt == null) return "Unknown";
        return createdAt.toString();
    }
}