package GUI;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import user.Player;
import user.InGamePlayer;
import gameEngine.GameSession;
import gameEngine.GameType;

/**
 * Manages the sequential login flow for local multiplayer games
 */
public class PlayerLoginFlow {
    private GameType selectedGameType;
    private int requiredPlayerCount;
    private Frame parentFrame;
    private ArrayList<Player> loggedInPlayers;
    private Set<String> usedUsernames;
    private JDialog progressDialog;
    private JLabel progressLabel;

    public PlayerLoginFlow(Frame parentFrame, GameType gameType, int playerCount) {
        this.parentFrame = parentFrame;
        this.selectedGameType = gameType;
        this.requiredPlayerCount = playerCount;
        this.loggedInPlayers = new ArrayList<>();
        this.usedUsernames = new HashSet<>();
    }

    /**
     * Starts the sequential login process
     */
    public void startLoginFlow() {
        SwingUtilities.invokeLater(() -> {
            showProgressDialog();
            startNextLogin(1);
        });
    }

    private void showProgressDialog() {
        progressDialog = new JDialog(parentFrame, "Setting Up Game", true);
        progressDialog.setSize(400, 150);
        progressDialog.setLayout(new BorderLayout());
        progressDialog.setLocationRelativeTo(parentFrame);

        // Title
        JLabel titleLabel = new JLabel("Setting Up Local Multiplayer Game", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        progressDialog.add(titleLabel, BorderLayout.NORTH);

        // Progress info
        progressLabel = new JLabel("", JLabel.CENTER);
        progressLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        progressLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        progressDialog.add(progressLabel, BorderLayout.CENTER);

        // Cancel button
        JPanel buttonPanel = new JPanel();
        JButton cancelButton = new JButton("Cancel Setup");
        cancelButton.addActionListener(e -> {
            progressDialog.dispose();
            // Return to admin screen
            new AdminScreen().display();
        });
        buttonPanel.add(cancelButton);
        progressDialog.add(buttonPanel, BorderLayout.SOUTH);

        progressDialog.setVisible(true);
    }

    private void updateProgressMessage(int currentPlayer, int totalPlayers) {
        String message = String.format(
                "<html>Waiting for Player %d of %d to login...<br>" +
                        "Game Type: %s<br>" +
                        "Players logged in: %d/%d</html>",
                currentPlayer, totalPlayers,
                selectedGameType.getDisplayName(),
                loggedInPlayers.size(), totalPlayers
        );
        progressLabel.setText(message);
    }

    private void startNextLogin(final int playerNumber) {
        updateProgressMessage(playerNumber, requiredPlayerCount);

        // Show login dialog for current player
        SwingUtilities.invokeLater(() -> {
            Player player = LoginDialog.showLoginDialog(parentFrame, playerNumber, requiredPlayerCount);

            if (player == null) {
                // Login was cancelled
                JOptionPane.showMessageDialog(progressDialog,
                        "Login cancelled for Player " + playerNumber +
                                "\nGame setup aborted.",
                        "Login Cancelled",
                        JOptionPane.WARNING_MESSAGE);
                progressDialog.dispose();
                new AdminScreen().display();
                return;
            }

            // Check if player is already logged in
            if (usedUsernames.contains(player.getName())) {
                JOptionPane.showMessageDialog(progressDialog,
                        "Player '" + player.getName() + "' is already logged in.\n" +
                                "Please use a different account.",
                        "Duplicate Player",
                        JOptionPane.ERROR_MESSAGE);
                // Retry same player number
                startNextLogin(playerNumber);
                return;
            }

            // Add player to logged in list
            loggedInPlayers.add(player);
            usedUsernames.add(player.getName());

            // Check if we have all players
            if (loggedInPlayers.size() < requiredPlayerCount) {
                // Continue with next player
                startNextLogin(playerNumber + 1);
            } else {
                // All players logged in, create game
                completeSetup();
            }
        });
    }

    private void completeSetup() {
        try {
            // Create GameSession with all players
            GameSession gameSession = new GameSession(loggedInPlayers);
            gameSession.setSelectedGame(selectedGameType);

            // Start the game
            boolean gameStarted = gameSession.startGame();
            if (!gameStarted) {
                throw new Exception("Failed to start game");
            }

            progressDialog.dispose();

            // Open GameScreen for each player
            for (InGamePlayer player : gameSession.getPlayers()) {
                openGameScreenForPlayer(gameSession, player);
            }

            // Show success message
            JOptionPane.showMessageDialog(parentFrame,
                    "Game successfully created!\n" +
                            "Game Type: " + selectedGameType.getDisplayName() + "\n" +
                            "Players: " + loggedInPlayers.size() + "\n" +
                            "Each player now has their own game window.",
                    "Game Created",
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception e) {
            progressDialog.dispose();
            JOptionPane.showMessageDialog(parentFrame,
                    "Failed to create game: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            new AdminScreen().display();
        }
    }

    private void openGameScreenForPlayer(GameSession gameSession, InGamePlayer player) {
        SwingUtilities.invokeLater(() -> {
            // Delay slightly to stagger window opening
            Timer timer = new Timer(200 * (gameSession.getPlayers().indexOf(player) + 1), e -> {
                try {
                    GameScreen gameScreen = new GameScreen(gameSession, player);
                    gameScreen.display();
                } catch (Exception ex) {
                    System.err.println("Failed to open game screen for " +
                            player.getPlayer().getName() + ": " + ex.getMessage());
                }
            });
            timer.setRepeats(false);
            timer.start();
        });
    }

    public ArrayList<Player> getLoggedInPlayers() {
        return loggedInPlayers;
    }

    public GameType getSelectedGameType() {
        return selectedGameType;
    }

    /**
     * Static method to start the login flow
     */
    public static void startNewGame(Frame parentFrame, GameType gameType, int playerCount) {
        PlayerLoginFlow flow = new PlayerLoginFlow(parentFrame, gameType, playerCount);
        flow.startLoginFlow();
    }
}