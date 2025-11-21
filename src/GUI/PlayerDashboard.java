package GUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.ArrayList;
import data_base_connection.GameRepository;
import data_base_connection.GameSessionInfo;
import user.User;
import gameEngine.GameSession;
import user.InGamePlayer;
import user.Player;
import network.GameClient;
import network.GameMessage;

public class PlayerDashboard implements GameClient.MessageListener {
    private JFrame frame;
    private JPanel mainPanel;
    private JList<GameSessionInfo> sessionsList;
    private DefaultListModel<GameSessionInfo> listModel;
    private JButton joinButton;
    private JButton refreshButton;
    private JButton backButton;
    private JLabel statusLabel;
    private JLabel welcomeLabel;

    private GameRepository gameRepository;
    private GameClient gameClient;
    private User currentUser;
    private boolean connectedToServer = false;
    private int currentSessionId = -1;

    public PlayerDashboard(User user) {
        this.currentUser = user;
        this.gameRepository = new GameRepository();
        this.gameClient = new GameClient(user.getName(), this);
        initialize();
        connectToServer();
    }

    public void display() {
        frame.setVisible(true);
        refreshSessionsList();
    }

    private void initialize() {
        createFrame();
        createMainPanel();
        createWelcomePanel();
        createSessionsList();
        createControlPanel();

        frame.add(mainPanel);
        frame.setLocationRelativeTo(null);
    }

    private void connectToServer() {
        boolean success = gameClient.connect("localhost", 8080);
        connectedToServer = success;

        if (success) {
            // Request current sessions from server
            gameClient.requestSessions();
            statusLabel.setText("Connected to game server - Loading sessions...");
            System.out.println("✅ Connected to game server as: " + currentUser.getName());
        } else {
            statusLabel.setText("Using local sessions only");
            System.out.println("⚠️  Could not connect to server, using local sessions");
            refreshSessionsList(); // Fallback to local
        }
    }

    // Implement MessageListener
    @Override
    public void onMessageReceived(GameMessage message) {
        SwingUtilities.invokeLater(() -> {
            handleServerMessage(message);
        });
    }

    @Override
    public void onConnectionStatusChanged(boolean connected) {
        connectedToServer = connected;
        String status = connected ? "Connected to server" : "Disconnected from server";
        statusLabel.setText(status);

        if (connected) {
            // Request sessions when reconnected
            gameClient.requestSessions();
            System.out.println("✅ Reconnected to game server");
        } else {
            System.out.println("❌ Disconnected from game server");
            // Fallback to local sessions
            refreshSessionsList();
        }
    }

    private void handleServerMessage(GameMessage message) {
        System.out.println("📨 Received: " + message.getType());

        switch (message.getType()) {
            case GameMessage.SESSION_LIST:
                updateSessionsFromServer(message.getData());
                break;
            case GameMessage.SESSION_CREATED:
                refreshSessionsList(); // Refresh to show new session
                break;
            case GameMessage.JOIN_SUCCESS:
                handleJoinSuccess(message);
                break;
            case GameMessage.JOIN_FAILED:
                handleJoinFailed(message);
                break;
            case GameMessage.PLAYER_JOINED:
                handlePlayerJoined(message);
                break;
            case GameMessage.WELCOME:
                statusLabel.setText("Connected to game server - Loading sessions...");
                gameClient.requestSessions();
                break;
            case GameMessage.ERROR:
                handleError(message);
                break;
        }
    }

    private void updateSessionsFromServer(Object data) {
        if (data instanceof List) {
            List<GameMessage.SessionData> sessions = (List<GameMessage.SessionData>) data;
            listModel.clear();

            if (sessions.isEmpty()) {
                listModel.addElement(new GameSessionInfo(0, "No network sessions available", 0, 0, "none", null));
                joinButton.setEnabled(false);
            } else {
                for (GameMessage.SessionData session : sessions) {
                    // Convert to GameSessionInfo for display
                    GameSessionInfo sessionInfo = new GameSessionInfo(
                            session.sessionId,
                            session.hostName,
                            session.maxPlayers,
                            session.currentPlayers,
                            session.status,
                            new java.sql.Timestamp(System.currentTimeMillis()),
                            session.gameType
                    );
                    listModel.addElement(sessionInfo);
                }
                joinButton.setEnabled(true);
            }

            statusLabel.setText("Found " + sessions.size() + " network game sessions");
            System.out.println("📋 Loaded " + sessions.size() + " sessions from server");
        }
    }

    private void handleJoinSuccess(GameMessage message) {
        if (message.getData() instanceof Integer) {
            int sessionId = (Integer) message.getData();
            currentSessionId = sessionId;

            System.out.println("✅ Successfully joined session: " + sessionId);

            // Load the game session and open game screen
            GameSession gameSession = loadGameSession(sessionId);
            if (gameSession != null) {
                InGamePlayer currentPlayer = findOrCreatePlayerInSession(gameSession, currentUser);
                openGameScreen(gameSession, currentPlayer);
            } else {
                JOptionPane.showMessageDialog(frame,
                        "Failed to load game session. Please try again.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
        joinButton.setEnabled(true);
    }

    private void handleJoinFailed(GameMessage message) {
        String error = (String) message.getData();
        JOptionPane.showMessageDialog(frame,
                "Failed to join session: " + error,
                "Join Failed",
                JOptionPane.ERROR_MESSAGE);
        joinButton.setEnabled(true);
    }

    private void handlePlayerJoined(GameMessage message) {
        if (message.getData() instanceof GameMessage.PlayerJoinData) {
            GameMessage.PlayerJoinData data = (GameMessage.PlayerJoinData) message.getData();
            System.out.println("👤 Player " + data.playerName + " joined session " + data.sessionId);
            // Could update UI to show current players count
        }
    }

    private void handleError(GameMessage message) {
        String error = (String) message.getData();
        statusLabel.setText("Server error: " + error);
        System.err.println("❌ Server error: " + error);
    }

    private void createFrame() {
        frame = new JFrame("Player Dashboard - " + currentUser.getName());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setMinimumSize(new Dimension(700, 500));
    }

    private void createMainPanel() {
        mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(new Color(240, 240, 240));
    }

    private void createWelcomePanel() {
        JPanel welcomePanel = new JPanel(new BorderLayout());
        welcomePanel.setBackground(new Color(70, 130, 180));
        welcomePanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        welcomeLabel = new JLabel("Welcome, " + currentUser.getName() + "!", JLabel.LEFT);
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 20));
        welcomeLabel.setForeground(Color.WHITE);

        JLabel subtitleLabel = new JLabel("Available Game Sessions", JLabel.LEFT);
        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        subtitleLabel.setForeground(Color.WHITE);

        JLabel connectionLabel = new JLabel(
                connectedToServer ? "🔗 Connected to Server" : "⚠️  Local Mode Only",
                JLabel.RIGHT
        );
        connectionLabel.setFont(new Font("Arial", Font.BOLD, 12));
        connectionLabel.setForeground(Color.WHITE);

        JPanel textPanel = new JPanel(new GridLayout(2, 1));
        textPanel.setOpaque(false);
        textPanel.add(welcomeLabel);
        textPanel.add(subtitleLabel);

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.add(textPanel, BorderLayout.WEST);
        headerPanel.add(connectionLabel, BorderLayout.EAST);

        welcomePanel.add(headerPanel, BorderLayout.CENTER);
        mainPanel.add(welcomePanel, BorderLayout.NORTH);
    }

    private void createSessionsList() {
        listModel = new DefaultListModel<>();
        sessionsList = new JList<>(listModel);
        sessionsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        sessionsList.setFont(new Font("Arial", Font.PLAIN, 14));
        sessionsList.setCellRenderer(new SessionListRenderer());

        // Add double-click listener for quick joining
        sessionsList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    joinSelectedSession();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(sessionsList);
        scrollPane.setBorder(BorderFactory.createTitledBorder(
                "Available Game Sessions - Double-click to join quickly"));
        scrollPane.setPreferredSize(new Dimension(0, 300));
        mainPanel.add(scrollPane, BorderLayout.CENTER);
    }

    private void createControlPanel() {
        JPanel controlPanel = new JPanel(new BorderLayout());
        controlPanel.setBackground(Color.WHITE);
        controlPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        // Status label
        statusLabel = new JLabel("Select a game session to join", JLabel.CENTER);
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        controlPanel.add(statusLabel, BorderLayout.NORTH);

        // Buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        buttonPanel.setBackground(Color.WHITE);

        refreshButton = createStyledButton("Refresh", new Color(70, 130, 180));
        refreshButton.addActionListener(e -> refreshSessionsList());

        joinButton = createStyledButton("Join Session", new Color(0, 150, 0));
        joinButton.addActionListener(e -> joinSelectedSession());

        backButton = createStyledButton("Logout", new Color(150, 150, 150));
        backButton.addActionListener(e -> logout());

        buttonPanel.add(refreshButton);
        buttonPanel.add(joinButton);
        buttonPanel.add(backButton);

        controlPanel.add(buttonPanel, BorderLayout.CENTER);
        mainPanel.add(controlPanel, BorderLayout.SOUTH);
    }

    private JButton createStyledButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    private void refreshSessionsList() {
        if (connectedToServer) {
            // Request sessions from server
            gameClient.requestSessions();
            statusLabel.setText("Refreshing sessions from server...");
        } else {
            // Load local sessions
            loadLocalSessions();
        }
    }

    private void loadLocalSessions() {
        listModel.clear();
        List<GameSessionInfo> sessions = gameRepository.getActiveGameSessions();

        if (sessions.isEmpty()) {
            listModel.addElement(new GameSessionInfo(0, "No local game sessions available", 0, 0, "none", null));
            joinButton.setEnabled(false);
        } else {
            for (GameSessionInfo session : sessions) {
                // Only show sessions that are waiting for players or active
                if ("waiting".equals(session.getGameState()) || "active".equals(session.getGameState())) {
                    listModel.addElement(session);
                }
            }
            joinButton.setEnabled(true);
        }

        statusLabel.setText("Found " + listModel.size() + " local game sessions");
        System.out.println("📋 Loaded " + listModel.size() + " local sessions");
    }

    private void joinSelectedSession() {
        GameSessionInfo selected = sessionsList.getSelectedValue();
        if (selected == null || selected.getSessionId() == 0) {
            JOptionPane.showMessageDialog(frame, "Please select a valid game session");
            return;
        }

        if (connectedToServer) {
            // Join via server
            joinSessionViaServer(selected.getSessionId());
        } else {
            // Fallback to local join
            joinLocalSession(selected.getSessionId());
        }
    }

    private void joinSessionViaServer(int sessionId) {
        try {
            joinButton.setEnabled(false);
            statusLabel.setText("Joining session " + sessionId + " via network...");

            // Send join request to server
            gameClient.joinSession(sessionId);

            System.out.println("📤 Sent JOIN_SESSION request for session: " + sessionId);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame,
                    "Error joining game session: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            joinButton.setEnabled(true);
        }
    }

    private void joinLocalSession(int sessionId) {
        try {
            GameSession gameSession = loadGameSession(sessionId);
            if (gameSession != null) {
                InGamePlayer currentPlayer = findOrCreatePlayerInSession(gameSession, currentUser);
                if (currentPlayer != null) {
                    openGameScreen(gameSession, currentPlayer);
                } else {
                    JOptionPane.showMessageDialog(frame,
                            "Could not join the game session. Session may be full.",
                            "Join Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(frame,
                        "Session not found or no longer available.",
                        "Join Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame,
                    "Error joining game session: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private GameSession loadGameSession(int sessionId) {
        // Try to load from database first
        GameSession gameSession = gameRepository.loadGameSession(sessionId);

        if (gameSession == null && connectedToServer) {
            // If not found locally but connected to server, create a placeholder session
            System.out.println("🔄 Creating placeholder session for network game");
            gameSession = createPlaceholderSession(sessionId);
        }

        return gameSession;
    }

    private GameSession createPlaceholderSession(int sessionId) {
        // Create a minimal session for network play
        // In a real implementation, the server would send the full game state
        ArrayList<Player> players = new ArrayList<>();
        players.add(new Player(currentUser.getName(), currentUser.getEmail(), currentUser.getPassword()));

        GameSession gameSession = new GameSession(players);
        gameSession.setSessionId(sessionId);
        return gameSession;
    }

    private InGamePlayer findOrCreatePlayerInSession(GameSession gameSession, User user) {
        // First, try to find the player by username
        for (InGamePlayer player : gameSession.getPlayers()) {
            if (player.getPlayer().getName().equals(user.getName())) {
                return player;
            }
        }

        // If player not found, check if there's an empty slot
        if (gameSession.getPlayers().size() < 6) { // Max players
            // Create a new player and add to session
            Player newPlayer = new Player(user.getName(), user.getEmail(), user.getPassword());
            InGamePlayer newInGamePlayer = new InGamePlayer(newPlayer);

            // Note: This is a simplified approach. In a real application,
            // you would need to properly add the player to the game session
            return newInGamePlayer;
        }

        return null;
    }

    private void openGameScreen(GameSession gameSession, InGamePlayer currentPlayer) {
        frame.dispose();
        GameScreen gameScreen = new GameScreen(gameSession, currentPlayer, gameClient);
        gameScreen.display();
        System.out.println("🎮 Opened game screen for session: " + gameSession.getSessionId());
    }

    private void logout() {
        int confirm = JOptionPane.showConfirmDialog(
                frame,
                "Are you sure you want to logout?",
                "Confirm Logout",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            // Disconnect from server if connected
            if (gameClient != null) {
                gameClient.disconnect();
            }

            frame.dispose();
            new FirstScreen().display();
        }
    }

    // Custom list renderer for game sessions
    private static class SessionListRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value instanceof GameSessionInfo) {
                GameSessionInfo session = (GameSessionInfo) value;

                if (session.getSessionId() == 0) {
                    setText(session.toString());
                    setForeground(Color.GRAY);
                    setBackground(isSelected ? new Color(200, 200, 200) : Color.WHITE);
                } else {
                    setText(session.toString());

                    // Color coding based on status and player count
                    if ("active".equals(session.getGameState())) {
                        if (session.getJoinedPlayers() >= session.getPlayerCount()) {
                            setBackground(isSelected ? new Color(255, 200, 200) : new Color(255, 240, 240));
                            setForeground(Color.GRAY);
                            setToolTipText("Session is full");
                        } else {
                            setBackground(isSelected ? new Color(200, 255, 200) : new Color(240, 255, 240));
                            setForeground(Color.BLACK);
                            setToolTipText("Active game - " + session.getJoinedPlayers() + "/" + session.getPlayerCount() + " players");
                        }
                    } else if ("waiting".equals(session.getGameState())) {
                        if (session.getJoinedPlayers() >= session.getPlayerCount()) {
                            setBackground(isSelected ? new Color(255, 200, 200) : new Color(255, 240, 240));
                            setForeground(Color.GRAY);
                            setToolTipText("Session is full");
                        } else {
                            setBackground(isSelected ? new Color(255, 255, 200) : new Color(255, 255, 240));
                            setForeground(Color.BLACK);
                            setToolTipText("Waiting for players - " + session.getJoinedPlayers() + "/" + session.getPlayerCount() + " joined");
                        }
                    } else {
                        setBackground(isSelected ? new Color(255, 200, 200) : new Color(255, 240, 240));
                        setForeground(Color.GRAY);
                        setToolTipText("Session not available");
                    }
                }
            }

            return this;
        }
    }
}