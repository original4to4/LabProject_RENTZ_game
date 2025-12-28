package GUI;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;
import gameEngine.GameType;

/**
 * AdminScreen - Redesigned for local multiplayer with sequential login
 * Removed all network functionality
 */
public class AdminScreen {
    private JFrame frame;
    private JPanel mainPanel;
    private JComboBox<GameType> gameTypeComboBox;
    private JSpinner playerCountSpinner;
    private JButton createGameButton;
    private JButton backButton;
    private JLabel statusLabel;

    public AdminScreen() {
        initialize();
    }

    public void display() {
        frame.setVisible(true);
    }

    private void initialize() {
        createFrame();
        createMainPanel();
        createGameSetupSection();
        createControlPanel();

        frame.add(mainPanel);
        frame.setLocationRelativeTo(null);
    }

    private void createFrame() {
        frame = new JFrame("Admin Panel - Create Local Game");
        frame.setSize(600, 500);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setMinimumSize(new Dimension(500, 400));
    }

    private void createMainPanel() {
        mainPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // Try to load background image
                try {
                    ImageIcon backgroundIcon = new ImageIcon("Pictures/background.jpg");
                    if (backgroundIcon.getIconWidth() != -1) {
                        Image backgroundImage = backgroundIcon.getImage();
                        g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
                    } else {
                        // Fallback gradient background
                        Graphics2D g2d = (Graphics2D) g;
                        Color color1 = new Color(30, 60, 90);
                        Color color2 = new Color(10, 30, 50);
                        GradientPaint gradient = new GradientPaint(0, 0, color1, getWidth(), getHeight(), color2);
                        g2d.setPaint(gradient);
                        g2d.fillRect(0, 0, getWidth(), getHeight());
                    }
                } catch (Exception e) {
                    // Solid color fallback
                    g.setColor(new Color(30, 60, 90));
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
            }
        };
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
    }

    private void createGameSetupSection() {
        JPanel setupPanel = new JPanel(new GridBagLayout());
        setupPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Title
        JLabel titleLabel = new JLabel("Create Local Multiplayer Game", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(new Color(255, 215, 0));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(0, 0, 30, 0);
        setupPanel.add(titleLabel, gbc);

        // Instructions
        JTextArea instructionText = new JTextArea(
                "Game Setup Instructions:\n\n" +
                        "1. Select the game type from the dropdown\n" +
                        "2. Choose the number of players (2-6)\n" +
                        "3. Click 'Create Game' to start\n" +
                        "4. Players will login one by one\n" +
                        "5. Each player will get their own game window"
        );
        instructionText.setFont(new Font("Arial", Font.PLAIN, 14));
        instructionText.setForeground(Color.WHITE);
        instructionText.setOpaque(false);
        instructionText.setEditable(false);
        instructionText.setLineWrap(true);
        instructionText.setWrapStyleWord(true);
        instructionText.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(100, 150, 200), 2),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(0, 0, 30, 0);
        setupPanel.add(instructionText, gbc);

        // Game Type Selection
        JLabel gameTypeLabel = new JLabel("Game Type:");
        gameTypeLabel.setFont(new Font("Arial", Font.BOLD, 16));
        gameTypeLabel.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.LINE_END;
        setupPanel.add(gameTypeLabel, gbc);

        gameTypeComboBox = new JComboBox<>(GameType.values());
        gameTypeComboBox.setFont(new Font("Arial", Font.PLAIN, 14));
        gameTypeComboBox.setPreferredSize(new Dimension(200, 30));
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.LINE_START;
        setupPanel.add(gameTypeComboBox, gbc);

        // Player Count Selection
        JLabel playerCountLabel = new JLabel("Number of Players:");
        playerCountLabel.setFont(new Font("Arial", Font.BOLD, 16));
        playerCountLabel.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.LINE_END;
        setupPanel.add(playerCountLabel, gbc);

        playerCountSpinner = new JSpinner(new SpinnerNumberModel(4, 2, 6, 1));
        playerCountSpinner.setFont(new Font("Arial", Font.BOLD, 14));
        playerCountSpinner.setPreferredSize(new Dimension(80, 30));
        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.LINE_START;
        setupPanel.add(playerCountSpinner, gbc);

        // Empty space
        JPanel emptyPanel = new JPanel();
        emptyPanel.setOpaque(false);
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.VERTICAL;
        setupPanel.add(emptyPanel, gbc);

        mainPanel.add(setupPanel, BorderLayout.CENTER);
    }

    private void createControlPanel() {
        JPanel controlPanel = new JPanel(new BorderLayout());
        controlPanel.setOpaque(false);
        controlPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));

        // Status panel
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        statusPanel.setOpaque(false);
        statusLabel = new JLabel("Ready to create local multiplayer game", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        statusLabel.setForeground(Color.GREEN);
        statusPanel.add(statusLabel);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 10));
        buttonPanel.setOpaque(false);

        createGameButton = createStyledButton("Create Game", new Color(0, 150, 0));
        createGameButton.setPreferredSize(new Dimension(150, 45));
        createGameButton.setFont(new Font("Arial", Font.BOLD, 16));
        createGameButton.addActionListener(e -> createGame());

        backButton = createStyledButton("Back to Main", new Color(150, 150, 150));
        backButton.setPreferredSize(new Dimension(150, 45));
        backButton.setFont(new Font("Arial", Font.BOLD, 16));
        backButton.addActionListener(e -> goBackToMain());

        buttonPanel.add(createGameButton);
        buttonPanel.add(backButton);

        controlPanel.add(statusPanel, BorderLayout.NORTH);
        controlPanel.add(buttonPanel, BorderLayout.CENTER);
        mainPanel.add(controlPanel, BorderLayout.SOUTH);
    }

    private JButton createStyledButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(darkerColor(color), 2),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)
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

    private Color darkerColor(Color color) {
        return new Color(
                Math.max(0, color.getRed() - 30),
                Math.max(0, color.getGreen() - 30),
                Math.max(0, color.getBlue() - 30)
        );
    }

    private void createGame() {
        GameType selectedGameType = (GameType) gameTypeComboBox.getSelectedItem();
        int playerCount = (Integer) playerCountSpinner.getValue();

        if (selectedGameType == null) {
            JOptionPane.showMessageDialog(frame,
                    "Please select a game type",
                    "Selection Required",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Confirm game creation
        int confirm = JOptionPane.showConfirmDialog(frame,
                String.format("Create a %s game with %d players?\n\n" +
                                "Players will login one by one.",
                        selectedGameType.getDisplayName(), playerCount),
                "Confirm Game Creation",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            // Start sequential login flow
            frame.dispose();  // Close admin screen

            PlayerLoginFlow.startNewGame(frame, selectedGameType, playerCount);
        }
    }

    private void goBackToMain() {
        frame.dispose();
        new FirstScreen().display();
    }

    public static void main(String[] args) {
        // For testing the admin screen
        SwingUtilities.invokeLater(() -> {
            new AdminScreen().display();
        });
    }
}