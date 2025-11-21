package data_base_connection;
import data_base_connection.DatabaseConnection;
import java.sql.Connection;

public class TestDBConnection {
    public static void main(String[] args) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn != null) {
                System.out.println("✅ Conexiunea la baza de date functioneaza!");
            } else {
                System.out.println("❌ Conexiunea a esuat (conn = null)");
            }
        } catch (Exception e) {
            System.out.println("❌ Eroare la conectare:");
            e.printStackTrace();
        }
    }
}
