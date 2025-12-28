/*
package network;

import java.io.*;
import java.net.*;

*/
/**
 * Runs inside the player app.
 * Connects to the admin server and exchanges messages.
 *//*

public class ClientHandler {

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean connected = false;

    */
/** Connect to the admin server (use "localhost" for same PC) *//*

    public boolean connect(String host, int port) {
        try {
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            connected = true;

            System.out.println("Connected to server " + host + ":" + port);

            // Thread that listens for server messages
            new Thread(() -> {
                String msg;
                try {
                    while ((msg = in.readLine()) != null) {
                        System.out.println("[SERVER] " + msg);
                    }
                } catch (IOException e) {
                    System.out.println("Disconnected from server.");
                }
            }).start();

            return true;
        } catch (IOException e) {
            System.out.println("Failed to connect: " + e.getMessage());
            return false;
        }
    }

    */
/** Send message to the server *//*

    public void send(String message) {
        if (connected && out != null) {
            out.println(message);
        }
    }

    */
/** Close connection *//*

    public void disconnect() {
        try {
            connected = false;
            if (socket != null) socket.close();
            System.out.println("Client disconnected.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
*/
