package GUI;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;

public class FirstScreen {
    JFrame frame;
    JPanel buttonPanel;
    JPanel backgroundPanel;
    JPanel mainPanel;

    public void display() {
        frame = new JFrame("Card Game - Main Menu");
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

        backgroundPanel = createLogoPanel();
        buttonPanel = createButtonPanel();

        backgroundPanel.setOpaque(false);
        buttonPanel.setOpaque(false);

        mainPanel.add(backgroundPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        frame.add(mainPanel);
        frame.setVisible(true);

        System.out.println("Main screen displayed successfully");
    }

    private JPanel createLogoPanel() {
        JPanel logoPanel = new JPanel(new BorderLayout());
        logoPanel.setOpaque(false);
        logoPanel.setBorder(BorderFactory.createEmptyBorder(50, 50, 50, 50));

        String imagePath = "Pictures/Modern_Rentz.png";
        ImageIcon originalIcon = new ImageIcon(imagePath);

        if (originalIcon.getIconWidth() == -1) {
            JLabel titleLabel = new JLabel("CARD GAME", JLabel.CENTER);
            titleLabel.setFont(new Font("Arial", Font.BOLD, 48));
            titleLabel.setForeground(Color.WHITE);
            logoPanel.add(titleLabel, BorderLayout.CENTER);
        } else {
            Image originalImage = originalIcon.getImage();
            Image scaledImage = originalImage.getScaledInstance(700, 350, Image.SCALE_SMOOTH);
            ImageIcon scaledIcon = new ImageIcon(scaledImage);

            JLabel imageLabel = new JLabel(scaledIcon, JLabel.CENTER);
            logoPanel.add(imageLabel, BorderLayout.CENTER);
        }

        return logoPanel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 20));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 40, 20));

        JButton playerLoginBtn = createStyledButton("Login - Player", new Color(70, 130, 180));
        JButton adminLoginBtn = createStyledButton("Login - Admin", new Color(0, 150, 100));
        JButton signUpBtn = createStyledButton("Sign Up", new Color(218, 165, 32));
        JButton exitBtn = createStyledButton("Exit", new Color(200, 0, 0));

        playerLoginBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openPlayerLogin();
            }
        });

        adminLoginBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openAdminLogin();
            }
        });

        signUpBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openSignUp();
            }
        });

        exitBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                exitApplication();
            }
        });

        panel.add(playerLoginBtn);
        panel.add(adminLoginBtn);
        panel.add(signUpBtn);
        panel.add(exitBtn);

        return panel;
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

    private void openPlayerLogin() {
        System.out.println("Opening Player Login...");
        frame.dispose();
        LogInScreen logInScreen = new LogInScreen();
        logInScreen.displayAsPlayer();
    }

    private void openAdminLogin() {
        System.out.println("Opening Admin Login...");
        frame.dispose();
        LogInScreen adminLogInScreen = new LogInScreen();
        adminLogInScreen.displayAsAdmin();
    }

    private void openSignUp() {
        System.out.println("Opening Sign Up...");
        frame.dispose();
        SignUpScreen signUpScreen = new SignUpScreen();
        signUpScreen.display();
    }

    private void exitApplication() {
        int confirm = JOptionPane.showConfirmDialog(
                frame,
                "Are you sure you want to exit the application?",
                "Confirm Exit",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            System.out.println("Application exited by user");
            System.exit(0);
        }
    }
}