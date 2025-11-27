package GUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import data_base_connection.UserRepository;
import network.GameServer;
import user.Player;
import gameEngine.GameSession;
import network.GameClient;
import network.GameMessage;

/**
 * AdminScreen updated to call UserRepository.usernameExists(...) and GameClient.getPlayerName()
 */
public class AdminScreen implements GameClient.MessageListener {
    private JFrame frame;
    private JPanel mainPanel;
    private JPanel playerSetupPanel;
    private JPanel controlPanel;
    private JSpinner playerCountSpinner;
    private ArrayList<JTextField> playerFields;
    private JButton createGameButton;
    private JButton backButton;
    private JLabel statusLabel;

    private UserRepository userRepository;
    private GameClient gameClient;
    private GameSession currentGameSession;
    private boolean connectedToServer = false;

    public AdminScreen() {
        this.userRepository = new UserRepository();
        this.gameClient = new GameClient("Admin", this);
        this.playerFields = new ArrayList<>();
        initialize();
        connectToServer();
    }

    public void display() { frame.setVisible(true); }

    private void initialize() {
        createFrame();
        createMainPanel();
        createPlayerSetupSection();
        createControlPanel();
        frame.add(mainPanel);
        frame.setLocationRelativeTo(null);
    }

    private void connectToServer() {
        boolean success = gameClient.connect("localhost", 8080);
        connectedToServer = success;

        if (success) {
            statusLabel.setText("Connected to game server - Ready to create sessions");
            statusLabel.setForeground(Color.GREEN);
        } else {
            statusLabel.setText("Not connected to server - Local mode only");
            statusLabel.setForeground(Color.ORANGE);
        }
    }

    @Override
    public void onMessageReceived(GameMessage message) {
        SwingUtilities.invokeLater(() -> handleServerMessage(message));
    }

    @Override
    public void onConnectionStatusChanged(boolean connected) {
        connectedToServer = connected;
        statusLabel.setText(connected ? "Connected to server" : "Disconnected from server");
        statusLabel.setForeground(connected ? Color.GREEN : Color.RED);
    }

    private void handleServerMessage(GameMessage message) {
        switch (message.getType()) {
            case GameMessage.SESSION_CREATED:
                handleSessionCreated(message);
                break;
            case GameMessage.ERROR:
                handleError(message);
                break;
            case GameMessage.WELCOME:
                statusLabel.setText("Connected to game server - Ready to create sessions");
                statusLabel.setForeground(Color.GREEN);
                break;
            default:
                // ignore other messages
        }
    }

    private void handleError(GameMessage message) {
        String payload = message.getData() != null ? message.getData().toString() : "Unknown error";
        JOptionPane.showMessageDialog(frame, "Server error: " + payload, "Server Error", JOptionPane.ERROR_MESSAGE);
    }

    private void handleSessionCreated(GameMessage message) {
        JOptionPane.showMessageDialog(frame, "Session created successfully", "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    private void createFrame() {
        frame = new JFrame("Admin - Create Game Session");
        frame.setSize(700, 500);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    private void createMainPanel() {
        mainPanel = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Create Game Session", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        mainPanel.add(title, BorderLayout.NORTH);
        playerSetupPanel = new JPanel();
        mainPanel.add(playerSetupPanel, BorderLayout.CENTER);
    }

    private void createPlayerSetupSection() {
        playerCountSpinner = new JSpinner(new SpinnerNumberModel(2, 2, 6, 1));
        playerCountSpinner.addChangeListener(e -> rebuildPlayerFields());
        playerFields.clear();
        rebuildPlayerFields();
        JPanel p = new JPanel(new GridLayout(0,1));
        p.add(new JLabel("Player count:"));
        p.add(playerCountSpinner);
        playerSetupPanel.add(p);
    }

    private void rebuildPlayerFields() {
        int count = (Integer) playerCountSpinner.getValue();
        playerFields.clear();
        playerSetupPanel.removeAll();
        JPanel p = new JPanel(new GridLayout(count + 1, 1, 4, 4));
        p.add(new JLabel("Enter usernames for each player (registered accounts only):"));
        for (int i = 0; i < count; i++) {
            JTextField tf = new JTextField();
            playerFields.add(tf);
            p.add(tf);
        }
        playerSetupPanel.add(p);
        playerSetupPanel.revalidate();
        playerSetupPanel.repaint();
    }

    private void createControlPanel() {
        controlPanel = new JPanel();
        createGameButton = new JButton("Create Session");
        createGameButton.addActionListener(e -> createGameSession());

        backButton = new JButton("Back");
        backButton.addActionListener(e -> frame.dispose());

        controlPanel.add(createGameButton);
        controlPanel.add(backButton);

        statusLabel = new JLabel("Not connected");
        mainPanel.add(controlPanel, BorderLayout.SOUTH);
        mainPanel.add(statusLabel, BorderLayout.SOUTH);
    }

    private void createGameSession() {
        int playerCount = (Integer) playerCountSpinner.getValue();
        List<String> playerNames = new ArrayList<>();

        if (playerFields.size() != playerCount) {

            JOptionPane.showMessageDialog(frame, "Player fields mismatch", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        for (int i = 0; i < playerCount; i++) {
            String username = playerFields.get(i).getText().trim();
            if (username.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "All player slots must be filled with registered usernames. No AI allowed.", "Validation error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!userRepository.usernameExists(username)) {
                JOptionPane.showMessageDialog(frame, "User '" + username + "' is not registered. Please register before adding them to the session.", "User not found", JOptionPane.ERROR_MESSAGE);
                return;
            }
            playerNames.add(username);
        }

        String hostName = gameClient.getPlayerName();
        if (hostName == null || hostName.trim().isEmpty()) hostName = "Admin";

        if (connectedToServer) {
            GameMessage.CreateSessionData data = new GameMessage.CreateSessionData(hostName, playerCount, playerNames);
            gameClient.sendMessage(new GameMessage(GameMessage.CREATE_SESSION, hostName, data));
            statusLabel.setText("Session creation requested...");
        } else {
            ArrayList<Player> players = new ArrayList<>();
            for (String uname : playerNames) players.add(new Player(uname, uname + "@game.com", "local"));
            GameSession localSession = new GameSession(players);
            localSession.setSessionId(0);
            localSession.setNetworkGame(false);
            localSession.startGame();
            JOptionPane.showMessageDialog(frame, "Local session created and started", "Local Mode", JOptionPane.INFORMATION_MESSAGE);
        }
    }
}
