package GUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import cards.*;
import gameEngine.*;
import user.InGamePlayer;

/**
 * GameScreen for local multiplayer
 * Simplified UI with no network or chat components
 */
public class GameScreen implements GameSessionListener {
    private JFrame frame;
    private JPanel mainPanel;
    private JPanel playerHandPanel;
    private JPanel playedCardsPanel;
    private JPanel gameInfoPanel;
    private JPanel scorePanel;
    private JButton okButton;
    private JLabel gameInfoLabel;
    private JLabel currentPlayerLabel;
    private JLabel roundInfoLabel;
    private JLabel leadingSuitLabel;
    private JLabel statusLabel;
    private JLabel turnIndicatorLabel;

    private GameSession gameSession;
    private InGamePlayer currentUser;
    private Timer gameTimer;
    private boolean cardsEnabled = false;
    private boolean isUsersTurn = false;

    public GameScreen(GameSession gameSession, InGamePlayer currentUser) {
        this.gameSession = gameSession;
        this.currentUser = currentUser;

        if (this.gameSession != null) {
            this.gameSession.addListener(this);
        }

        initialize();
    }

    private void initialize() {
        createFrame();
        createMainPanel();
        createGameInfoPanel();
        createPlayedCardsPanel();
        createPlayerHandPanel();
        createScorePanel();
        setupGameTimer();

        frame.add(mainPanel);
        frame.setVisible(true);

        updateGameState();

        // Center the window with slight offset for each player
        positionWindow();
    }

    private void positionWindow() {
        int playerIndex = gameSession.getPlayers().indexOf(currentUser);
        int totalPlayers = gameSession.getPlayers().size();

        if (totalPlayers > 1 && playerIndex >= 0) {
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            int screenWidth = screenSize.width;
            int screenHeight = screenSize.height;

            // Arrange windows in a grid
            int cols = Math.min(totalPlayers, 2);
            int rows = (int) Math.ceil(totalPlayers / 2.0);
            int windowWidth = frame.getWidth();
            int windowHeight = frame.getHeight();

            int col = playerIndex % cols;
            int row = playerIndex / cols;

            int x = col * (windowWidth + 10) + 50;
            int y = row * (windowHeight + 10) + 50;

            // Ensure windows don't go off screen
            if (x + windowWidth > screenWidth) {
                x = screenWidth - windowWidth - 50;
            }
            if (y + windowHeight > screenHeight) {
                y = screenHeight - windowHeight - 50;
            }

            frame.setLocation(x, y);
        }
    }

    private void createFrame() {
        String title = "Card Game - " + (currentUser != null ? currentUser.getPlayer().getName() : "Player");
        frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(1200, 800);
        frame.setLocationRelativeTo(null);
        frame.setMinimumSize(new Dimension(1000, 700));
    }

    private void createMainPanel() {
        mainPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                try {
                    ImageIcon backgroundIcon = new ImageIcon("Pictures/background.jpg");
                    if (backgroundIcon.getIconWidth() != -1) {
                        Image backgroundImage = backgroundIcon.getImage();
                        g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
                    } else {
                        g.setColor(new Color(0, 100, 0));
                        g.fillRect(0, 0, getWidth(), getHeight());
                    }
                } catch (Exception e) {
                    g.setColor(new Color(0, 100, 0));
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
            }
        };
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }

    private void createGameInfoPanel() {
        gameInfoPanel = new JPanel(new BorderLayout());
        gameInfoPanel.setOpaque(false);
        gameInfoPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel topInfoPanel = new JPanel(new GridLayout(2, 3, 10, 5));
        topInfoPanel.setOpaque(false);

        gameInfoLabel = createInfoLabel("Game: Setting up...", Color.YELLOW, 16);
        currentPlayerLabel = createInfoLabel("Current: -", Color.WHITE, 14);
        roundInfoLabel = createInfoLabel("Round: -/-", Color.WHITE, 14);
        leadingSuitLabel = createInfoLabel("Leading Suit: None", Color.CYAN, 14);
        turnIndicatorLabel = createInfoLabel("", Color.ORANGE, 16);

        // Turn indicator (will be updated dynamically)
        JPanel turnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        turnPanel.setOpaque(false);
        turnPanel.add(turnIndicatorLabel);

        topInfoPanel.add(gameInfoLabel);
        topInfoPanel.add(currentPlayerLabel);
        topInfoPanel.add(turnIndicatorLabel);
        topInfoPanel.add(roundInfoLabel);
        topInfoPanel.add(leadingSuitLabel);

        statusLabel = createInfoLabel("Game starting...", Color.GREEN, 14);
        statusLabel.setHorizontalAlignment(JLabel.CENTER);

        gameInfoPanel.add(topInfoPanel, BorderLayout.NORTH);
        gameInfoPanel.add(statusLabel, BorderLayout.CENTER);

        // OK Button panel (for when it's not user's turn)
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        buttonPanel.setOpaque(false);

        okButton = createStyledButton("OK", new Color(70, 130, 180));
        okButton.setPreferredSize(new Dimension(100, 35));
        okButton.setVisible(false);
        okButton.addActionListener(e -> {
            // Just an acknowledgment button - no action needed
            if (gameSession.getCurrentPlayer() != null) {
                statusLabel.setText("Waiting for " + gameSession.getCurrentPlayer().getPlayer().getName() + "...");
            }
        });

        buttonPanel.add(okButton);
        gameInfoPanel.add(buttonPanel, BorderLayout.SOUTH);

        mainPanel.add(gameInfoPanel, BorderLayout.NORTH);
    }

    private void createPlayedCardsPanel() {
        playedCardsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
        playedCardsPanel.setOpaque(false);
        playedCardsPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.WHITE, 2), "Cards Played This Round"));
        playedCardsPanel.setPreferredSize(new Dimension(0, 220));

        JScrollPane scrollPane = new JScrollPane(playedCardsPanel);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setPreferredSize(new Dimension(0, 250));

        mainPanel.add(scrollPane, BorderLayout.CENTER);
    }

    private void createPlayerHandPanel() {
        playerHandPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 15));
        playerHandPanel.setOpaque(false);
        playerHandPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.YELLOW, 2), "Your Hand"));

        JScrollPane scrollPane = new JScrollPane(playerHandPanel);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setPreferredSize(new Dimension(0, 220));

        mainPanel.add(scrollPane, BorderLayout.SOUTH);
        loadPlayerCards();
    }

    private void createScorePanel() {
        scorePanel = new JPanel(new GridLayout(0, 1, 5, 5));
        scorePanel.setOpaque(false);
        scorePanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.WHITE, 1), "Scores"));
        scorePanel.setPreferredSize(new Dimension(200, 0));

        updateScorePanel();

        JScrollPane scrollPane = new JScrollPane(scorePanel);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setPreferredSize(new Dimension(180, 0));

        mainPanel.add(scrollPane, BorderLayout.EAST);
    }

    private JLabel createInfoLabel(String text, Color color, int fontSize) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Arial", Font.BOLD, fontSize));
        label.setForeground(color);
        label.setOpaque(false);
        label.setHorizontalAlignment(JLabel.CENTER);
        return label;
    }

    private void loadPlayerCards() {
        playerHandPanel.removeAll();

        if (currentUser != null && currentUser.getHand() != null && currentUser.getHand().hand != null) {
            for (Card card : currentUser.getHand().hand) {
                JLabel cardLabel = createCardLabel(card);
                playerHandPanel.add(cardLabel);
            }
        } else {
            JLabel noCardsLabel = new JLabel("No cards available in hand", JLabel.CENTER);
            noCardsLabel.setFont(new Font("Arial", Font.BOLD, 16));
            noCardsLabel.setForeground(Color.WHITE);
            playerHandPanel.add(noCardsLabel);
        }

        playerHandPanel.revalidate();
        playerHandPanel.repaint();
    }

    private JLabel createCardLabel(Card card) {
        String fileName = card.nameOfPNG();
        String imagePath = "CARDS_PNG/" + fileName;

        ImageIcon originalIcon = new ImageIcon(imagePath);
        JLabel cardLabel;

        if (originalIcon.getIconWidth() == -1) {
            cardLabel = createPlaceholderCard(card);
        } else {
            Image originalImage = originalIcon.getImage();
            Image scaledImage = originalImage.getScaledInstance(100, 145, Image.SCALE_SMOOTH);
            ImageIcon scaledIcon = new ImageIcon(scaledImage);
            cardLabel = new JLabel(scaledIcon);
            cardLabel.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
        }

        cardLabel.setToolTipText("<html><b>" + card.description() + "</b><br>Click to play</html>");
        cardLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));

        cardLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (cardsEnabled && gameSession.isValidMove(currentUser, card)) {
                    playCard(cardLabel, card);
                } else if (!cardsEnabled) {
                    showMessage("Please wait for your turn");
                } else {
                    showMessage("Invalid move! " + getInvalidMoveReason(card));
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                if (cardsEnabled && gameSession.isValidMove(currentUser, card)) {
                    cardLabel.setBorder(BorderFactory.createLineBorder(Color.GREEN, 3));
                    cardLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
                } else {
                    cardLabel.setBorder(BorderFactory.createLineBorder(Color.RED, 3));
                    cardLabel.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                cardLabel.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
            }
        });

        return cardLabel;
    }

    private String getInvalidMoveReason(Card card) {
        if (gameSession.getCurrentPlayer() != currentUser) {
            return "Not your turn!";
        }
        if (gameSession.getLeadingSuit() != 0 &&
                gameSession.playerHasSuit(currentUser, gameSession.getLeadingSuit()) &&
                card.getCardSuit() != gameSession.getLeadingSuit()) {
            return "You must follow the leading suit: " + gameSession.getLeadingSuitName();
        }
        return "Invalid card selection";
    }

    private JLabel createPlaceholderCard(Card card) {
        JPanel placeholder = new JPanel(new BorderLayout());
        placeholder.setOpaque(true);
        placeholder.setBackground(Color.WHITE);
        placeholder.setPreferredSize(new Dimension(100, 145));
        placeholder.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));

        JLabel valueLabel = new JLabel(getCardDisplayValue(card.getCardNumber()), JLabel.CENTER);
        valueLabel.setFont(new Font("Arial", Font.BOLD, 16));
        valueLabel.setForeground(Color.BLACK);

        JLabel suitLabel = new JLabel(String.valueOf(card.getSuitSymbol()), JLabel.CENTER);
        suitLabel.setFont(new Font("Arial", Font.BOLD, 24));

        if (card.getCardSuit() == 'H' || card.getCardSuit() == 'D') {
            suitLabel.setForeground(Color.RED);
        } else {
            suitLabel.setForeground(Color.BLACK);
        }

        placeholder.add(valueLabel, BorderLayout.NORTH);
        placeholder.add(suitLabel, BorderLayout.CENTER);

        JLabel container = new JLabel() {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(100, 145);
            }
        };
        container.setLayout(new BorderLayout());
        container.add(placeholder, BorderLayout.CENTER);

        return container;
    }

    private void playCard(JLabel cardLabel, Card card) {
        boolean played = gameSession.playCard(currentUser, card);
        if (played) {
            // Disable this card
            cardLabel.setEnabled(false);
            for (MouseListener listener : cardLabel.getMouseListeners()) {
                cardLabel.removeMouseListener(listener);
            }
            cardLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 2));
        }

        updatePlayedCards();
        loadPlayerCards();
    }

    private void updatePlayedCards() {
        playedCardsPanel.removeAll();

        Map<InGamePlayer, Card> roundCards = gameSession.getCurrentRoundCardsByPlayer();
        for (Map.Entry<InGamePlayer, Card> entry : roundCards.entrySet()) {
            JPanel playerCardPanel = new JPanel(new BorderLayout());
            playerCardPanel.setOpaque(false);

            String playerName = entry.getKey().getPlayer().getName();
            JLabel playerLabel = new JLabel(playerName, JLabel.CENTER);
            playerLabel.setFont(new Font("Arial", Font.BOLD, 12));
            playerLabel.setForeground(Color.WHITE);

            JLabel cardLabel = createPlayedCardLabel(entry.getValue());

            if (entry.getKey() == currentUser) {
                playerLabel.setForeground(Color.YELLOW);
                cardLabel.setBorder(BorderFactory.createLineBorder(Color.YELLOW, 3));
            }

            playerCardPanel.add(playerLabel, BorderLayout.NORTH);
            playerCardPanel.add(cardLabel, BorderLayout.CENTER);
            playedCardsPanel.add(playerCardPanel);
        }

        playedCardsPanel.revalidate();
        playedCardsPanel.repaint();
    }

    private JLabel createPlayedCardLabel(Card card) {
        String fileName = card.nameOfPNG();
        String imagePath = "CARDS_PNG/" + fileName;

        ImageIcon originalIcon = new ImageIcon(imagePath);
        JLabel cardLabel;

        if (originalIcon.getIconWidth() == -1) {
            cardLabel = createPlaceholderCard(card);
        } else {
            Image originalImage = originalIcon.getImage();
            Image scaledImage = originalImage.getScaledInstance(80, 116, Image.SCALE_SMOOTH);
            ImageIcon scaledIcon = new ImageIcon(scaledImage);
            cardLabel = new JLabel(scaledIcon);
            cardLabel.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
        }

        return cardLabel;
    }

    private void updateGameState() {
        if (gameSession == null) {
            statusLabel.setText("No game session available");
            statusLabel.setForeground(Color.RED);
            return;
        }

        // Update game info labels
        gameInfoLabel.setText("Game: " +
                (gameSession.getSelectedGame() != null ?
                        gameSession.getSelectedGame().getDisplayName() : "Not selected"));

        InGamePlayer currentPlayer = gameSession.getCurrentPlayer();
        currentPlayerLabel.setText("Current: " +
                (currentPlayer != null ? currentPlayer.getPlayer().getName() : "-"));

        roundInfoLabel.setText("Round: " + (gameSession.getRoundsPlayed() + 1) + "/" + gameSession.getTotalRounds());
        leadingSuitLabel.setText("Leading Suit: " + gameSession.getLeadingSuitName());

        // Check if it's this user's turn
        isUsersTurn = (currentPlayer == currentUser);

        // Fix: Simplified condition for enabling cards
        cardsEnabled = gameSession.isGameStarted() &&
                !gameSession.isGameOver() &&
                isUsersTurn &&
                gameSession.getSelectedGame() != null;

        // Update turn indicator
        if (isUsersTurn) {
            turnIndicatorLabel.setText("YOUR TURN!");
            turnIndicatorLabel.setForeground(Color.GREEN);
        } else {
            turnIndicatorLabel.setText("Waiting...");
            turnIndicatorLabel.setForeground(Color.ORANGE);
        }

        // Update status and OK button
        if (!gameSession.isGameStarted()) {
            statusLabel.setText("Game not started");
            statusLabel.setForeground(Color.RED);
            okButton.setVisible(false);
        } else if (gameSession.getSelectedGame() == null) {
            statusLabel.setText("Waiting for game selection");
            statusLabel.setForeground(Color.YELLOW);
            okButton.setVisible(false);
        } else if (gameSession.isGameOver()) {
            String winnerName = gameSession.getWinnerDisplayName();
            statusLabel.setText("Game Over! Winner: " + winnerName);
            statusLabel.setForeground(Color.GREEN);
            okButton.setVisible(false);
            cardsEnabled = false;
        } else if (isUsersTurn) {
            // FIX: Changed from cardsEnabled to isUsersTurn
            statusLabel.setText("Your turn! Play a card");
            statusLabel.setForeground(Color.GREEN);
            okButton.setVisible(false);
        } else {
            // FIX: Show correct waiting message
            statusLabel.setText("Waiting for " +
                    (currentPlayer != null ? currentPlayer.getPlayer().getName() : "other players") + "...");
            statusLabel.setForeground(Color.ORANGE);
            okButton.setVisible(true);
        }

        updateScorePanel();
        loadPlayerCards();
        updatePlayedCards();
    }

    private void updateScorePanel() {
        scorePanel.removeAll();

        if (gameSession == null) {
            JLabel noScoresLabel = new JLabel("No game session");
            noScoresLabel.setForeground(Color.WHITE);
            scorePanel.add(noScoresLabel);
            return;
        }

        for (InGamePlayer player : gameSession.getPlayers()) {
            JPanel playerScorePanel = new JPanel(new BorderLayout());
            playerScorePanel.setOpaque(false);
            playerScorePanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

            String playerName = player.getPlayer().getName();
            JLabel nameLabel = new JLabel(playerName);
            nameLabel.setFont(new Font("Arial", Font.BOLD, 12));

            if (player == currentUser) {
                nameLabel.setForeground(Color.YELLOW);
                nameLabel.setText("▶ " + playerName + " (You)");
            } else {
                nameLabel.setForeground(Color.WHITE);
            }

            int score = gameSession.getScores().getOrDefault(player, 0);
            JLabel scoreLabel = new JLabel(String.valueOf(score));
            scoreLabel.setFont(new Font("Arial", Font.BOLD, 14));
            scoreLabel.setForeground(Color.CYAN);

            // Highlight current player
            if (player == gameSession.getCurrentPlayer()) {
                playerScorePanel.setBackground(new Color(255, 255, 255, 50));
                playerScorePanel.setOpaque(true);
            }

            playerScorePanel.add(nameLabel, BorderLayout.WEST);
            playerScorePanel.add(scoreLabel, BorderLayout.EAST);
            scorePanel.add(playerScorePanel);
        }

        scorePanel.revalidate();
        scorePanel.repaint();
    }

    private void setupGameTimer() {
        gameTimer = new Timer(1000, e -> updateGameState());
        gameTimer.start();
    }

    private JButton createStyledButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(30, 80, 120), 2),
                BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setOpaque(true);

        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) {
                button.setBackground(brighterColor(color));
            }
            public void mouseExited(MouseEvent evt) {
                button.setBackground(color);
            }
        });

        return button;
    }

    private Color brighterColor(Color color) {
        return new Color(
                Math.min(255, color.getRed() + 30),
                Math.min(255, color.getGreen() + 30),
                Math.min(255, color.getBlue() + 30)
        );
    }

    private void showMessage(String message) {
        statusLabel.setText(message);
        Timer messageTimer = new Timer(3000, e -> {
            updateGameState();
        });
        messageTimer.setRepeats(false);
        messageTimer.start();
    }

    private String getCardDisplayValue(int cardNumber) {
        switch (cardNumber) {
            case 11: return "J";
            case 12: return "Q";
            case 13: return "K";
            case 14: return "A";
            default: return String.valueOf(cardNumber);
        }
    }

    // GameSessionListener implementations
    @Override
    public void onGameStarted() {
        SwingUtilities.invokeLater(() -> {
            showMessage("Game started!");
            updateGameState();
        });
    }

    @Override
    public void onCardPlayed(Card card) {
        SwingUtilities.invokeLater(() -> {
            showMessage(card.description() + " was played");
            updateGameState();
        });
    }

    @Override
    public void onRoundCompleted(InGamePlayer winner) {
        SwingUtilities.invokeLater(() -> {
            String message = "Round won by: " + winner.getPlayer().getName();
            showMessage(message);
            JOptionPane.showMessageDialog(frame, message, "Round Complete", JOptionPane.INFORMATION_MESSAGE);
            updateGameState();
        });
    }

    @Override
    public void onGameOver(Map<InGamePlayer, Integer> scores) {
        SwingUtilities.invokeLater(() -> {
            // Use the GameSession's method to get formatted final scores
            String result = gameSession.getFinalScoresForDisplay();

            JOptionPane.showMessageDialog(frame, result, "Game Over", JOptionPane.INFORMATION_MESSAGE);
            updateGameState();

            // Stop the timer when game is over
            if (gameTimer != null) {
                gameTimer.stop();
            }
        });
    }

    @Override
    public void onPlayerChanged(InGamePlayer currentPlayer) {
        SwingUtilities.invokeLater(() -> {
            updateGameState();
        });
    }

    @Override
    public void onGameSelected(GameType gameType) {
        SwingUtilities.invokeLater(() -> {
            gameInfoLabel.setText("Game: " + gameType.getDisplayName());
            statusLabel.setText("Game selected: " + gameType.getDisplayName() + " - Ready to play!");
            statusLabel.setForeground(Color.GREEN);
            updateGameState();
        });
    }

    @Override
    public void onPlayerReady(String playerName) {
        // Not used in local multiplayer
    }

    public void display() {
        // Already displayed in constructor
    }

    public void dispose() {
        if (gameTimer != null) {
            gameTimer.stop();
        }
        if (gameSession != null) {
            gameSession.removeListener(this);
        }
        if (frame != null) {
            frame.dispose();
        }
    }

    public static void main(String[] args) {
        // Test method - can be used for testing the GameScreen
        SwingUtilities.invokeLater(() -> {
            java.util.ArrayList<user.Player> players = new java.util.ArrayList<>();
            players.add(new user.Player("Player1", "p1@test.com", "pass"));
            players.add(new user.Player("Player2", "p2@test.com", "pass"));

            GameSession gameSession = new GameSession(players);
            gameSession.startGame();

            new GameScreen(gameSession, gameSession.getPlayers().get(0));
        });
    }
}