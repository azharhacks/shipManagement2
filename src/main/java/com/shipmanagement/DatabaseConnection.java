package com.shipmanagement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mindrot.jbcrypt.BCrypt;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;

import com.shipmanagement.model.User;

public class DatabaseConnection {
    private static SQLiteDataSource dataSource;
    
    static {
        try {
            // Configure SQLite
            SQLiteConfig config = new SQLiteConfig();
            config.enforceForeignKeys(true);
            config.setJournalMode(SQLiteConfig.JournalMode.WAL);
            config.setSynchronous(SQLiteConfig.SynchronousMode.NORMAL);
            
            // Set up the data source
            dataSource = new SQLiteDataSource(config);
            dataSource.setUrl("jdbc:sqlite:ship_management.db");
            
            // Initialize database
            initializeDatabase();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize database", e);
        }
    }
    
    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
    
    private static void initializeDatabase() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Enable foreign keys
            stmt.execute("PRAGMA foreign_keys = ON");
            
            // Create users table
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "username TEXT UNIQUE NOT NULL, " +
                "password TEXT NOT NULL, " +
                "role TEXT NOT NULL DEFAULT 'user', " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            // Create ships table
            stmt.execute("CREATE TABLE IF NOT EXISTS ships (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "type TEXT, " +
                "capacity INTEGER, " +
                "status TEXT, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
                
            // Create crew_members table
            stmt.execute("CREATE TABLE IF NOT EXISTS crew_members (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "first_name TEXT NOT NULL, " +
                "last_name TEXT NOT NULL, " +
                "position TEXT, " +
                "rank TEXT, " +
                "nationality TEXT, " +
                "date_of_birth TEXT, " +
                "license_number TEXT UNIQUE, " +
                "ship_id INTEGER, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (ship_id) REFERENCES ships(id) ON DELETE SET NULL)");
                
            // Create reports table
            stmt.execute("CREATE TABLE IF NOT EXISTS reports (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "title TEXT NOT NULL, " +
                "report_type TEXT NOT NULL, " +
                "content TEXT NOT NULL, " +
                "generated_by INTEGER, " +
                "ship_id INTEGER, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (generated_by) REFERENCES users(id) ON DELETE SET NULL, " +
                "FOREIGN KEY (ship_id) REFERENCES ships(id) ON DELETE SET NULL)");
            
            // Create default admin user if not exists
            if (!userExists("admin")) {
                String hashedPassword = BCrypt.hashpw("admin123", BCrypt.gensalt());
                try (PreparedStatement pstmt = conn.prepareStatement(
                        "INSERT INTO users (username, password, role) VALUES (?, ?, ?)")) {
                    pstmt.setString(1, "admin");
                    pstmt.setString(2, hashedPassword);
                    pstmt.setString(3, "admin");
                    pstmt.executeUpdate();
                }
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize database", e);
        }
    }
    
    public static boolean userExists(String username) {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            return rs.getInt(1) > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public static User getUserByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setUsername(rs.getString("username"));
                user.setPassword(rs.getString("password"));
                user.setRole(rs.getString("role"));
                return user;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public static boolean createUser(String username, String password, String role) {
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
        String sql = "INSERT INTO users (username, password, role) VALUES (?, ?, ?)";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, hashedPassword);
            pstmt.setString(3, role);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public static List<Map<String, Object>> getAllCrewMembers() {
        List<Map<String, Object>> crew = new ArrayList<>();
        String sql = "SELECT * FROM crew_members";
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(metaData.getColumnName(i), rs.getObject(i));
                }
                crew.add(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return crew;
    }
    
    public static boolean addCrewMember(String firstName, String lastName, String position,
                                      String rank, String nationality, String dateOfBirth,
                                      String licenseNumber, Integer shipId) {
        String sql = "INSERT INTO crew_members (first_name, last_name, position, rank, " +
                    "nationality, date_of_birth, license_number, ship_id) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, firstName);
            pstmt.setString(2, lastName);
            pstmt.setString(3, position);
            pstmt.setString(4, rank);
            pstmt.setString(5, nationality);
            pstmt.setString(6, dateOfBirth);
            pstmt.setString(7, licenseNumber);
            
            if (shipId != null) {
                pstmt.setInt(8, shipId);
            } else {
                pstmt.setNull(8, java.sql.Types.INTEGER);
            }
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public static List<Map<String, Object>> getAllReports() {
        List<Map<String, Object>> reports = new ArrayList<>();
        String sql = "SELECT r.*, u.username as generated_by_username, s.name as ship_name " +
                   "FROM reports r " +
                   "LEFT JOIN users u ON r.generated_by = u.id " +
                   "LEFT JOIN ships s ON r.ship_id = s.id " +
                   "ORDER BY r.created_at DESC";
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Map<String, Object> report = new HashMap<>();
                report.put("id", rs.getInt("id"));
                report.put("title", rs.getString("title"));
                report.put("report_type", rs.getString("report_type"));
                report.put("content", rs.getString("content"));
                report.put("generated_by", rs.getInt("generated_by"));
                report.put("generated_by_username", rs.getString("generated_by_username"));
                report.put("ship_id", rs.getInt("ship_id"));
                report.put("ship_name", rs.getString("ship_name"));
                report.put("created_at", rs.getString("created_at"));
                reports.add(report);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return reports;
    }
    
    public static boolean addReport(String title, String reportType, String content, 
                                 Integer generatedBy, Integer shipId) {
        String sql = "INSERT INTO reports (title, report_type, content, generated_by, ship_id) " +
                    "VALUES (?, ?, ?, ?, ?)";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, title);
            pstmt.setString(2, reportType);
            pstmt.setString(3, content);
            
            if (generatedBy != null) {
                pstmt.setInt(4, generatedBy);
            } else {
                pstmt.setNull(4, java.sql.Types.INTEGER);
            }
            
            if (shipId != null) {
                pstmt.setInt(5, shipId);
            } else {
                pstmt.setNull(5, java.sql.Types.INTEGER);
            }
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public static List<Map<String, Object>> getAllShips(int page, int pageSize) {
        List<Map<String, Object>> ships = new ArrayList<>();
        int offset = (page - 1) * pageSize;
        String sql = "SELECT * FROM ships ORDER BY name LIMIT ? OFFSET ?";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, pageSize);
            pstmt.setInt(2, offset);
            
            ResultSet rs = pstmt.executeQuery();
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(metaData.getColumnName(i), rs.getObject(i));
                }
                ships.add(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ships;
    }
    
    /**
     * Get all ships from the database
     * @return List of ships as maps
     */
    public static List<Map<String, Object>> getAllShips() {
        List<Map<String, Object>> ships = new ArrayList<>();
        String sql = "SELECT * FROM ships";
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Map<String, Object> ship = new HashMap<>();
                ship.put("id", rs.getInt("id"));
                ship.put("name", rs.getString("name"));
                ship.put("type", rs.getString("type"));
                ship.put("capacity", rs.getInt("capacity"));
                ship.put("status", rs.getString("status"));
                ship.put("created_at", rs.getString("created_at"));
                ships.add(ship);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return ships;
    }
    
    /**
     * Get ship details by ID
     * @param shipId The ship ID
     * @return Ship details as a map
     */
    public static Map<String, Object> getShipById(int shipId) {
        String sql = "SELECT * FROM ships WHERE id = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, shipId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                Map<String, Object> ship = new HashMap<>();
                ship.put("id", rs.getInt("id"));
                ship.put("name", rs.getString("name"));
                ship.put("type", rs.getString("type"));
                ship.put("capacity", rs.getInt("capacity"));
                ship.put("status", rs.getString("status"));
                ship.put("created_at", rs.getString("created_at"));
                
                // Get crew members for this ship
                List<Map<String, Object>> crew = new ArrayList<>();
                String crewSql = "SELECT * FROM crew_members WHERE ship_id = ?";
                try (PreparedStatement crewStmt = conn.prepareStatement(crewSql)) {
                    crewStmt.setInt(1, shipId);
                    ResultSet crewRs = crewStmt.executeQuery();
                    
                    while (crewRs.next()) {
                        Map<String, Object> crewMember = new HashMap<>();
                        crewMember.put("id", crewRs.getInt("id"));
                        crewMember.put("first_name", crewRs.getString("first_name"));
                        crewMember.put("last_name", crewRs.getString("last_name"));
                        crewMember.put("position", crewRs.getString("position"));
                        crewMember.put("rank", crewRs.getString("rank"));
                        crew.add(crewMember);
                    }
                }
                
                ship.put("crew", crew);
                return ship;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Get bookings for a specific user
     * @param userId The user ID
     * @return List of bookings as maps
     */
    public static List<Map<String, Object>> getUserBookings(int userId) {
        List<Map<String, Object>> bookings = new ArrayList<>();
        String sql = "SELECT b.*, s.name as ship_name, s.type as ship_type " +
                     "FROM bookings b " +
                     "JOIN ships s ON b.ship_id = s.id " +
                     "WHERE b.user_id = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Map<String, Object> booking = new HashMap<>();
                booking.put("id", rs.getInt("id"));
                booking.put("ship_id", rs.getInt("ship_id"));
                booking.put("ship_name", rs.getString("ship_name"));
                booking.put("ship_type", rs.getString("ship_type"));
                booking.put("start_date", rs.getString("start_date"));
                booking.put("end_date", rs.getString("end_date"));
                booking.put("status", rs.getString("status"));
                booking.put("created_at", rs.getString("created_at"));
                bookings.add(booking);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return bookings;
    }
    
    /**
     * Get tasks assigned to a specific user
     * @param userId The user ID
     * @return List of tasks as maps
     */
    public static List<Map<String, Object>> getUserTasks(int userId) {
        List<Map<String, Object>> tasks = new ArrayList<>();
        String sql = "SELECT t.*, s.name as ship_name " +
                     "FROM tasks t " +
                     "LEFT JOIN ships s ON t.ship_id = s.id " +
                     "WHERE t.assigned_to = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Map<String, Object> task = new HashMap<>();
                task.put("id", rs.getInt("id"));
                task.put("title", rs.getString("title"));
                task.put("description", rs.getString("description"));
                task.put("status", rs.getString("status"));
                task.put("ship_id", rs.getInt("ship_id"));
                task.put("ship_name", rs.getString("ship_name"));
                task.put("due_date", rs.getString("due_date"));
                task.put("created_at", rs.getString("created_at"));
                tasks.add(task);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return tasks;
    }
    
    /**
     * Get all users (for admin)
     * @return List of users as maps
     */
    public static List<Map<String, Object>> getAllUsers() {
        List<Map<String, Object>> users = new ArrayList<>();
        String sql = "SELECT id, username, role, created_at FROM users";
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Map<String, Object> user = new HashMap<>();
                user.put("id", rs.getInt("id"));
                user.put("username", rs.getString("username"));
                user.put("role", rs.getString("role"));
                user.put("created_at", rs.getString("created_at"));
                users.add(user);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return users;
    }
    
    /**
     * Updates a user's password
     * @param username The username of the user
     * @param newPassword The new password (will be hashed before storing)
     * @return true if password was updated successfully, false otherwise
     */
    public static boolean updateUserPassword(String username, String newPassword) {
        String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
        String sql = "UPDATE users SET password = ? WHERE username = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, hashedPassword);
            pstmt.setString(2, username);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Verifies if the provided password matches the stored password for a user
     * @param username The username of the user
     * @param password The password to verify
     * @return true if password matches, false otherwise
     */
    public static boolean verifyUserPassword(String username, String password) {
        User user = getUserByUsername(username);
        if (user != null) {
            return BCrypt.checkpw(password, user.getPassword());
        }
        return false;
    }
    
    /**
     * Add demo data to the database for testing purposes
     */
    public static void addDemoData() {
        try (Connection conn = getConnection()) {
            // Check if we already have demo data
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM ships")) {
                if (rs.getInt(1) > 0) {
                    // Demo data already exists
                    return;
                }
            }
            
            // Create bookings table if it doesn't exist
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE TABLE IF NOT EXISTS bookings (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "user_id INTEGER NOT NULL, " +
                    "ship_id INTEGER NOT NULL, " +
                    "start_date TEXT NOT NULL, " +
                    "end_date TEXT NOT NULL, " +
                    "status TEXT NOT NULL DEFAULT 'pending', " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (ship_id) REFERENCES ships(id) ON DELETE CASCADE)");
            }
            
            // Create tasks table if it doesn't exist
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE TABLE IF NOT EXISTS tasks (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "title TEXT NOT NULL, " +
                    "description TEXT, " +
                    "status TEXT NOT NULL DEFAULT 'pending', " +
                    "assigned_to INTEGER, " +
                    "ship_id INTEGER, " +
                    "due_date TEXT, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (assigned_to) REFERENCES users(id) ON DELETE SET NULL, " +
                    "FOREIGN KEY (ship_id) REFERENCES ships(id) ON DELETE SET NULL)");
            }
            
            // Add demo ships
            String[] shipNames = {"Ocean Explorer", "Coastal Voyager", "Sea Breeze", "Northern Star", "Pacific Wanderer"};
            String[] shipTypes = {"Cargo", "Passenger", "Tanker", "Container", "Cruise"};
            String[] shipStatuses = {"Active", "Maintenance", "Docked", "En Route", "Available"};
            int[] capacities = {5000, 2500, 8000, 10000, 3000};
            
            for (int i = 0; i < shipNames.length; i++) {
                try (PreparedStatement pstmt = conn.prepareStatement(
                        "INSERT INTO ships (name, type, capacity, status) VALUES (?, ?, ?, ?)")) {
                    pstmt.setString(1, shipNames[i]);
                    pstmt.setString(2, shipTypes[i]);
                    pstmt.setInt(3, capacities[i]);
                    pstmt.setString(4, shipStatuses[i]);
                    pstmt.executeUpdate();
                }
            }
            
            // Add demo crew members
            String[][] crewNames = {
                {"John", "Smith"}, {"Maria", "Garcia"}, {"David", "Johnson"}, 
                {"Sarah", "Williams"}, {"Michael", "Brown"}, {"Emma", "Davis"},
                {"James", "Miller"}, {"Sophia", "Wilson"}, {"Robert", "Moore"},
                {"Olivia", "Taylor"}
            };
            
            String[] positions = {"Captain", "First Officer", "Engineer", "Navigator", "Deck Officer"};
            String[] ranks = {"Senior", "Junior", "Chief", "Assistant", "Lead"};
            String[] nationalities = {"American", "British", "Canadian", "Australian", "Spanish"};
            
            for (int i = 0; i < crewNames.length; i++) {
                try (PreparedStatement pstmt = conn.prepareStatement(
                        "INSERT INTO crew_members (first_name, last_name, position, rank, " +
                        "nationality, date_of_birth, license_number, ship_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
                    pstmt.setString(1, crewNames[i][0]);
                    pstmt.setString(2, crewNames[i][1]);
                    pstmt.setString(3, positions[i % positions.length]);
                    pstmt.setString(4, ranks[i % ranks.length]);
                    pstmt.setString(5, nationalities[i % nationalities.length]);
                    pstmt.setString(6, "1980-01-" + (i + 1));
                    pstmt.setString(7, "LIC" + (10000 + i));
                    pstmt.setInt(8, (i % 5) + 1); // Assign to one of the 5 ships
                    pstmt.executeUpdate();
                }
            }
            
            // Add a regular user if not exists
            if (!userExists("user")) {
                String hashedPassword = BCrypt.hashpw("user123", BCrypt.gensalt());
                try (PreparedStatement pstmt = conn.prepareStatement(
                        "INSERT INTO users (username, password, role) VALUES (?, ?, ?)")) {
                    pstmt.setString(1, "user");
                    pstmt.setString(2, hashedPassword);
                    pstmt.setString(3, "user");
                    pstmt.executeUpdate();
                }
            }
            
            // Get user IDs
            int adminId = 0;
            int userId = 0;
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT id, username FROM users")) {
                while (rs.next()) {
                    if ("admin".equals(rs.getString("username"))) {
                        adminId = rs.getInt("id");
                    } else if ("user".equals(rs.getString("username"))) {
                        userId = rs.getInt("id");
                    }
                }
            }
            
            // Add demo reports
            String[] reportTitles = {
                "Monthly Maintenance Report", "Fuel Consumption Analysis", 
                "Safety Inspection Results", "Crew Performance Evaluation", 
                "Route Optimization Study"
            };
            
            String[] reportTypes = {"Maintenance", "Operational", "Safety", "Personnel", "Strategic"};
            
            for (int i = 0; i < reportTitles.length; i++) {
                try (PreparedStatement pstmt = conn.prepareStatement(
                        "INSERT INTO reports (title, report_type, content, generated_by, ship_id) VALUES (?, ?, ?, ?, ?)")) {
                    pstmt.setString(1, reportTitles[i]);
                    pstmt.setString(2, reportTypes[i]);
                    pstmt.setString(3, "This is a sample " + reportTypes[i].toLowerCase() + " report for demonstration purposes. It contains detailed information about " + 
                                    reportTitles[i].toLowerCase() + " and provides insights for decision making.");
                    pstmt.setInt(4, adminId);
                    pstmt.setInt(5, (i % 5) + 1);
                    pstmt.executeUpdate();
                }
            }
            
            // Add demo bookings
            String[] startDates = {"2025-07-01", "2025-07-05", "2025-07-10", "2025-07-15", "2025-07-20"};
            String[] endDates = {"2025-07-04", "2025-07-09", "2025-07-14", "2025-07-19", "2025-07-24"};
            String[] statuses = {"confirmed", "pending", "completed", "cancelled", "confirmed"};
            
            for (int i = 0; i < startDates.length; i++) {
                try (PreparedStatement pstmt = conn.prepareStatement(
                        "INSERT INTO bookings (user_id, ship_id, start_date, end_date, status) VALUES (?, ?, ?, ?, ?)")) {
                    pstmt.setInt(1, userId);
                    pstmt.setInt(2, (i % 5) + 1);
                    pstmt.setString(3, startDates[i]);
                    pstmt.setString(4, endDates[i]);
                    pstmt.setString(5, statuses[i]);
                    pstmt.executeUpdate();
                }
            }
            
            // Add demo tasks
            String[] taskTitles = {
                "Engine Maintenance", "Safety Drill", "Inventory Check", 
                "Route Planning", "Crew Scheduling", "Fuel Replenishment",
                "Hull Inspection", "Navigation System Update", "Cargo Loading",
                "Waste Management"
            };
            
            String[] taskDescriptions = {
                "Perform routine maintenance on the main engine",
                "Conduct monthly safety drill with all crew members",
                "Complete inventory check of all supplies and equipment",
                "Plan optimal route for next voyage considering weather conditions",
                "Create crew schedule for next month",
                "Arrange for fuel replenishment at next port",
                "Inspect hull for damage and schedule repairs if needed",
                "Update navigation system software to latest version",
                "Supervise loading of cargo according to manifest",
                "Implement waste management procedures according to regulations"
            };
            
            String[] taskStatuses = {"pending", "in_progress", "completed", "pending", "in_progress"};
            String[] dueDates = {"2025-07-05", "2025-07-10", "2025-07-15", "2025-07-20", "2025-07-25"};
            
            for (int i = 0; i < taskTitles.length; i++) {
                try (PreparedStatement pstmt = conn.prepareStatement(
                        "INSERT INTO tasks (title, description, status, assigned_to, ship_id, due_date) VALUES (?, ?, ?, ?, ?, ?)")) {
                    pstmt.setString(1, taskTitles[i]);
                    pstmt.setString(2, taskDescriptions[i % taskDescriptions.length]);
                    pstmt.setString(3, taskStatuses[i % taskStatuses.length]);
                    pstmt.setInt(4, i % 2 == 0 ? adminId : userId);
                    pstmt.setInt(5, (i % 5) + 1);
                    pstmt.setString(6, dueDates[i % dueDates.length]);
                    pstmt.executeUpdate();
                }
            }
            
            System.out.println("Demo data added successfully!");
            
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Failed to add demo data: " + e.getMessage());
        }
    }
}