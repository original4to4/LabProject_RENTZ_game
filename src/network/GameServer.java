package network;

import gameEngine.GameSession;
import gameEngine.GameType;
import data_base_connection.GameRepository;
import data_base_connection.GameSessionInfo;
import data_base_connection.UserRepository;
import user.Player;
import user.InGamePlayer;
import cards.Card;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * GameServer (phase 1 core)
 * - uses UserRepository.usernameExists(...) for player validation
 * - no chat support
 * - provides inner data classes (SessionData, PlayerJoinData, PlayerHandData, GameStartData...)
 */
public class GameServer {
    private ServerSocket serverSocket;
    private final Map<Integer, GameSession> activeSessions = new ConcurrentHashMap<>();
    private final Map<String, ClientThread> connectedClients = new ConcurrentHashMap<>();
    private final Map<Integer, List<String>> sessionPlayers = new ConcurrentHashMap<>(); // sessionId -> joined player names
    private final Map<Integer, String> sessionHosts = new ConcurrentHashMap<>(); // sessionId -> hostName
    private final GameRepository gameRepository;
    private final UserRepository userRepository;
    private boolean running = false;
    private int nextSessionId = 1000;

    public GameServer() {
        this.gameRepository = new GameRepository();
        this.userRepository = new UserRepository();
        System.out.println("GameServer initialized");
    }

    public void start(int port) {
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            System.out.println("🎮 GameServer started on port " + port);
            new Thread(this::acceptClients).start();
            new Thread(this::monitorConnections).start();
        } catch (IOException e) {
            System.err.println("❌ Failed to start GameServer: " + e.getMessage());
        }
    }

    private void acceptClients() {
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                String clientId = "client_" + System.currentTimeMillis() + "_" + new Random().nextInt(1000);
                System.out.println("🔗 New client connected: " + clientId);
                ClientThread clientThread = new ClientThread(clientSocket, clientId, this);
                connectedClients.put(clientId, clientThread);
                clientThread.start();
            } catch (IOException e) {
                if (running) System.err.println("Error accepting client: " + e.getMessage());
            }
        }
    }

    private void monitorConnections() {
        while (running) {
            try {
                Thread.sleep(30000);
                List<String> toRemove = new ArrayList<>();
                for (Map.Entry<String, ClientThread> entry : connectedClients.entrySet()) {
                    if (!entry.getValue().isConnected()) toRemove.add(entry.getKey());
                }
                for (String clientId : toRemove) {
                    connectedClients.remove(clientId);
                    System.out.println("🧹 Cleaned up disconnected client: " + clientId);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    // --- Session management ---

    public int createGameSession(String hostName, int playerCount) {
        if (hostName == null || hostName.trim().isEmpty()) {
            System.err.println("Cannot create session: empty hostName");
            return -1;
        }
        if (playerCount < 2 || playerCount > 6) {
            System.err.println("Invalid player count: " + playerCount);
            return -1;
        }

        int sessionId = nextSessionId++;
        try {
            GameSession session = new GameSession();
            session.setSessionId(sessionId);
            session.setNetworkGame(true);

            activeSessions.put(sessionId, session);
            sessionHosts.put(sessionId, hostName);
            sessionPlayers.put(sessionId, new ArrayList<>());

            // Persist skeleton session (optional)
            try {
                ArrayList<Player> placeholder = new ArrayList<>();
                placeholder.add(new Player(hostName, hostName + "@game.com", "host"));
                gameRepository.saveGameSession(session, placeholder);
            } catch (Exception e) {
                System.err.println("Warning: could not persist session skeleton: " + e.getMessage());
            }

            broadcastToAll(new GameMessage(GameMessage.SESSION_CREATED, "SERVER",
                    new SessionData(sessionId, hostName, playerCount, 0, "waiting", "Not selected")));

            System.out.println("🎯 Game session created: " + sessionId + " by " + hostName + " awaiting " + playerCount + " players");
            return sessionId;
        } catch (Exception e) {
            System.err.println("❌ Failed to create game session: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }

    public synchronized boolean joinGameSession(int sessionId, String playerName, String clientId) {
        GameSession session = activeSessions.get(sessionId);
        if (session == null) {
            System.out.println("❌ Session not found: " + sessionId);
            return false;
        }

        // Validate registered username
        if (!userRepository.usernameExists(playerName)) {
            System.out.println("❌ Player not registered, join rejected: " + playerName);
            return false;
        }

        List<String> joined = sessionPlayers.get(sessionId);
        if (joined.contains(playerName)) {
            System.out.println("❌ Player already in session: " + playerName);
            return false;
        }

        if (joined.size() >= 6) {
            System.out.println("❌ Session is full: " + sessionId);
            return false;
        }

        joined.add(playerName);

        if (session.getPlayerByName(playerName) == null) {
            Player newPlayer = new Player(playerName, playerName.toLowerCase() + "@game.com", "player123");
            session.addPlayer(newPlayer);
            System.out.println("➕ Created new player in session: " + playerName);
        }

        broadcastToSession(sessionId, new GameMessage(GameMessage.PLAYER_JOINED, "SERVER",
                new PlayerJoinData(sessionId, playerName, joined.size(), new ArrayList<>(joined))));

        System.out.println("🎯 Player " + playerName + " joined session " + sessionId);
        return true;
    }

    public synchronized boolean setPlayerReady(int sessionId, String playerName) {
        GameSession session = activeSessions.get(sessionId);
        if (session == null) {
            System.out.println("❌ setPlayerReady: session not found: " + sessionId);
            return false;
        }

        List<String> joined = sessionPlayers.get(sessionId);
        if (!joined.contains(playerName)) {
            System.out.println("❌ setPlayerReady: player not in session: " + playerName);
            return false;
        }

        session.setPlayerReady(playerName, true);

        broadcastToSession(sessionId, new GameMessage(GameMessage.PLAYER_READY, "SERVER",
                new PlayerJoinData(sessionId, playerName, joined.size(), new ArrayList<>(joined))));

        boolean allReady = session.areAllJoinedPlayersReady();
        if (allReady && joined.size() >= 2) {
            System.out.println("All players ready in session " + sessionId + " -> starting game");
            startGameInternal(sessionId);
        }
        return true;
    }

    private void startGameInternal(int sessionId) {
        GameSession session = activeSessions.get(sessionId);
        if (session == null) return;

        try {
            boolean started = session.startGame();
            if (!started) {
                System.out.println("startGameInternal: session.startGame() returned false");
                return;
            }

            for (InGamePlayer igp : session.getPlayers()) {
                String playerName = igp.getPlayer().getName();
                String playerClientId = getClientIdByPlayerName(playerName);
                if (playerClientId != null) {
                    sendToClient(playerClientId, new GameMessage(GameMessage.PLAYER_HAND, "SERVER",
                            new PlayerHandData(sessionId, new ArrayList<>(igp.getHand().getCards()))));
                }
            }

            broadcastToSession(sessionId, new GameMessage(GameMessage.GAME_STARTED, "SERVER",
                    new GameStartData(sessionId, getJoinedPlayersList(sessionId), session.getScoresAsMap())));

            System.out.println("🎮 Game started (session " + sessionId + ")");
        } catch (Exception e) {
            System.err.println("Error starting game for session " + sessionId + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    // --- Utilities ---
    public List<GameSessionInfo> getActiveSessions() {
        List<GameSessionInfo> sessions = new ArrayList<>();
        for (Map.Entry<Integer, GameSession> entry : activeSessions.entrySet()) {
            GameSession session = entry.getValue();
            String host = sessionHosts.getOrDefault(entry.getKey(), "UnknownHost");
            List<String> joined = sessionPlayers.getOrDefault(entry.getKey(), new ArrayList<>());
            sessions.add(new GameSessionInfo(
                    session.getSessionId(),
                    host,
                    session.getPlayers().size(),
                    joined.size(),
                    session.isGameStarted() ? "active" : "waiting",
                    new java.sql.Timestamp(System.currentTimeMillis()),
                    session.getSelectedGame() != null ? session.getSelectedGame().getDisplayName() : "Not selected"
            ));
        }
        return sessions;
    }

    public List<String> getSessionPlayers(int sessionId) {
        return sessionPlayers.getOrDefault(sessionId, new ArrayList<>());
    }

    public GameSession getGameSession(int sessionId) {
        return activeSessions.get(sessionId);
    }

    public String getClientIdByPlayerName(String playerName) {
        for (Map.Entry<String, ClientThread> entry : connectedClients.entrySet()) {
            if (playerName.equals(entry.getValue().getPlayerName())) {
                return entry.getKey();
            }
        }
        return null;
    }

    public void removeClient(String clientId) {
        ClientThread client = connectedClients.remove(clientId);
        if (client != null) {
            String playerName = client.getPlayerName();
            if (playerName != null) {
                for (Map.Entry<Integer, List<String>> entry : sessionPlayers.entrySet()) {
                    if (entry.getValue().remove(playerName)) {
                        System.out.println("👋 Removed player " + playerName + " from session " + entry.getKey());
                        broadcastToSession(entry.getKey(), new GameMessage(
                                GameMessage.PLAYER_LEAVE,
                                "SERVER",
                                playerName + " left the game"
                        ));
                    }
                }
            }
        }
        System.out.println("👋 Client disconnected: " + clientId);
    }

    public void stop() {
        running = false;
        try {
            for (ClientThread client : connectedClients.values()) client.disconnect();
            connectedClients.clear();
            if (serverSocket != null) serverSocket.close();
            System.out.println("🛑 GameServer stopped");
        } catch (IOException e) {
            System.err.println("Error stopping server: " + e.getMessage());
        }
    }

    // --- Messaging helpers ---
    private void broadcastToAll(GameMessage message) {
        for (ClientThread ct : connectedClients.values()) ct.sendMessage(message);
    }

    public void broadcastToSession(int sessionId, GameMessage message) {
        List<String> joined = sessionPlayers.getOrDefault(sessionId, new ArrayList<>());
        for (String playerName : joined) {
            String clientId = getClientIdByPlayerName(playerName);
            if (clientId != null) sendToClient(clientId, message);
        }
    }

    private void sendToClient(String clientId, GameMessage message) {
        ClientThread ct = connectedClients.get(clientId);
        if (ct != null) ct.sendMessage(message);
    }

    private List<String> getJoinedPlayersList(int sessionId) {
        return new ArrayList<>(sessionPlayers.getOrDefault(sessionId, new ArrayList<>()));
    }

    // === Inner ClientThread ===
    public class ClientThread extends Thread {
        private final Socket socket;
        private final String clientId;
        private final GameServer server;
        private ObjectOutputStream output;
        private ObjectInputStream input;
        private boolean connected = true;
        private String playerName;

        public ClientThread(Socket socket, String clientId, GameServer server) {
            this.socket = socket;
            this.clientId = clientId;
            this.server = server;
        }

        @Override
        public void run() {
            try {
                output = new ObjectOutputStream(socket.getOutputStream());
                input = new ObjectInputStream(socket.getInputStream());

                sendMessage(new GameMessage(GameMessage.WELCOME, "SERVER", clientId));
                sendSessionList();

                while (connected) {
                    try {
                        Object obj = input.readObject();
                        if (obj instanceof GameMessage) handleMessage((GameMessage) obj);
                    } catch (EOFException e) {
                        break;
                    } catch (ClassNotFoundException e) {
                        System.err.println("Invalid message from client " + clientId);
                    }
                }
            } catch (IOException e) {
                System.err.println("Client connection error: " + e.getMessage());
            } finally {
                disconnect();
            }
        }

        private void handleMessage(GameMessage message) {
            System.out.println("📨 From " + message.getSender() + ": " + message.getType());
            try {
                switch (message.getType()) {
                    case GameMessage.PLAYER_JOIN:
                        handlePlayerJoin(message);
                        break;
                    case GameMessage.JOIN_SESSION:
                        handleJoinSession(message);
                        break;
                    case GameMessage.CREATE_SESSION:
                        handleCreateSession(message);
                        break;
                    case GameMessage.START_GAME:
                        handleStartGame(message);
                        break;
                    case GameMessage.SELECT_GAME_TYPE:
                        handleSelectGameType(message);
                        break;
                    case GameMessage.PLAY_CARD:
                        handlePlayCard(message);
                        break;
                    case GameMessage.PLAYER_READY:
                        handlePlayerReady(message);
                        break;
                    case GameMessage.REQUEST_SESSIONS:
                        sendSessionList();
                        break;
                    default:
                        System.out.println("Unknown message type: " + message.getType());
                }
            } catch (Exception e) {
                System.err.println("Error handling message: " + e.getMessage());
                sendMessage(new GameMessage(GameMessage.ERROR, "SERVER", "Action failed: " + e.getMessage()));
            }
        }

        private void handlePlayerJoin(GameMessage message) {
            this.playerName = (String) message.getData();
            System.out.println("👤 Player identified: " + playerName + " (Client: " + clientId + ")");
            sendMessage(new GameMessage(GameMessage.WELCOME, "SERVER", "Hello " + playerName));
        }

        private void handleJoinSession(GameMessage message) {
            if (!(message.getData() instanceof Integer)) return;
            int sessionId = (Integer) message.getData();
            boolean success = joinGameSession(sessionId, playerName, clientId);
            if (!success) sendMessage(new GameMessage(GameMessage.ERROR, "SERVER", "Failed to join session " + sessionId));
        }

        private void handleCreateSession(GameMessage message) {
            Object data = message.getData();
            if (data instanceof GameMessage.CreateSessionData) {
                GameMessage.CreateSessionData d = (GameMessage.CreateSessionData) data;
                int sessionId = createGameSession(d.hostName, d.playerCount);
                if (sessionId > 0) {
                    joinGameSession(sessionId, d.hostName, clientId);
                    sendMessage(new GameMessage(GameMessage.SESSION_CREATED, "SERVER",
                            new SessionData(sessionId, d.hostName, d.playerCount, 0, "waiting", "Not selected")));
                } else {
                    sendMessage(new GameMessage(GameMessage.ERROR, "SERVER", "Failed to create session"));
                }
            } else {
                sendMessage(new GameMessage(GameMessage.ERROR, "SERVER", "Invalid create session payload"));
            }
        }

        private void handleStartGame(GameMessage message) {
            if (!(message.getData() instanceof Integer)) return;
            int sessionId = (Integer) message.getData();
            String host = sessionHosts.get(sessionId);
            if (host == null || !host.equals(playerName)) {
                sendMessage(new GameMessage(GameMessage.ERROR, "SERVER", "Only host can force start"));
                return;
            }
            startGameInternal(sessionId);
        }

        private void handleSelectGameType(GameMessage message) {
            if (!(message.getData() instanceof GameMessage.GameTypeData)) return;
            GameMessage.GameTypeData d = (GameMessage.GameTypeData) message.getData();
            selectGameType(d.sessionId, playerName, d.gameType);
        }

        private void handlePlayCard(GameMessage message) {
            if (!(message.getData() instanceof GameMessage.CardPlayData)) return;
            GameMessage.CardPlayData d = (GameMessage.CardPlayData) message.getData();
            playCard(d.sessionId, playerName, d.card);
        }

        private void handlePlayerReady(GameMessage message) {
            if (!(message.getData() instanceof Integer)) return;
            int sessionId = (Integer) message.getData();
            setPlayerReady(sessionId, playerName);
        }

        private void sendSessionList() {
            List<GameSessionInfo> sessions = server.getActiveSessions();
            sendMessage(new GameMessage(GameMessage.SESSION_LIST, "SERVER", sessions));
        }

        public void sendMessage(GameMessage message) {
            if (connected && output != null) {
                try {
                    output.writeObject(message);
                    output.flush();
                    output.reset();
                } catch (IOException e) {
                    System.err.println("Failed to send message to client " + clientId);
                    disconnect();
                }
            }
        }

        public void disconnect() {
            connected = false;
            try {
                if (output != null) output.close();
                if (input != null) input.close();
                if (socket != null) socket.close();
            } catch (IOException e) {
                // ignore
            } finally {
                server.removeClient(clientId);
            }
        }

        public boolean isConnected() { return connected; }
        public String getClientId() { return clientId; }
        public String getPlayerName() { return playerName; }
    }

    // --- Minimal game-related stubs ---
    public boolean selectGameType(int sessionId, String playerName, GameType gameType) {
        GameSession session = activeSessions.get(sessionId);
        if (session == null) return false;
        session.setSelectedGame(gameType);
        broadcastToSession(sessionId, new GameMessage(GameMessage.SELECT_GAME_TYPE, "SERVER",
                new GameMessage.GameTypeData(sessionId, gameType)));
        return true;
    }

    public boolean playCard(int sessionId, String playerName, Card card) {
        broadcastToSession(sessionId, new GameMessage(GameMessage.PLAY_CARD, playerName,
                new GameMessage.CardPlayData(sessionId, playerName, card, false)));
        return true;
    }

    // === Inner serializable data classes used by clients ===

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

    public static class PlayerHandData implements Serializable {
        public final int sessionId;
        public final List<Card> hand;

        public PlayerHandData(int sessionId, List<Card> hand) {
            this.sessionId = sessionId;
            this.hand = hand;
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

    public boolean startGame(int sessionId, String playerName) {
        GameSession session = activeSessions.get(sessionId);
        if (session == null) return false;

        // Check if player is host or has permission
        String host = sessionHosts.get(sessionId);
        if (host != null && host.equals(playerName)) {
            return session.startGame();
        }
        return false;
    }

    public List<Integer> getActiveSessionIds() {
        return new ArrayList<>(activeSessions.keySet());
    }

    public boolean isPlayerInSession(int sessionId, String playerName) {
        List<String> players = sessionPlayers.get(sessionId);
        return players != null && players.contains(playerName);
    }
}
