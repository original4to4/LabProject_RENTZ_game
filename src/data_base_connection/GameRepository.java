package data_base_connection;

import gameEngine.GameSession;
import gameEngine.GameType;
import user.Player;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GameRepository {

    public GameRepository() {
        createTablesIfNotExist();
    }

    private void createTablesIfNotExist() {
        String createGameSessionsTable = """
            CREATE TABLE IF NOT EXISTS game_sessions (
                session_id INTEGER PRIMARY KEY AUTOINCREMENT,
                host_name TEXT NOT NULL,
                player_count INTEGER NOT NULL,
                game_type TEXT,
                game_state TEXT DEFAULT 'waiting',
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
            )
        """;

        String createGamePlayersTable = """
            CREATE TABLE IF NOT EXISTS game_players (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                session_id INTEGER,
                player_name TEXT NOT NULL,
                player_email TEXT,
                player_order INTEGER,
                score INTEGER DEFAULT 0,
                FOREIGN KEY (session_id) REFERENCES game_sessions(session_id) ON DELETE CASCADE
            )
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(createGameSessionsTable);
            stmt.execute(createGamePlayersTable);
            System.out.println("Game tables created or already exist");

        } catch (SQLException e) {
            System.err.println("Error creating game tables: " + e.getMessage());
        }
    }

    public int saveGameSession(GameSession gameSession, ArrayList<Player> players) {
        String insertSessionSQL = """
            INSERT INTO game_sessions (host_name, player_count, game_state, game_type) 
            VALUES (?, ?, ?, ?)
        """;

        String insertPlayerSQL = """
            INSERT INTO game_players (session_id, player_name, player_email, player_order, score) 
            VALUES (?, ?, ?, ?, ?)
        """;

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // Insert game session
            PreparedStatement psSession = conn.prepareStatement(insertSessionSQL, Statement.RETURN_GENERATED_KEYS);
            psSession.setString(1, players.get(0).getName());
            psSession.setInt(2, players.size());
            psSession.setString(3, "waiting"); // Start as waiting for players
            psSession.setString(4, gameSession.getSelectedGame() != null ?
                    gameSession.getSelectedGame().getDisplayName() : "Not selected");

            int affectedRows = psSession.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating game session failed, no rows affected.");
            }

            // Get generated session ID
            int sessionId;
            try (ResultSet generatedKeys = psSession.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    sessionId = generatedKeys.getInt(1);
                } else {
                    throw new SQLException("Creating game session failed, no ID obtained.");
                }
            }

            // Insert players
            PreparedStatement psPlayer = conn.prepareStatement(insertPlayerSQL);
            for (int i = 0; i < players.size(); i++) {
                Player player = players.get(i);
                psPlayer.setInt(1, sessionId);
                psPlayer.setString(2, player.getName());
                psPlayer.setString(3, player.getEmail());
                psPlayer.setInt(4, i);
                psPlayer.setInt(5, 0);
                psPlayer.addBatch();
            }

            psPlayer.executeBatch();
            conn.commit();

            System.out.println("Game session saved with ID: " + sessionId + ", Players: " + players.size());
            return sessionId;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            System.err.println("Error saving game session: " + e.getMessage());
            throw new RuntimeException("Failed to save game session", e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public List<GameSessionInfo> getActiveGameSessions() {
        String sql = """
            SELECT s.session_id, s.host_name, s.player_count, s.game_state, s.game_type, s.created_at,
                   COUNT(p.id) as joined_players
            FROM game_sessions s
            LEFT JOIN game_players p ON s.session_id = p.session_id
            WHERE s.game_state IN ('waiting', 'active')
            GROUP BY s.session_id
            ORDER BY s.created_at DESC
        """;

        List<GameSessionInfo> sessions = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                GameSessionInfo sessionInfo = new GameSessionInfo(
                        rs.getInt("session_id"),
                        rs.getString("host_name"),
                        rs.getInt("player_count"),
                        rs.getInt("joined_players"),
                        rs.getString("game_state"),
                        rs.getTimestamp("created_at"),
                        rs.getString("game_type")
                );
                sessions.add(sessionInfo);
            }

        } catch (SQLException e) {
            System.err.println("Error retrieving game sessions: " + e.getMessage());
        }

        return sessions;
    }

    public GameSessionInfo getGameSessionById(int sessionId) {
        String sql = """
            SELECT s.session_id, s.host_name, s.player_count, s.game_state, s.game_type, s.created_at,
                   COUNT(p.id) as joined_players
            FROM game_sessions s
            LEFT JOIN game_players p ON s.session_id = p.session_id
            WHERE s.session_id = ?
            GROUP BY s.session_id
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, sessionId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new GameSessionInfo(
                        rs.getInt("session_id"),
                        rs.getString("host_name"),
                        rs.getInt("player_count"),
                        rs.getInt("joined_players"),
                        rs.getString("game_state"),
                        rs.getTimestamp("created_at"),
                        rs.getString("game_type")
                );
            }

        } catch (SQLException e) {
            System.err.println("Error retrieving game session: " + e.getMessage());
        }

        return null;
    }

    public boolean updateGameSessionState(int sessionId, String state) {
        String sql = "UPDATE game_sessions SET game_state = ?, updated_at = CURRENT_TIMESTAMP WHERE session_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, state);
            stmt.setInt(2, sessionId);

            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            System.err.println("Error updating game session state: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteGameSession(int sessionId) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            String deletePlayersSQL = "DELETE FROM game_players WHERE session_id = ?";
            PreparedStatement deletePlayersStmt = conn.prepareStatement(deletePlayersSQL);
            deletePlayersStmt.setInt(1, sessionId);
            deletePlayersStmt.executeUpdate();

            String deleteSessionSQL = "DELETE FROM game_sessions WHERE session_id = ?";
            PreparedStatement deleteSessionStmt = conn.prepareStatement(deleteSessionSQL);
            deleteSessionStmt.setInt(1, sessionId);

            int affectedRows = deleteSessionStmt.executeUpdate();
            conn.commit();

            return affectedRows > 0;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            System.err.println("Error deleting game session: " + e.getMessage());
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public GameSession loadGameSession(int sessionId) {
        String sessionSQL = "SELECT * FROM game_sessions WHERE session_id = ?";
        String playersSQL = "SELECT * FROM game_players WHERE session_id = ? ORDER BY player_order";

        try (Connection conn = DatabaseConnection.getConnection()) {
            // Load session information
            PreparedStatement sessionStmt = conn.prepareStatement(sessionSQL);
            sessionStmt.setInt(1, sessionId);
            ResultSet sessionRs = sessionStmt.executeQuery();

            if (!sessionRs.next()) {
                System.out.println("No game session found with ID: " + sessionId);
                return null;
            }

            String hostName = sessionRs.getString("host_name");
            int playerCount = sessionRs.getInt("player_count");
            String gameState = sessionRs.getString("game_state");
            String gameType = sessionRs.getString("game_type");

            System.out.println("Loading game session " + sessionId + ": " + hostName + " (" + playerCount + " players)");

            // Load players
            PreparedStatement playersStmt = conn.prepareStatement(playersSQL);
            playersStmt.setInt(1, sessionId);
            ResultSet playersRs = playersStmt.executeQuery();

            ArrayList<Player> players = new ArrayList<>();
            while (playersRs.next()) {
                String playerName = playersRs.getString("player_name");
                String playerEmail = playersRs.getString("player_email");
                int playerOrder = playersRs.getInt("player_order");
                int score = playersRs.getInt("score");

                Player player = new Player(playerName, playerEmail, "password");
                player.addScore(score);
                players.add(player);

                System.out.println("Loaded player: " + playerName + " (order: " + playerOrder + ", score: " + score + ")");
            }

            if (players.isEmpty()) {
                System.out.println("No players found for session " + sessionId);
                return null;
            }

            // Create game session
            GameSession gameSession = new GameSession(players);
            gameSession.setSessionId(sessionId);

            // Set game type if available
            if (gameType != null && !gameType.isEmpty()) {
                try {
                    for (GameType type : GameType.values()) {
                        if (type.getDisplayName().equals(gameType)) {
                            // We need to simulate game selection - this is a workaround
                            System.out.println("Setting game type to: " + gameType);
                            break;
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error setting game type: " + e.getMessage());
                }
            }

            // Start game if it was active
            if ("active".equals(gameState)) {
                gameSession.startGame();
            }

            System.out.println("Successfully loaded game session " + sessionId + " with " + players.size() + " players");
            return gameSession;

        } catch (SQLException e) {
            System.err.println("Error loading game session " + sessionId + ": " + e.getMessage());
            return null;
        }
    }

    public List<GameSessionInfo> getGameSessionsForPlayer(String playerName) {
        String sql = """
            SELECT DISTINCT s.session_id, s.host_name, s.player_count, s.game_state, s.game_type, s.created_at,
                   COUNT(p.id) as joined_players
            FROM game_sessions s
            LEFT JOIN game_players p ON s.session_id = p.session_id
            WHERE s.game_state IN ('waiting', 'active')
            GROUP BY s.session_id
            HAVING joined_players < s.player_count OR EXISTS (
                SELECT 1 FROM game_players p2 
                WHERE p2.session_id = s.session_id AND p2.player_name = ?
            )
            ORDER BY s.created_at DESC
        """;

        List<GameSessionInfo> sessions = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, playerName);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                GameSessionInfo sessionInfo = new GameSessionInfo(
                        rs.getInt("session_id"),
                        rs.getString("host_name"),
                        rs.getInt("player_count"),
                        rs.getInt("joined_players"),
                        rs.getString("game_state"),
                        rs.getTimestamp("created_at"),
                        rs.getString("game_type")
                );
                sessions.add(sessionInfo);
            }

        } catch (SQLException e) {
            System.err.println("Error retrieving game sessions for player: " + e.getMessage());
        }

        return sessions;
    }

    public boolean addPlayerToSession(int sessionId, Player player) {
        String sql = "INSERT INTO game_players (session_id, player_name, player_email, player_order, score) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            int playerOrder = getNextPlayerOrder(sessionId);

            stmt.setInt(1, sessionId);
            stmt.setString(2, player.getName());
            stmt.setString(3, player.getEmail());
            stmt.setInt(4, playerOrder);
            stmt.setInt(5, 0);

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                System.out.println("Player '" + player.getName() + "' added to session " + sessionId + " as player " + playerOrder);

                // Update joined players count
                updateSessionPlayerCount(sessionId);
                return true;
            }

        } catch (SQLException e) {
            System.err.println("Error adding player to session: " + e.getMessage());
        }

        return false;
    }

    private int getNextPlayerOrder(int sessionId) {
        String sql = "SELECT MAX(player_order) as max_order FROM game_players WHERE session_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, sessionId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("max_order") + 1;
            }

        } catch (SQLException e) {
            System.err.println("Error getting next player order: " + e.getMessage());
        }

        return 0;
    }

    private void updateSessionPlayerCount(int sessionId) {
        String sql = "UPDATE game_sessions SET player_count = (SELECT COUNT(*) FROM game_players WHERE session_id = ?) WHERE session_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, sessionId);
            stmt.setInt(2, sessionId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Error updating session player count: " + e.getMessage());
        }
    }

    public boolean isUserAdmin(String username) {
        String sql = "SELECT isAdmin FROM users WHERE username = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getBoolean("isAdmin");
            }

        } catch (SQLException e) {
            System.err.println("Error checking admin status: " + e.getMessage());
        }

        return false;
    }

    public List<String> getSessionPlayers(int sessionId) {
        String sql = "SELECT player_name FROM game_players WHERE session_id = ? ORDER BY player_order";
        List<String> players = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, sessionId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                players.add(rs.getString("player_name"));
            }

        } catch (SQLException e) {
            System.err.println("Error getting session players: " + e.getMessage());
        }

        return players;
    }
}