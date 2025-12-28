import GUI.FirstScreen;
import utils.ApplicationManager;
import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        // Parse command line arguments
        boolean multiInstance = false;

        for (String arg : args) {
            if (arg.equals("--multi")) {
                multiInstance = true;
            }
        }

        if (multiInstance) {
            ApplicationManager.initializeMultiInstance();
        }

        // Create final variables for use in lambda
        SwingUtilities.invokeLater(() -> {
            try {
                FirstScreen firstScreen = new FirstScreen();
                firstScreen.display();
                String instanceInfo = ApplicationManager.isMultiInstance() ?
                        " (Instance: " + ApplicationManager.getInstanceId() + ")" : " [Single Instance]";
                System.out.println("Card Game Application started successfully" + instanceInfo);
                System.out.println("Mode: Local Multiplayer with Sequential Login");
            } catch (Exception e) {
                System.err.println("Failed to start application: " + e.getMessage());
                JOptionPane.showMessageDialog(null,
                        "Failed to start application: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}