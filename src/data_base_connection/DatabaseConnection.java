package data_base_connection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Manages the connection to the SQLite database.
 * Database file will be created automatically in your project folder.
 */
public class DatabaseConnection {

    private static final String DB_URL = "jdbc:sqlite:cardgame.db";

    static {
        try {
            // Load the SQLite JDBC driver
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("Failed to load SQLite JDBC driver.");
        }
    }

    /** Opens and returns a connection to the database. */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }
}
