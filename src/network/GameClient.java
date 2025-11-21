package network;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import javax.swing.*;

public class GameClient {
    private Socket socket;
    private ObjectOutputStream output;
    private ObjectInputStream input;
    private boolean connected = false;
    private String clientId;
    private String playerName;
    private BlockingQueue<GameMessage> messageQueue = new LinkedBlockingQueue<>();
    private MessageListener messageListener;

    public interface MessageListener {
        void onMessageReceived(GameMessage message);
        void onConnectionStatusChanged(boolean connected);
    }

    public GameClient(String playerName, MessageListener listener) {
        this.playerName = playerName;
        this.messageListener = listener;
        this.clientId = "client_" + System.currentTimeMillis();
    }

    public boolean connect(String host, int port) {
        try {
            socket = new Socket(host, port);
            output = new ObjectOutputStream(socket.getOutputStream());
            input = new ObjectInputStream(socket.getInputStream());
            connected = true;

            System.out.println("🔗 Connected to game server at " + host + ":" + port);

            // Start message receiver thread
            new Thread(this::receiveMessages).start();

            // Start message processor thread
            new Thread(this::processMessages).start();

            // Identify ourselves to the server
            sendMessage(new GameMessage(
                    GameMessage.PLAYER_JOIN,
                    playerName,
                    playerName
            ));

            if (messageListener != null) {
                SwingUtilities.invokeLater(() -> {
                    messageListener.onConnectionStatusChanged(true);
                });
            }

            return true;

        } catch (IOException e) {
            System.err.println("❌ Failed to connect to server: " + e.getMessage());
            if (messageListener != null) {
                SwingUtilities.invokeLater(() -> {
                    messageListener.onConnectionStatusChanged(false);
                });
            }
            return false;
        }
    }

    private void receiveMessages() {
        try {
            while (connected) {
                Object obj = input.readObject();
                if (obj instanceof GameMessage) {
                    messageQueue.put((GameMessage) obj);
                }
            }
        } catch (IOException | ClassNotFoundException | InterruptedException e) {
            if (connected) {
                System.err.println("📡 Disconnected from server: " + e.getMessage());
            }
            disconnect();
        }
    }

    private void processMessages() {
        try {
            while (connected) {
                GameMessage message = messageQueue.take();
                if (messageListener != null) {
                    SwingUtilities.invokeLater(() -> {
                        messageListener.onMessageReceived(message);
                    });
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // === Client Actions ===

    public void requestSessions() {
        sendMessage(new GameMessage(
                GameMessage.REQUEST_SESSIONS,
                playerName,
                null
        ));
    }

    public void createSession() {
        sendMessage(new GameMessage(
                GameMessage.CREATE_SESSION,
                playerName,
                null
        ));
    }

    public void joinSession(int sessionId) {
        sendMessage(new GameMessage(
                GameMessage.JOIN_SESSION,
                playerName,
                sessionId
        ));
    }

    public void startGame(int sessionId) {
        sendMessage(new GameMessage(
                GameMessage.START_GAME,
                playerName,
                sessionId
        ));
    }

    public void selectGameType(int sessionId, gameEngine.GameType gameType) {
        GameServer.GameTypeData data = new GameServer.GameTypeData(sessionId, gameType);
        sendMessage(new GameMessage(
                GameMessage.SELECT_GAME_TYPE,
                playerName,
                data
        ));
    }

    public void playCard(int sessionId, cards.Card card) {
        GameServer.CardPlayData data = new GameServer.CardPlayData(sessionId, playerName, card, false);
        sendMessage(new GameMessage(
                GameMessage.PLAY_CARD,
                playerName,
                data
        ));
    }

    public void sendChatMessage(String text) {
        sendMessage(new GameMessage(
                GameMessage.CHAT_MESSAGE,
                playerName,
                text
        ));
    }

    public void sendMessage(GameMessage message) {
        if (connected && output != null) {
            try {
                output.writeObject(message);
                output.flush();
                output.reset();
            } catch (IOException e) {
                System.err.println("❌ Failed to send message: " + e.getMessage());
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
            if (messageListener != null) {
                SwingUtilities.invokeLater(() -> {
                    messageListener.onConnectionStatusChanged(false);
                });
            }
            System.out.println("👋 Disconnected from game server");
        }
    }


    public void setMessageListener(MessageListener listener) {
        this.messageListener = listener;
    }

    // Getters
    public boolean isConnected() { return connected; }
    public String getClientId() { return clientId; }
    public String getPlayerName() { return playerName; }
}