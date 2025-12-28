/*
package network;

import user.Player;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

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

            // Send welcome message with client ID
            sendMessage(new GameMessage(GameMessage.WELCOME, "SERVER", clientId));

            // Send current session list
            sendSessionList();

            // Listen for messages from client
            while (connected) {
                try {
                    Object obj = input.readObject();
                    if (obj instanceof GameMessage) {
                        handleMessage((GameMessage) obj);
                    }
                } catch (EOFException e) {
                    break; // Client disconnected
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

                case GameMessage.CHAT_MESSAGE:
                    // Use broadcastToSession instead of private broadcastToAll
                    handleChatMessage(message);
                    break;

                case GameMessage.REQUEST_SESSIONS:
                    sendSessionList();
                    break;

                case GameMessage.PLAYER_READY:
                    handlePlayerReady(message);
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

        sendMessage(new GameMessage(
                GameMessage.PLAYER_JOINED,
                "SERVER",
                "Welcome " + playerName + "! You are connected to the game server."
        ));
    }

    private void handleJoinSession(GameMessage message) {
        if (!(message.getData() instanceof Integer)) return;

        int sessionId = (Integer) message.getData();
        boolean success = server.joinGameSession(sessionId, playerName, clientId);

        if (success) {
            sendMessage(new GameMessage(
                    GameMessage.JOIN_SUCCESS,
                    "SERVER",
                    sessionId
            ));
        } else {
            sendMessage(new GameMessage(
                    GameMessage.ERROR,
                    "SERVER",
                    "Failed to join session " + sessionId
            ));
        }
    }

    private void handleCreateSession(GameMessage message) {
        Object data = message.getData();
        if (data instanceof GameMessage.CreateSessionData) {
            GameMessage.CreateSessionData createData = (GameMessage.CreateSessionData) data;

            // Fix: Use playerCount instead of playerNames list
            int sessionId = server.createGameSession(createData.hostName, createData.playerCount);

            if (sessionId > 0) {
                // Auto-join the creator to the session
                server.joinGameSession(sessionId, createData.hostName, clientId);

                sendMessage(new GameMessage(
                        GameMessage.SESSION_CREATED,
                        "SERVER",
                        sessionId
                ));
            } else {
                sendMessage(new GameMessage(
                        GameMessage.ERROR,
                        "SERVER",
                        "Failed to create session"
                ));
            }
        } else {
            // Fallback for simple session creation
            int sessionId = server.createGameSession(playerName, 4); // Default 4 players
            if (sessionId > 0) {
                server.joinGameSession(sessionId, playerName, clientId);
                sendMessage(new GameMessage(GameMessage.SESSION_CREATED, "SERVER", sessionId));
            } else {
                sendMessage(new GameMessage(GameMessage.ERROR, "SERVER", "Failed to create session"));
            }
        }
    }

    private void handleStartGame(GameMessage message) {
        if (!(message.getData() instanceof Integer)) return;

        int sessionId = (Integer) message.getData();

        // Fix: Use the server's startGame method
        boolean success = server.startGame(sessionId, playerName);

        if (!success) {
            sendMessage(new GameMessage(
                    GameMessage.ERROR,
                    "SERVER",
                    "Failed to start game in session " + sessionId
            ));
        }
    }

    private void handleSelectGameType(GameMessage message) {
        if (!(message.getData() instanceof GameMessage.GameTypeData)) return;

        GameMessage.GameTypeData data = (GameMessage.GameTypeData) message.getData();
        boolean success = server.selectGameType(data.sessionId, playerName, data.gameType);

        if (!success) {
            sendMessage(new GameMessage(
                    GameMessage.ERROR,
                    "SERVER",
                    "Failed to select game type for session " + data.sessionId
            ));
        }
    }

    private void handlePlayCard(GameMessage message) {
        if (!(message.getData() instanceof GameMessage.CardPlayData)) return;

        GameMessage.CardPlayData data = (GameMessage.CardPlayData) message.getData();
        boolean success = server.playCard(data.sessionId, playerName, data.card);

        if (!success) {
            sendMessage(new GameMessage(
                    GameMessage.ERROR,
                    "SERVER",
                    "Failed to play card in session " + data.sessionId
            ));
        }
    }

    private void handleChatMessage(GameMessage message) {
        // Broadcast chat to the appropriate session instead of using private broadcastToAll
        String chatData = message.getSender() + ": " + message.getData();

        // Find which session the player is in and broadcast to that session
        for (Integer sessionId : server.getActiveSessionIds()) {
            if (server.isPlayerInSession(sessionId, playerName)) {
                server.broadcastToSession(sessionId,
                        new GameMessage(GameMessage.CHAT_MESSAGE, message.getSender(), message.getData()));
                break;
            }
        }
    }

    private void handlePlayerReady(GameMessage message) {
        Object data = message.getData();
        if (data instanceof Integer) {
            int sessionId = (Integer) data;
            server.setPlayerReady(sessionId, playerName);
        }
    }

    private void sendSessionList() {
        List<data_base_connection.GameSessionInfo> sessions = server.getActiveSessions();
        sendMessage(new GameMessage(
                GameMessage.SESSION_LIST,
                "SERVER",
                sessions
        ));
    }

    public void sendMessage(GameMessage message) {
        if (connected && output != null) {
            try {
                output.writeObject(message);
                output.flush();
                output.reset(); // Reset for same object references
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
            // Ignore during disconnect
        } finally {
            server.removeClient(clientId);
        }
    }

    // Getters
    public boolean isConnected() { return connected; }
    public String getClientId() { return clientId; }
    public String getPlayerName() { return playerName; }
}*/
