package GUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import data_base_connection.GameRepository;
import data_base_connection.GameSessionInfo;

public class GameManagementScreen {
    private JFrame frame;
    private JPanel mainPanel;
    private JList<GameSessionInfo> sessionsList;
    private DefaultListModel<GameSessionInfo> listModel;
    private JButton refreshButton;
    private JButton deleteButton;
    private JButton viewButton;
    private JButton backButton;
    private JLabel statusLabel;

    private GameRepository gameRepository;

    public GameManagementScreen(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
        initialize();
    }

    public void display() {
        frame.setVisible(true);
        refreshSessionsList();
    }

    private void initialize() {
        createFrame();
        createMainPanel();
        createSessionsList();
        createControlPanel();

        frame.add(mainPanel);
        frame.setLocationRelativeTo(null);
    }

    private void createFrame() {
        frame = new JFrame("Game Session Management");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(700, 500);
    }

    private void createMainPanel() {
        mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(new Color(240, 240, 240));

        // Title
        JLabel titleLabel = new JLabel("Active Game Sessions", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        mainPanel.add(titleLabel, BorderLayout.NORTH);
    }

    private void createSessionsList() {
        listModel = new DefaultListModel<>();
        sessionsList = new JList<>(listModel);
        sessionsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        sessionsList.setFont(new Font("Arial", Font.PLAIN, 14));
        sessionsList.setCellRenderer(new SessionListRenderer());

        JScrollPane scrollPane = new JScrollPane(sessionsList);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Game Sessions"));
        mainPanel.add(scrollPane, BorderLayout.CENTER);
    }

    private void createControlPanel() {
        JPanel controlPanel = new JPanel(new BorderLayout());

        // Status label
        statusLabel = new JLabel("Select a game session to manage", JLabel.CENTER);
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        controlPanel.add(statusLabel, BorderLayout.NORTH);

        // Buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));

        refreshButton = createStyledButton("Refresh", new Color(70, 130, 180));
        refreshButton.addActionListener(e -> refreshSessionsList());

        viewButton = createStyledButton("View Details", new Color(0, 150, 0));
        viewButton.addActionListener(e -> viewSelectedSession());

        deleteButton = createStyledButton("Delete Session", new Color(200, 0, 0));
        deleteButton.addActionListener(e -> deleteSelectedSession());

        backButton = createStyledButton("Back to Admin", new Color(150, 150, 150));
        backButton.addActionListener(e -> goBackToAdmin());

        buttonPanel.add(refreshButton);
        buttonPanel.add(viewButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(backButton);

        controlPanel.add(buttonPanel, BorderLayout.CENTER);
        mainPanel.add(controlPanel, BorderLayout.SOUTH);
    }

    private JButton createStyledButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 12));
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        return button;
    }

    private void refreshSessionsList() {
        listModel.clear();
        List<GameSessionInfo> sessions = gameRepository.getActiveGameSessions();

        if (sessions.isEmpty()) {
            listModel.addElement(new GameSessionInfo(0, "No active sessions found", 0, 0, "none", null));
            viewButton.setEnabled(false);
            deleteButton.setEnabled(false);
        } else {
            for (GameSessionInfo session : sessions) {
                listModel.addElement(session);
            }
            viewButton.setEnabled(true);
            deleteButton.setEnabled(true);
        }

        statusLabel.setText("Found " + sessions.size() + " active game sessions");
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
            <b>Players:</b> %d/%d<br>
            <b>Status:</b> %s<br>
            <b>Created:</b> %s<br>
            </html>
            """,
                selected.getSessionId(),
                selected.getHostName(),
                selected.getJoinedPlayers(),
                selected.getPlayerCount(),
                selected.getGameState(),
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

    private void deleteSelectedSession() {
        GameSessionInfo selected = sessionsList.getSelectedValue();
        if (selected == null || selected.getSessionId() == 0) {
            JOptionPane.showMessageDialog(frame, "Please select a valid game session");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                frame,
                "Are you sure you want to delete session " + selected.getSessionId() + "?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            boolean success = gameRepository.deleteGameSession(selected.getSessionId());
            if (success) {
                JOptionPane.showMessageDialog(frame, "Session deleted successfully");
                refreshSessionsList();
            } else {
                JOptionPane.showMessageDialog(frame, "Failed to delete session");
            }
        }
    }

    private void goBackToAdmin() {
        frame.dispose();
        new AdminScreen().display();
    }

    // Custom list renderer for better appearance
    private static class SessionListRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value instanceof GameSessionInfo) {
                GameSessionInfo session = (GameSessionInfo) value;
                setText(session.toString());

                // Color coding based on status
                if ("active".equals(session.getGameState())) {
                    setBackground(isSelected ? new Color(200, 255, 200) : new Color(240, 255, 240));
                } else if ("waiting".equals(session.getGameState())) {
                    setBackground(isSelected ? new Color(255, 255, 200) : new Color(255, 255, 240));
                }
            }

            return this;
        }
    }
}