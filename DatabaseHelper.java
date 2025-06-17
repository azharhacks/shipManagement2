import java.sql.*;

public class DatabaseHelper {
    
    public static void initializeDatabase() {
        String createShipsTable = "IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'ships')\n" +
            "CREATE TABLE ships (\n" +
            "    id INT IDENTITY(1,1) PRIMARY KEY,\n" +
            "    type NVARCHAR(50) NOT NULL,\n" +
            "    location NVARCHAR(100),\n" +
            "    destination NVARCHAR(100),\n" +
            "    cargo_capacity FLOAT,\n" +
            "    passenger_capacity INT,\n" +
            "    current_load FLOAT DEFAULT 0.0\n" +
            ")";
            
        String createCargoItemsTable = "IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'cargo_items')\n" +
            "CREATE TABLE cargo_items (\n" +
            "    id INT IDENTITY(1,1) PRIMARY KEY,\n" +
            "    ship_id INT,\n" +
            "    description NVARCHAR(255) NOT NULL,\n" +
            "    weight FLOAT NOT NULL,\n" +
            "    CONSTRAINT FK_CargoItem_Ship FOREIGN KEY (ship_id) REFERENCES ships(id) ON DELETE CASCADE\n" +
            ")";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Create tables
            stmt.execute(createShipsTable);
            stmt.execute(createCargoItemsTable);
            
            System.out.println("SQL Server database tables created successfully");
            
        } catch (SQLException e) {
            System.err.println("Error initializing database: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static void executeUpdate(String sql, Object... params) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            // Set parameters
            for (int i = 0; i < params.length; i++) {
                pstmt.setObject(i + 1, params[i]);
            }
            
            pstmt.executeUpdate();
            
        } catch (SQLException e) {
            System.err.println("Error executing update: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static ResultSet executeQuery(String sql, Object... params) throws SQLException {
        Connection conn = DatabaseConnection.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql);
        
        // Set parameters
        for (int i = 0; i < params.length; i++) {
            pstmt.setObject(i + 1, params[i]);
        }
        
        return pstmt.executeQuery();
    }
}