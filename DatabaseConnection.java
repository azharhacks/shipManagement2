import java.sql.*;

public class DatabaseConnection {
    private static final String DB_URL = "jdbc:sqlite:ships.db";
    private static Connection connection = null;

    public static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(DB_URL);
                System.out.println("Connected to SQLite database");
            }
        } catch (SQLException e) {
            System.err.println("Error connecting to database: " + e.getMessage());
        }
        return connection;
    }

    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database connection closed");
            }
        } catch (SQLException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }

    public static void initializeDatabase() {
        String createCargoTable = "CREATE TABLE IF NOT EXISTS cargo (" +
            "cargo_id TEXT PRIMARY KEY," +
            "owner_name TEXT NOT NULL)";

        String createCargoItemsTable = "CREATE TABLE IF NOT EXISTS cargo_items (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "cargo_id TEXT," +
            "name TEXT NOT NULL," +
            "amount INTEGER NOT NULL," +
            "FOREIGN KEY (cargo_id) REFERENCES cargo(cargo_id) ON DELETE CASCADE)";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            
            stmt.execute(createCargoTable);
            stmt.execute(createCargoItemsTable);
            System.out.println("Database tables created successfully");
            
        } catch (SQLException e) {
            System.err.println("Error creating tables: " + e.getMessage());
        }
    }
}