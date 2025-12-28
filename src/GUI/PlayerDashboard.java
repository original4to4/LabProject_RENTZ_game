package GUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.ArrayList;
import data_base_connection.GameRepository;
import data_base_connection.GameSessionInfo;
import user.User;

/**
 * PlayerDashboard - Simplified for viewing past games only
 * Since players now join via sequential login, this is optional
 */
public class PlayerDashboard {
    private JFrame frame;
    private JPanel mainPanel;
    private JList<GameSessionInfo> sessionsList;
    private DefaultListModel<GameSessionInfo> listModel;
    private JButton viewButton;
    private JButton refreshButton;
    private JButton backButton;
    private JLabel statusLabel;
    private JLabel welcomeLabel;

    private GameRepository gameRepository;
    private User currentUser;

    public PlayerDashboard(User user) {
        this.currentUser = user;
        this.gameRepository = new GameRepository();
        initialize();
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

    private void createFrame() {
        frame = new JFrame("Player Dashboard - " + currentUser.getName());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(700, 500);
        frame.setMinimumSize(new Dimension(600, 400));
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

        JLabel subtitleLabel = new JLabel("Your Past Game Sessions", JLabel.LEFT);
        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        subtitleLabel.setForeground(Color.WHITE);

        JPanel textPanel = new JPanel(new GridLayout(2, 1));
        textPanel.setOpaque(false);
        textPanel.add(welcomeLabel);
        textPanel.add(subtitleLabel);

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.add(textPanel, BorderLayout.WEST);

        welcomePanel.add(headerPanel, BorderLayout.CENTER);
        mainPanel.add(welcomePanel, BorderLayout.NORTH);
    }

    private void createSessionsList() {
        listModel = new DefaultListModel<>();
        sessionsList = new JList<>(listModel);
        sessionsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        sessionsList.setFont(new Font("Arial", Font.PLAIN, 14));
        sessionsList.setCellRenderer(new SessionListRenderer());

        JScrollPane scrollPane = new JScrollPane(sessionsList);
        scrollPane.setBorder(BorderFactory.createTitledBorder(
                "Past Game Sessions - View details only"));
        scrollPane.setPreferredSize(new Dimension(0, 300));
        mainPanel.add(scrollPane, BorderLayout.CENTER);
    }

    private void createControlPanel() {
        JPanel controlPanel = new JPanel(new BorderLayout());
        controlPanel.setBackground(Color.WHITE);
        controlPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        // Status label
        statusLabel = new JLabel("Select a game session to view details", JLabel.CENTER);
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        controlPanel.add(statusLabel, BorderLayout.NORTH);

        // Buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        buttonPanel.setBackground(Color.WHITE);

        refreshButton = createStyledButton("Refresh", new Color(70, 130, 180));
        refreshButton.addActionListener(e -> refreshSessionsList());

        viewButton = createStyledButton("View Details", new Color(0, 150, 0));
        viewButton.addActionListener(e -> viewSelectedSession());

        backButton = createStyledButton("Back to Main", new Color(150, 150, 150));
        backButton.addActionListener(e -> logout());

        buttonPanel.add(refreshButton);
        buttonPanel.add(viewButton);
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
        listModel.clear();
        List<GameSessionInfo> sessions = gameRepository.getActiveGameSessions();

        if (sessions.isEmpty()) {
            listModel.addElement(new GameSessionInfo(0, "No past game sessions found", 0, 0, "none", null));
            viewButton.setEnabled(false);
        } else {
            // Filter to show only sessions this player was in
            for (GameSessionInfo session : sessions) {
                List<String> players = gameRepository.getSessionPlayers(session.getSessionId());
                if (players.contains(currentUser.getName())) {
                    listModel.addElement(session);
                }
            }

            if (listModel.isEmpty()) {
                listModel.addElement(new GameSessionInfo(0, "No sessions found for " + currentUser.getName(), 0, 0, "none", null));
                viewButton.setEnabled(false);
            } else {
                viewButton.setEnabled(true);
            }
        }

        statusLabel.setText("Found " + listModel.size() + " game sessions for " + currentUser.getName());
    }

    private void viewSelectedSession() {
        GameSessionInfo selected = sessionsList.getSelectedValue();
        if (selected == null || selected.getSessionId() == 0) {
            JOptionPane.showMessageDialog(frame, "Please select a valid game session");
            return;
        }

        JDialog detailsDialog = new JDialog(frame, "Session Details", true);
        detailsDialog.setSize(400, 300);
        detailsDialog.setLocationRelativeTo(frame);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        String details = String.format("""
            <html>
            <h2>Session %d</h2>
            <b>Host:</b> %s<br>
            <b>Players:</b> %d<br>
            <b>Status:</b> %s<br>
            <b>Game Type:</b> %s<br>
            <b>Created:</b> %s<br>
            </html>
            """,
                selected.getSessionId(),
                selected.getHostName(),
                selected.getPlayerCount(),
                selected.getGameState(),
                selected.getGameType() != null ? selected.getGameType() : "Not specified",
                selected.getCreatedAt()
        );

        JLabel detailsLabel = new JLabel(details);
        panel.add(detailsLabel, BorderLayout.CENTER);

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> detailsDialog.dispose());
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(closeButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        detailsDialog.add(panel);
        detailsDialog.setVisible(true);
    }

    private void logout() {
        int confirm = JOptionPane.showConfirmDialog(
                frame,
                "Are you sure you want to logout?",
                "Confirm Logout",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
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

                    // Color coding based on status
                    if ("active".equals(session.getGameState())) {
                        setBackground(isSelected ? new Color(200, 255, 200) : new Color(240, 255, 240));
                        setForeground(Color.BLACK);
                    } else if ("waiting".equals(session.getGameState())) {
                        setBackground(isSelected ? new Color(255, 255, 200) : new Color(255, 255, 240));
                        setForeground(Color.BLACK);
                    } else if ("completed".equals(session.getGameState())) {
                        setBackground(isSelected ? new Color(230, 230, 255) : new Color(245, 245, 255));
                        setForeground(Color.BLACK);
                    }
                }
            }

            return this;
        }
    }
}