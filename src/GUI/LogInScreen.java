package GUI;

import java.awt.*;
import javax.swing.*;
import data_base_connection.UserRepository;
import user.User;

public class LogInScreen {
    JFrame frame;
    JPanel mainPanel;
    JTextField usernameField;
    JPasswordField passwordField;
    JButton logInButton;
    JButton backButton;

    private final UserRepository userRepository = new UserRepository();
    private boolean isAdminLogin = false;

    public void display() {
        frame = new JFrame("Login");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 600);
        frame.setLocationRelativeTo(null);

        mainPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                ImageIcon backgroundIcon = new ImageIcon("Pictures/background.jpg");
                if (backgroundIcon.getIconWidth() != -1) {
                    Image backgroundImage = backgroundIcon.getImage();
                    g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
                } else {
                    g.setColor(new Color(0, 100, 0));
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
            }
        };

        JPanel formPanel = createFormPanel();

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottomPanel.setOpaque(false);
        backButton = createStyledButton("Back to Main", new Color(150, 150, 150));
        backButton.addActionListener(e -> goBackToMain());
        bottomPanel.add(backButton);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        mainPanel.add(formPanel, BorderLayout.CENTER);
        frame.add(mainPanel);
        frame.setVisible(true);
    }

    public void displayAsAdmin() {
        isAdminLogin = true;
        display();
        frame.setTitle("Admin Login");
    }

    public void displayAsPlayer() {
        isAdminLogin = false;
        display();
        frame.setTitle("Player Login");
    }

    private JPanel createFormPanel() {
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        formPanel.setBorder(BorderFactory.createEmptyBorder(50, 100, 50, 100));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        String title = isAdminLogin ? "Admin Login" : "Player Login";
        JLabel titleLabel = new JLabel(title, JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        formPanel.add(titleLabel, gbc);

        JLabel userLabel = new JLabel("Username:");
        userLabel.setForeground(Color.WHITE);
        userLabel.setFont(new Font("Arial", Font.BOLD, 14));
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.LINE_END;
        formPanel.add(userLabel, gbc);

        usernameField = new JTextField(20);
        usernameField.setFont(new Font("Arial", Font.PLAIN, 14));
        usernameField.setPreferredSize(new Dimension(200, 30));
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.LINE_START;
        formPanel.add(usernameField, gbc);

        JLabel passLabel = new JLabel("Password:");
        passLabel.setForeground(Color.WHITE);
        passLabel.setFont(new Font("Arial", Font.BOLD, 14));
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.LINE_END;
        formPanel.add(passLabel, gbc);

        passwordField = new JPasswordField(20);
        passwordField.setFont(new Font("Arial", Font.PLAIN, 14));
        passwordField.setPreferredSize(new Dimension(200, 30));
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.LINE_START;
        formPanel.add(passwordField, gbc);

        logInButton = createStyledButton("Log In", new Color(70, 130, 180));
        logInButton.addActionListener(e -> attemptLogin());
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        formPanel.add(logInButton, gbc);

        return formPanel;
    }

    private JButton createStyledButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 16));
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(30, 80, 120), 2),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setOpaque(true);

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(brighterColor(color));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
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

    private void attemptLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(frame,
                    "Please enter both username and password",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        User user = userRepository.findByUsernameAndPassword(username, password);
        if (user != null) {
            boolean isAdmin = userRepository.isUserAdmin(username);

            if (isAdminLogin && !isAdmin) {
                JOptionPane.showMessageDialog(frame,
                        "This account is not an admin account. Please use player login.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (!isAdminLogin && isAdmin) {
                JOptionPane.showMessageDialog(frame,
                        "Admin accounts cannot login as players. Please use admin login.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Show success message
            JOptionPane.showMessageDialog(frame,
                    "Login successful! Welcome " + username,
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);

            // Redirect based on user type
            if (isAdminLogin) {
                redirectToAdminDashboard();
            } else {
                redirectToPlayerDashboard(user);
            }
        } else {
            JOptionPane.showMessageDialog(frame,
                    "Invalid username or password",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void redirectToAdminDashboard() {
        frame.dispose();

        SwingUtilities.invokeLater(() -> {
            try {
                AdminScreen adminScreen = new AdminScreen();
                adminScreen.display();
                System.out.println("Admin successfully redirected to Admin Dashboard");
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null,
                        "Error opening admin dashboard: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                new FirstScreen().display();
            }
        });
    }

    private void redirectToPlayerDashboard(User user) {
        frame.dispose();

        SwingUtilities.invokeLater(() -> {
            try {
                PlayerDashboard playerDashboard = new PlayerDashboard(user);
                playerDashboard.display();
                System.out.println("Player '" + user.getName() + "' redirected to Player Dashboard");
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null,
                        "Error opening player dashboard: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                new FirstScreen().display();
            }
        });
    }

    private void goBackToMain() {
        frame.dispose();
        FirstScreen screen = new FirstScreen();
        screen.display();
    }
}