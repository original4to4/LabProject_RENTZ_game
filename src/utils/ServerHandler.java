/*
package network;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class ServerHandler {
    private ServerSocket serverSocket;
    private final List<ClientConnection> clients = new CopyOnWriteArrayList<>();
    private boolean running = false;

    public void startServer(int port) {
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            System.out.println("Game server started on port " + port);

            new Thread(() -> {
                while (running) {
                    try {
                        Socket socket = serverSocket.accept();
                        System.out.println("New player connected: " + socket.getInetAddress());
                        ClientConnection client = new ClientConnection(socket);
                        clients.add(client);
                        client.start();
                    } catch (IOException e) {
                        if (running) e.printStackTrace();
                    }
                }
            }).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void broadcast(String message) {
        for (ClientConnection c : clients) {
            c.send(message);
        }
    }

    public void stopServer() {
        running = false;
        try {
            for (ClientConnection c : clients) c.close();
            if (serverSocket != null) serverSocket.close();
            System.out.println("Server stopped.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class ClientConnection extends Thread {
        private final Socket socket;
        private PrintWriter out;
        private BufferedReader in;

        public ClientConnection(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                out.println("Welcome to the game server!");

                String msg;
                while ((msg = in.readLine()) != null) {
                    System.out.println("Received from player: " + msg);
                    broadcast("Player said: " + msg);
                }
            } catch (IOException e) {
                System.out.println("Player disconnected.");
            } finally {
                close();
                clients.remove(this);
            }
        }

        public void send(String message) {
            if (out != null) out.println(message);
        }

        public void close() {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }
}*/
