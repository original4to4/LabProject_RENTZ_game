package network;
import network.GameServer;
import javax.swing.*;

public class ServerLauncher {
    public static void main(String[] args) {
        int port = 8080;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid port, using default: 8080");
            }
        }

        GameServer server = new GameServer();
        server.start(port);

        System.out.println("🚀 Game Server is running...");
        System.out.println("📍 Connect players to: localhost:" + port);
        System.out.println("⏹️  Press Enter to stop the server");

        // Wait for shutdown command
        try {
            System.in.read();
        } catch (Exception e) {
            // Ignore
        }

        server.stop();
        System.out.println("👋 Server stopped. Goodbye!");
        System.exit(0);
    }
}