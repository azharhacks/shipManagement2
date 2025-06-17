import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
public class DatabaseConnection {
   private static final String DB_URL = "jdbc:sqlserver://localhost:1433;databaseName=ship_management;encrypt=true;trustServerCertificate=true";
    private static final String USER = "SUN";
    private static final String PASSWORD = "4024";
    private static Connection connection = null;

    private DatabaseConnection() {}

    public static Connection getConnection() {
        if (connection == null) {
            try {
                // Load the SQL Server JDBC driver
                Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
                // Create a connection to the database
                connection = DriverManager.getConnection(DB_URL, USER, PASSWORD);
                System.out.println("Connected to SQL Server database successfully");
            } catch (ClassNotFoundException | SQLException e) {
                System.err.println("Error connecting to database: " + e.getMessage());
                e.printStackTrace();
            }
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
            System.err.println("Error closing database connection: " + e.getMessage());
        }
    }
}
}
