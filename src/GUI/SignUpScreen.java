package GUI;

import java.awt.*;
import javax.swing.*;
import data_base_connection.UserRepository;
import user.User;

public class SignUpScreen {
    JFrame frame;
    JPanel mainPanel;
    JTextField nameField;
    JTextField emailField;
    JPasswordField passwordField;
    JPasswordField confirmPasswordField;
    JCheckBox adminCheckBox;
    JButton signUpButton;
    JButton backButton;

    private final UserRepository userRepository = new UserRepository();

    public void display() {
        frame = new JFrame("Sign Up");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 700);
        frame.setLocationRelativeTo(null);

        mainPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                ImageIcon backgroundIcon = new ImageIcon("Pictures/background.jpg");
                if (backgroundIcon.getIconWidth() != -1) {
                    g.drawImage(backgroundIcon.getImage(), 0, 0, getWidth(), getHeight(), this);
                } else {
                    g.setColor(new Color(0, 100, 0));
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
            }
        };

        JPanel formPanel = createFormPanel();

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        backButton = createStyledButton("Back to Main", new Color(150, 150, 150));
        backButton.addActionListener(e -> goBackToMain());
        bottomPanel.add(backButton);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        mainPanel.add(formPanel, BorderLayout.CENTER);
        frame.add(mainPanel);
        frame.setVisible(true);
    }

    private JPanel createFormPanel() {
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        formPanel.setBorder(BorderFactory.createEmptyBorder(40, 100, 40, 100));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLabel = new JLabel("Create Account", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(Color.WHITE);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        formPanel.add(titleLabel, gbc);

        // ... (rest of the form fields remain the same as before)

        JLabel nameLabel = new JLabel("Username:");
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setFont(new Font("Arial", Font.BOLD, 14));
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.LINE_END;
        formPanel.add(nameLabel, gbc);

        nameField = new JTextField(20);
        nameField.setFont(new Font("Arial", Font.PLAIN, 14));
        nameField.setPreferredSize(new Dimension(200, 30));
        gbc.gridx = 1; gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.LINE_START;
        formPanel.add(nameField, gbc);

        JLabel emailLabel = new JLabel("Email:");
        emailLabel.setForeground(Color.WHITE);
        emailLabel.setFont(new Font("Arial", Font.BOLD, 14));
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.LINE_END;
        formPanel.add(emailLabel, gbc);

        emailField = new JTextField(20);
        emailField.setFont(new Font("Arial", Font.PLAIN, 14));
        emailField.setPreferredSize(new Dimension(200, 30));
        gbc.gridx = 1; gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.LINE_START;
        formPanel.add(emailField, gbc);

        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setForeground(Color.WHITE);
        passwordLabel.setFont(new Font("Arial", Font.BOLD, 14));
        gbc.gridx = 0; gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.LINE_END;
        formPanel.add(passwordLabel, gbc);

        passwordField = new JPasswordField(20);
        passwordField.setFont(new Font("Arial", Font.PLAIN, 14));
        passwordField.setPreferredSize(new Dimension(200, 30));
        gbc.gridx = 1; gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.LINE_START;
        formPanel.add(passwordField, gbc);

        JLabel confirmPasswordLabel = new JLabel("Confirm Password:");
        confirmPasswordLabel.setForeground(Color.WHITE);
        confirmPasswordLabel.setFont(new Font("Arial", Font.BOLD, 14));
        gbc.gridx = 0; gbc.gridy = 4;
        gbc.anchor = GridBagConstraints.LINE_END;
        formPanel.add(confirmPasswordLabel, gbc);

        confirmPasswordField = new JPasswordField(20);
        confirmPasswordField.setFont(new Font("Arial", Font.PLAIN, 14));
        confirmPasswordField.setPreferredSize(new Dimension(200, 30));
        gbc.gridx = 1; gbc.gridy = 4;
        gbc.anchor = GridBagConstraints.LINE_START;
        formPanel.add(confirmPasswordField, gbc);

        adminCheckBox = new JCheckBox("Create Admin Account");
        adminCheckBox.setOpaque(false);
        adminCheckBox.setForeground(Color.WHITE);
        adminCheckBox.setFont(new Font("Arial", Font.BOLD, 14));
        adminCheckBox.setFocusPainted(false);
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        formPanel.add(adminCheckBox, gbc);

        signUpButton = createStyledButton("Sign Up", new Color(218, 165, 32));
        signUpButton.addActionListener(e -> attemptSignUp());
        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(20, 8, 8, 8);
        formPanel.add(signUpButton, gbc);

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

    private void attemptSignUp() {
        String username = nameField.getText().trim();
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());
        boolean isAdmin = adminCheckBox.isSelected();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please fill in all fields", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!password.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(frame, "Passwords do not match", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (userRepository.usernameExists(username)) {
            JOptionPane.showMessageDialog(frame, "Username already exists", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        User user = new User(username, email, password);
        boolean success = userRepository.addUser(user, isAdmin);

        if (success) {
            String userType = isAdmin ? "Admin" : "Player";
            JOptionPane.showMessageDialog(frame,
                    userType + " account created successfully!\nYou can now login with your credentials.",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);

            // Auto-redirect to appropriate login after successful signup
            redirectAfterSignup(isAdmin);
        } else {
            JOptionPane.showMessageDialog(frame, "Registration failed. Try again.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void redirectAfterSignup(boolean isAdmin) {
        frame.dispose();

        SwingUtilities.invokeLater(() -> {
            if (isAdmin) {
                // Redirect to admin login
                LogInScreen adminLogin = new LogInScreen();
                adminLogin.displayAsAdmin();
                System.out.println("Redirecting new admin to login screen");
            } else {
                // Redirect to player login
                LogInScreen playerLogin = new LogInScreen();
                playerLogin.displayAsPlayer();
                System.out.println("Redirecting new player to login screen");
            }
        });
    }

    private void goBackToMain() {
        frame.dispose();
        FirstScreen screen = new FirstScreen();
        screen.display();
    }
}