import GUI.FirstScreen;
import utils.ApplicationManager;
import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        // Parse command line arguments
        boolean multiInstance = false;
        boolean networkMode = false;
        String serverHost = "localhost";
        int serverPort = 8080;

        for (String arg : args) {
            if (arg.equals("--multi")) {
                multiInstance = true;
            } else if (arg.equals("--network")) {
                networkMode = true;
            } else if (arg.startsWith("--server=")) {
                String[] parts = arg.split("=");
                if (parts.length == 2) {
                    serverHost = parts[1];
                }
            } else if (arg.startsWith("--port=")) {
                String[] parts = arg.split("=");
                if (parts.length == 2) {
                    try {
                        serverPort = Integer.parseInt(parts[1]);
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid port, using default: 8080");
                    }
                }
            }
        }

        if (multiInstance) {
            ApplicationManager.initializeMultiInstance();
        }

        // Store network settings for use in the application
        if (networkMode) {
            ApplicationManager.setNetworkMode(true);
            ApplicationManager.setServerHost(serverHost);
            ApplicationManager.setServerPort(serverPort);
        }

        // Create final variables for use in lambda
        final boolean finalNetworkMode = networkMode;

        SwingUtilities.invokeLater(() -> {
            try {
                FirstScreen firstScreen = new FirstScreen();
                firstScreen.display();
                String instanceInfo = ApplicationManager.isMultiInstance() ?
                        " (Instance: " + ApplicationManager.getInstanceId() + ")" : "";
                String networkInfo = finalNetworkMode ? " [Network Mode]" : " [Local Mode]";
                System.out.println("Application started successfully" + instanceInfo + networkInfo);
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