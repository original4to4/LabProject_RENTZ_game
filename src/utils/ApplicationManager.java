package utils;

import java.util.UUID;

public class ApplicationManager {
    private static String instanceId;
    private static boolean isMultiInstance = false;
    private static boolean networkMode = false;
    private static String serverHost = "localhost";
    private static int serverPort = 8080;

    public static void initializeMultiInstance() {
        instanceId = UUID.randomUUID().toString().substring(0, 8);
        isMultiInstance = true;
        System.out.println("Initialized application instance: " + instanceId);
    }

    public static String getInstanceId() {
        if (instanceId == null) {
            instanceId = "main";
        }
        return instanceId;
    }

    public static boolean isMultiInstance() {
        return isMultiInstance;
    }

    public static String getDatabasePath() {
        if (isMultiInstance) {
            return "jdbc:sqlite:cardgame_" + instanceId + ".db";
        }
        return "jdbc:sqlite:cardgame.db";
    }

    // Network configuration methods
    public static void setNetworkMode(boolean networkMode) {
        ApplicationManager.networkMode = networkMode;
    }

    public static boolean isNetworkMode() {
        return networkMode;
    }

    public static void setServerHost(String host) {
        serverHost = host;
    }

    public static String getServerHost() {
        return serverHost;
    }

    public static void setServerPort(int port) {
        serverPort = port;
    }

    public static int getServerPort() {
        return serverPort;
    }
}
