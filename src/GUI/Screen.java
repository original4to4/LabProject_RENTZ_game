/// temporary class for tssting if the idea of printing card imiges work
package GUI;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import cards.*;
import java.util.ArrayList;
public class Screen {
    private JFrame frame;
    private JPanel cardPanel;

    public void displayHand(Card[] hand) {
        frame = new JFrame("Card Hand");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 400);
        frame.setLocationRelativeTo(null);

        // Main panel with border layout
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Title
        JLabel titleLabel = new JLabel("Your Hand - 8 Cards", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        // Panel for cards
        cardPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 10));
        cardPanel.setBackground(new Color(0, 100, 0)); // Green felt background

        // Load and display each card
        for (Card card : hand) {
            JLabel cardLabel = createCardLabel(card);
            cardPanel.add(cardLabel);
        }

        mainPanel.add(cardPanel, BorderLayout.CENTER);

        // Control panel
        JPanel controlPanel = new JPanel();
        JButton refreshButton = new JButton("New Hand");
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshHand();
            }
        });
        controlPanel.add(refreshButton);

        mainPanel.add(controlPanel, BorderLayout.SOUTH);

        frame.add(mainPanel);
        frame.setVisible(true);
    }

    private JLabel createCardLabel(Card card) {
        // Convert card data to filename format
        String fileName = card.nameOfPNG();
        String imagePath = "CARDS_PNG/" + fileName;

        // Create image icon
        ImageIcon originalIcon = new ImageIcon(imagePath);

        // Check if image loaded successfully
        if (originalIcon.getIconWidth() == -1) {
            System.err.println("Could not load image: " + imagePath);
            // Create a placeholder
            JLabel placeholder = new JLabel("Card " + card.getCardNumber() + card.getSuitSymbol());
            placeholder.setOpaque(true);
            placeholder.setBackground(Color.WHITE);
            placeholder.setPreferredSize(new Dimension(80, 120));
            placeholder.setBorder(BorderFactory.createLineBorder(Color.BLACK));
            placeholder.setHorizontalAlignment(JLabel.CENTER);
            return placeholder;
        }

        // Scale image to appropriate size
        Image originalImage = originalIcon.getImage();
        Image scaledImage = originalImage.getScaledInstance(80, 120, Image.SCALE_SMOOTH);
        ImageIcon scaledIcon = new ImageIcon(scaledImage);

        JLabel cardLabel = new JLabel(scaledIcon);
        cardLabel.setToolTipText(card.description());
        cardLabel.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));

        return cardLabel;
    }

    private void refreshHand() {
        // Create new deck and hand
        DeckOfCards deck = new DeckOfCards(6);
        deck.shuffleDeck();

        Hand hand = new Hand();
        for (int i = 0; i < 8 && i < deck.cards.size(); i++) {
            hand.addCard(deck.cards.get(i));
        }

        Card[] handArray = hand.hand.toArray(new Card[0]);

        // Clear current cards and display new ones
        cardPanel.removeAll();
        for (Card card : handArray) {
            JLabel cardLabel = createCardLabel(card);
            cardPanel.add(cardLabel);
        }

        cardPanel.revalidate();
        cardPanel.repaint();
    }

    // Original startScreen method (kept for compatibility)
    public void startScreen(){
        // This can be your login screen or removed if not needed
        JFrame screen = new JFrame();
        screen.setSize(600,500);
        screen.setVisible(true);
    }
}
