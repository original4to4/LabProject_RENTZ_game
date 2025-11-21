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
import network.GameClient;
import network.GameMessage;

public class GameScreen implements GameSessionListener, GameClient.MessageListener {
    private JFrame frame;
    private JPanel mainPanel;
    private JPanel playerHandPanel;
    private JPanel playedCardsPanel;
    private JPanel gameInfoPanel;
    private JPanel scorePanel;
    private JButton chooseGameButton;
    private JButton readyButton;
    private JButton startGameButton;
    private JLabel gameInfoLabel;
    private JLabel currentPlayerLabel;
    private JLabel roundInfoLabel;
    private JLabel leadingSuitLabel;
    private JLabel statusLabel;

    private GameSession gameSession;
    private InGamePlayer currentUser;
    private GameClient gameClient;
    private Timer gameTimer;
    private boolean cardsEnabled = false;
    private boolean isNetworkGame = false;
    private int currentSessionId = -1;

    public GameScreen(GameSession gameSession, InGamePlayer currentUser) {
        this(gameSession, currentUser, null);
    }

    public GameScreen(GameSession gameSession, InGamePlayer currentUser, GameClient gameClient) {
        this.gameSession = gameSession;
        this.currentUser = currentUser;
        this.gameClient = gameClient;
        this.isNetworkGame = (gameClient != null);
        this.currentSessionId = gameSession.getSessionId();

        if (this.gameSession != null) {
            this.gameSession.addListener(this);
        }
        if (this.gameClient != null) {
            this.gameClient.setMessageListener(this);
            this.gameSession.setNetworkGame(true);
        }

        initialize();
        updateGameState();

        if (isNetworkGame) {
            sendGameJoinMessage();
        }
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
    }

    private void createFrame() {
        String title = "Card Game - " + (currentUser != null ? currentUser.getPlayer().getName() : "Player");
        if (isNetworkGame) {
            title += " (Network)";
        }
        frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 800);
        frame.setLocationRelativeTo(null);
        frame.setMinimumSize(new Dimension(1000, 700));
    }

    private void createMainPanel() {
        mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Set background
        mainPanel.setBackground(new Color(0, 100, 0)); // Green felt background
    }

    private void createGameInfoPanel() {
        gameInfoPanel = new JPanel(new BorderLayout());
        gameInfoPanel.setBackground(new Color(70, 130, 180));
        gameInfoPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel topInfoPanel = new JPanel(new GridLayout(2, 2, 10, 5));
        topInfoPanel.setOpaque(false);

        gameInfoLabel = createInfoLabel("Waiting for game selection", Color.YELLOW, 16);
        currentPlayerLabel = createInfoLabel("Current: -", Color.WHITE, 14);
        roundInfoLabel = createInfoLabel("Round: -/-", Color.WHITE, 14);
        leadingSuitLabel = createInfoLabel("Leading Suit: None", Color.CYAN, 14);

        topInfoPanel.add(gameInfoLabel);
        topInfoPanel.add(currentPlayerLabel);
        topInfoPanel.add(roundInfoLabel);
        topInfoPanel.add(leadingSuitLabel);

        statusLabel = createInfoLabel("Welcome to the game!", Color.GREEN, 12);
        statusLabel.setHorizontalAlignment(JLabel.CENTER);

        gameInfoPanel.add(topInfoPanel, BorderLayout.NORTH);
        gameInfoPanel.add(statusLabel, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        buttonPanel.setOpaque(false);

        chooseGameButton = createStyledButton("Choose Game", new Color(70, 130, 180));
        chooseGameButton.addActionListener(e -> openGameMenu());

        readyButton = createStyledButton("I'm Ready", new Color(218, 165, 32));
        readyButton.addActionListener(e -> setPlayerReady());
        readyButton.setVisible(isNetworkGame);

        startGameButton = createStyledButton("Start Game", new Color(0, 150, 0));
        startGameButton.addActionListener(e -> startGame());
        startGameButton.setVisible(!isNetworkGame ||
                (currentUser != null && gameSession != null && gameSession.getPlayers() != null &&
                        !gameSession.getPlayers().isEmpty() &&
                        currentUser.getPlayer().getName().equals(gameSession.getPlayers().get(0).getPlayer().getName())));

        buttonPanel.add(chooseGameButton);
        buttonPanel.add(readyButton);
        buttonPanel.add(startGameButton);

        gameInfoPanel.add(buttonPanel, BorderLayout.SOUTH);
        mainPanel.add(gameInfoPanel, BorderLayout.NORTH);
    }

    private void createPlayedCardsPanel() {
        playedCardsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        playedCardsPanel.setBackground(new Color(0, 120, 0));
        playedCardsPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.WHITE, 2), "Current Round - Played Cards"));
        playedCardsPanel.setPreferredSize(new Dimension(0, 180));

        JScrollPane scrollPane = new JScrollPane(playedCardsPanel);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setPreferredSize(new Dimension(0, 200));

        mainPanel.add(scrollPane, BorderLayout.CENTER);
    }

    private void createPlayerHandPanel() {
        playerHandPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 15));
        playerHandPanel.setBackground(new Color(0, 120, 0));
        playerHandPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.YELLOW, 2), "Your Hand - Click to Play"));

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
        scorePanel.setBackground(new Color(0, 120, 0));
        scorePanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.WHITE, 1), "Scores"));
        scorePanel.setPreferredSize(new Dimension(180, 0));

        updateScorePanel();

        JScrollPane scrollPane = new JScrollPane(scorePanel);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setPreferredSize(new Dimension(160, 0));

        mainPanel.add(scrollPane, BorderLayout.EAST);
    }

    private JLabel createInfoLabel(String text, Color color, int fontSize) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Arial", Font.BOLD, fontSize));
        label.setForeground(color);
        label.setOpaque(false);
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
                if (cardsEnabled && isValidMove(card)) {
                    playCard(cardLabel, card);
                } else if (!cardsEnabled) {
                    showMessage("Please wait for your turn or select a game first");
                } else {
                    showMessage("Invalid move! " + getInvalidMoveReason(card));
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                if (cardsEnabled && isValidMove(card)) {
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

    private boolean isValidMove(Card card) {
        if (gameSession == null || currentUser == null) return false;

        // Check if it's player's turn
        InGamePlayer currentPlayer = gameSession.getCurrentPlayer();
        if (currentPlayer == null || !currentPlayer.equals(currentUser)) {
            return false;
        }

        // Check leading suit rules
        char leadingSuit = gameSession.getLeadingSuit();
        if (leadingSuit != 0) {
            // Player must follow leading suit if they have it
            boolean hasLeadingSuit = false;
            for (Card handCard : currentUser.getHand().hand) {
                if (handCard.getCardSuit() == leadingSuit) {
                    hasLeadingSuit = true;
                    break;
                }
            }
            if (hasLeadingSuit && card.getCardSuit() != leadingSuit) {
                return false;
            }
        }

        return true;
    }

    private String getInvalidMoveReason(Card card) {
        if (gameSession.getCurrentPlayer() == null || !gameSession.getCurrentPlayer().equals(currentUser)) {
            String currentPlayerName = gameSession.getCurrentPlayer() != null ?
                    gameSession.getCurrentPlayer().getPlayer().getName() : "Unknown";
            return "Not your turn! Current player: " + currentPlayerName;
        }

        char leadingSuit = gameSession.getLeadingSuit();
        if (leadingSuit != 0) {
            boolean hasLeadingSuit = false;
            for (Card handCard : currentUser.getHand().hand) {
                if (handCard.getCardSuit() == leadingSuit) {
                    hasLeadingSuit = true;
                    break;
                }
            }
            if (hasLeadingSuit && card.getCardSuit() != leadingSuit) {
                return "You must follow the leading suit: " + getSuitName(leadingSuit);
            }
        }
        return "Invalid card selection";
    }

    private String getSuitName(char suit) {
        switch(suit) {
            case 'H': return "Hearts";
            case 'S': return "Spades";
            case 'D': return "Diamonds";
            case 'C': return "Clubs";
            default: return "Unknown";
        }
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
        if (isNetworkGame) {
            // Send card play to server
            if (gameClient != null) {
                gameClient.playCard(currentSessionId, card);
            }
            cardLabel.setEnabled(false);
            for (MouseListener listener : cardLabel.getMouseListeners()) {
                cardLabel.removeMouseListener(listener);
            }
        } else {
            // Local play - simplified version
            try {
                // Remove card from hand
                currentUser.getHand().removeCard(card);

                // Add to current round (simplified)
                // In a real implementation, you'd call gameSession.playCard()

                showMessage("Played: " + card.description());
                updateGameState();
            } catch (Exception e) {
                showMessage("Error playing card: " + e.getMessage());
            }
        }

        updatePlayedCards();
        loadPlayerCards();
    }

    private void updatePlayedCards() {
        playedCardsPanel.removeAll();

        // Simplified - in real implementation, get from gameSession
        if (gameSession != null && gameSession.getCurrentRound() != null) {
            for (Card card : gameSession.getCurrentRound()) {
                JLabel cardLabel = createPlayedCardLabel(card);
                playedCardsPanel.add(cardLabel);
            }
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

    private void openGameMenu() {
        if (gameSession == null) {
            showMessage("No game session available");
            return;
        }

        if (!gameSession.isGameStarted()) {
            showMessage("Please start the game first");
            return;
        }

        JDialog dialog = new JDialog(frame, "Choose Game Type", true);
        dialog.setSize(400, 500);
        dialog.setLocationRelativeTo(frame);
        dialog.setLayout(new BorderLayout());

        JPanel dialogPanel = new JPanel(new BorderLayout());
        dialogPanel.setBackground(new Color(40, 40, 60));
        dialogPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("Select Game Type", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        dialogPanel.add(titleLabel, BorderLayout.NORTH);

        JPanel gamesPanel = new JPanel(new GridLayout(0, 1, 10, 10));
        gamesPanel.setBackground(new Color(40, 40, 60));

        for (GameType game : GameType.values()) {
            JButton gameBtn = createStyledButton(game.getDisplayName(), new Color(100, 150, 200));
            gameBtn.setToolTipText(game.getCode());
            gameBtn.addActionListener(e -> {
                try {
                    if (isNetworkGame && gameClient != null) {
                        gameClient.selectGameType(currentSessionId, game);
                    } else {
                        gameSession.setSelectedGame(game);
                        onGameSelected(game);
                    }
                    chooseGameButton.setText("Game: " + game.getDisplayName());
                    chooseGameButton.setBackground(new Color(100, 200, 100));
                    dialog.dispose();
                    showMessage("Game selected: " + game.getDisplayName());
                } catch (Exception ex) {
                    showMessage("Error: " + ex.getMessage());
                }
            });
            gamesPanel.add(gameBtn);
        }

        JScrollPane scrollPane = new JScrollPane(gamesPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(new Color(40, 40, 60));
        dialogPanel.add(scrollPane, BorderLayout.CENTER);

        JButton closeButton = createStyledButton("Close", new Color(150, 150, 150));
        closeButton.addActionListener(e -> dialog.dispose());
        JPanel closePanel = new JPanel();
        closePanel.setBackground(new Color(40, 40, 60));
        closePanel.add(closeButton);
        dialogPanel.add(closePanel, BorderLayout.SOUTH);

        dialog.add(dialogPanel);
        dialog.setVisible(true);
    }

    private void setPlayerReady() {
        if (isNetworkGame && gameClient != null) {
            gameClient.sendMessage(new GameMessage(
                    GameMessage.PLAYER_READY,
                    currentUser.getPlayer().getName(),
                    currentSessionId
            ));
            readyButton.setEnabled(false);
            readyButton.setText("Ready!");
            readyButton.setBackground(new Color(100, 200, 100));
            showMessage("You are ready!");
        }
    }

    private void startGame() {
        if (isNetworkGame && gameClient != null) {
            gameClient.startGame(currentSessionId);
        } else {
            if (gameSession != null && !gameSession.isGameStarted()) {
                boolean started = gameSession.startGame();
                if (started) {
                    showMessage("Game started successfully!");
                } else {
                    showMessage("Failed to start game");
                }
            }
        }
    }

    private void sendGameJoinMessage() {
        if (isNetworkGame && gameClient != null) {
            gameClient.sendMessage(new GameMessage(
                    GameMessage.PLAYER_JOIN,
                    currentUser.getPlayer().getName(),
                    currentSessionId
            ));
        }
    }

    private void updateGameState() {
        if (gameSession == null) {
            statusLabel.setText("No game session available");
            statusLabel.setForeground(Color.RED);
            return;
        }

        String gameTypeText = gameSession.getSelectedGame() != null ?
                gameSession.getSelectedGame().getDisplayName() : "Not selected";
        gameInfoLabel.setText("Game: " + gameTypeText);

        String currentPlayerName = gameSession.getCurrentPlayer() != null ?
                gameSession.getCurrentPlayer().getPlayer().getName() : "Unknown";
        currentPlayerLabel.setText("Current: " + currentPlayerName);

        roundInfoLabel.setText("Round: " + (gameSession.getRoundsPlayed() + 1) + "/" + gameSession.getTotalRounds());

        String leadingSuitText = "Leading Suit: " + getSuitName(gameSession.getLeadingSuit());
        leadingSuitLabel.setText(leadingSuitText);

        cardsEnabled = gameSession.isRoundInProgress() &&
                gameSession.getCurrentPlayer() != null &&
                gameSession.getCurrentPlayer().equals(currentUser) &&
                gameSession.getSelectedGame() != null;

        if (!gameSession.isGameStarted()) {
            statusLabel.setText("Game not started");
            statusLabel.setForeground(Color.RED);
            startGameButton.setVisible(true);
        } else if (gameSession.getSelectedGame() == null) {
            statusLabel.setText("Waiting for game selection");
            statusLabel.setForeground(Color.YELLOW);
            startGameButton.setVisible(false);
        } else if (gameSession.isGameOver()) {
            statusLabel.setText("Game Over! Winner: " +
                    (gameSession.getGameWinner() != null ? gameSession.getGameWinner().getPlayer().getName() : ""));
            statusLabel.setForeground(Color.GREEN);
            startGameButton.setVisible(false);
        } else if (cardsEnabled) {
            statusLabel.setText("Your turn! Play a card");
            statusLabel.setForeground(Color.GREEN);
            startGameButton.setVisible(false);
        } else {
            statusLabel.setText("Waiting for other players...");
            statusLabel.setForeground(Color.ORANGE);
            startGameButton.setVisible(false);
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

            if (player.equals(currentUser)) {
                nameLabel.setForeground(Color.YELLOW);
                nameLabel.setText("▶ " + playerName);
            } else {
                nameLabel.setForeground(Color.WHITE);
            }

            int score = gameSession.getScores().getOrDefault(player, 0);
            JLabel scoreLabel = new JLabel(String.valueOf(score));
            scoreLabel.setFont(new Font("Arial", Font.BOLD, 14));
            scoreLabel.setForeground(Color.CYAN);

            if (player.equals(gameSession.getCurrentPlayer())) {
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
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
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
            showMessage("Game started! Please select a game type.");
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
            String message = "Round won by: " + (winner != null ? winner.getPlayer().getName() : "Unknown");
            showMessage(message);
            if (!isNetworkGame) {
                JOptionPane.showMessageDialog(frame, message, "Round Complete", JOptionPane.INFORMATION_MESSAGE);
            }
            updateGameState();
        });
    }

    @Override
    public void onGameOver(Map<InGamePlayer, Integer> scores) {
        SwingUtilities.invokeLater(() -> {
            StringBuilder result = new StringBuilder("<html><b>Game Over! Final Scores:</b><br>");
            for (Map.Entry<InGamePlayer, Integer> entry : scores.entrySet()) {
                result.append(entry.getKey().getPlayer().getName()).append(": ").append(entry.getValue()).append("<br>");
            }

            InGamePlayer winner = gameSession.getGameWinner();
            if (winner != null) {
                result.append("<br><b>Winner: ").append(winner.getPlayer().getName()).append("</b>");
            }

            result.append("</html>");

            JOptionPane.showMessageDialog(frame, result.toString(), "Game Over", JOptionPane.INFORMATION_MESSAGE);
            updateGameState();
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
            chooseGameButton.setText("Game: " + gameType.getDisplayName());
            chooseGameButton.setBackground(new Color(100, 200, 100));
            statusLabel.setText("Game selected: " + gameType.getDisplayName() + " - Ready to play!");
            statusLabel.setForeground(Color.GREEN);

            cardsEnabled = gameSession != null && gameSession.getCurrentPlayer() != null &&
                    gameSession.getCurrentPlayer().equals(currentUser);
            updateGameState();
        });
    }

    @Override
    public void onPlayerReady(String playerName) {
        SwingUtilities.invokeLater(() -> {
            showMessage("Player " + playerName + " is ready!");
        });
    }

    // GameClient.MessageListener implementations
    @Override
    public void onMessageReceived(GameMessage message) {
        SwingUtilities.invokeLater(() -> {
            handleNetworkMessage(message);
        });
    }

    @Override
    public void onConnectionStatusChanged(boolean connected) {
        SwingUtilities.invokeLater(() -> {
            if (!connected) {
                statusLabel.setText("Disconnected from server - playing locally");
                statusLabel.setForeground(Color.RED);
            } else {
                statusLabel.setText("Connected to server");
                statusLabel.setForeground(Color.GREEN);
            }
        });
    }

    private void handleNetworkMessage(GameMessage message) {
        System.out.println("📨 Network message: " + message.getType());

        try {
            switch (message.getType()) {
                case GameMessage.GAME_STARTED:
                    if (gameSession != null && !gameSession.isGameStarted()) {
                        gameSession.startGame();
                    }
                    break;

                case GameMessage.GAME_TYPE_SELECTED:
                    if (message.getData() instanceof GameMessage.GameTypeData) {
                        GameMessage.GameTypeData data = (GameMessage.GameTypeData) message.getData();
                        gameSession.setSelectedGame(data.gameType);
                        onGameSelected(data.gameType);
                    }
                    break;

                case GameMessage.CARD_PLAYED:
                    if (message.getData() instanceof GameMessage.CardPlayData) {
                        GameMessage.CardPlayData data = (GameMessage.CardPlayData) message.getData();
                        if (!data.playerName.equals(currentUser.getPlayer().getName())) {
                            // Update UI for other player's card play
                            showMessage(data.playerName + " played a card");
                            updateGameState();
                        }
                    }
                    break;

                case GameMessage.ROUND_COMPLETED:
                    if (message.getData() instanceof GameMessage.RoundCompleteData) {
                        GameMessage.RoundCompleteData data = (GameMessage.RoundCompleteData) message.getData();
                        showMessage("Round won by: " + data.winnerName);
                    }
                    break;

                case GameMessage.GAME_OVER:
                    if (message.getData() instanceof GameMessage.GameOverData) {
                        GameMessage.GameOverData data = (GameMessage.GameOverData) message.getData();
                        StringBuilder result = new StringBuilder("<html><b>Game Over! Final Scores:</b><br>");
                        for (Map.Entry<String, Integer> entry : data.finalScores.entrySet()) {
                            result.append(entry.getKey()).append(": ").append(entry.getValue()).append("<br>");
                        }
                        result.append("<br><b>Winner: ").append(data.winnerName).append("</b></html>");
                        JOptionPane.showMessageDialog(frame, result.toString(), "Game Over", JOptionPane.INFORMATION_MESSAGE);
                    }
                    break;

                case GameMessage.PLAYER_READY:
                    String readyPlayer = (String) message.getData();
                    onPlayerReady(readyPlayer);
                    break;

                case GameMessage.PLAYER_JOINED:
                    if (message.getData() instanceof GameMessage.PlayerJoinData) {
                        GameMessage.PlayerJoinData data = (GameMessage.PlayerJoinData) message.getData();
                        showMessage("Player " + data.playerName + " joined the game");
                    }
                    break;

                case GameMessage.ERROR:
                    String error = message.getData() != null ? message.getData().toString() : "Unknown error";
                    showMessage("Server error: " + error);
                    break;
            }

            updateGameState();
        } catch (Exception e) {
            System.err.println("Error handling network message: " + e.getMessage());
        }
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
}