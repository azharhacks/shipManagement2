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
            
            // Create docks table
            stmt.execute("CREATE TABLE IF NOT EXISTS docks (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "location TEXT NOT NULL, " +
                "capacity INTEGER, " +
                "status TEXT DEFAULT 'available', " + // available, occupied, maintenance
                "ship_id INTEGER, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (ship_id) REFERENCES ships(id) ON DELETE SET NULL)");
                
            // Create ship_locations table
            stmt.execute("CREATE TABLE IF NOT EXISTS ship_locations (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "ship_id INTEGER NOT NULL, " +
                "latitude REAL, " +
                "longitude REAL, " +
                "current_port TEXT, " +
                "status TEXT, " + // at sea, docked, in transit
                "last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (ship_id) REFERENCES ships(id) ON DELETE CASCADE)");
                
            // Create tasks table
            stmt.execute("CREATE TABLE IF NOT EXISTS tasks (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "title TEXT NOT NULL, " +
                "description TEXT, " +
                "assigned_by INTEGER, " + // admin user id
                "assigned_to INTEGER, " + // staff user id
                "ship_id INTEGER, " +
                "priority TEXT DEFAULT 'medium', " + // low, medium, high, urgent
                "status TEXT DEFAULT 'pending', " + // pending, accepted, in_progress, completed, rejected
                "due_date TEXT, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (assigned_by) REFERENCES users(id) ON DELETE SET NULL, " +
                "FOREIGN KEY (assigned_to) REFERENCES users(id) ON DELETE SET NULL, " +
                "FOREIGN KEY (ship_id) REFERENCES ships(id) ON DELETE SET NULL)");
                
            // Create problem_reports table
            stmt.execute("CREATE TABLE IF NOT EXISTS problem_reports (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "title TEXT NOT NULL, " +
                "description TEXT NOT NULL, " +
                "reported_by INTEGER NOT NULL, " + // staff user id
                "ship_id INTEGER, " +
                "severity TEXT DEFAULT 'medium', " + // low, medium, high, critical
                "status TEXT DEFAULT 'open', " + // open, in_progress, resolved, closed
                "resolution_notes TEXT, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "resolved_at TIMESTAMP, " +
                "FOREIGN KEY (reported_by) REFERENCES users(id) ON DELETE SET NULL, " +
                "FOREIGN KEY (ship_id) REFERENCES ships(id) ON DELETE SET NULL)");
            
            // Create staff table
            stmt.execute("CREATE TABLE IF NOT EXISTS staff (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "first_name TEXT NOT NULL, " +
                "last_name TEXT NOT NULL, " +
                "username TEXT UNIQUE NOT NULL, " +
                "password TEXT NOT NULL, " +
                "role TEXT NOT NULL, " +
                "status TEXT NOT NULL DEFAULT 'active', " +
                "ship_id INTEGER, " +
                "email TEXT, " +
                "phone TEXT, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (ship_id) REFERENCES ships(id) ON DELETE SET NULL)");
            
            // Create bookings table
            stmt.execute("CREATE TABLE IF NOT EXISTS bookings (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "ship_id INTEGER NOT NULL, " +
                "user_id INTEGER NOT NULL, " +
                "start_date TEXT NOT NULL, " +
                "end_date TEXT NOT NULL, " +
                "purpose TEXT, " +
                "status TEXT NOT NULL DEFAULT 'pending', " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (ship_id) REFERENCES ships(id) ON DELETE CASCADE, " +
                "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE)");
            
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
            
            // Create default staff user if not exists
            if (!userExists("staff")) {
                String hashedPassword = BCrypt.hashpw("staff123", BCrypt.gensalt());
                try (PreparedStatement pstmt = conn.prepareStatement(
                        "INSERT INTO users (username, password, role) VALUES (?, ?, ?)")) {
                    pstmt.setString(1, "staff");
                    pstmt.setString(2, hashedPassword);
                    pstmt.setString(3, "staff");
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
    
    /**
     * Creates a new user in the database
     * @param username The username
     * @param password The password (will be hashed)
     * @param role The user role (user, staff, admin)
     * @return true if user was created successfully, false otherwise
     */
    public static boolean createUser(String username, String password, String role) {
        if (usernameExists(username)) {
            return false;
        }
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
    
    /**
     * Checks if a username already exists in the database
     * @param username The username to check
     * @return true if the username exists, false otherwise
     */
    public static boolean usernameExists(String username) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT COUNT(*) FROM users WHERE username = ?"
             )) {
            
            stmt.setString(1, username);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
            
            return false;
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
     * Get tasks assigned to a specific user
     * @param userId The user ID
     * @return List of tasks as maps
     */
    public static List<Map<String, Object>> getUserTasks(int userId) {
        List<Map<String, Object>> tasks = new ArrayList<>();
        String sql = "SELECT t.*, " +
                    "s1.first_name || ' ' || s1.last_name as assigned_to_name, " +
                    "s2.first_name || ' ' || s2.last_name as assigned_by_name, " +
                    "sh.name as ship_name " +
                    "FROM tasks t " +
                    "LEFT JOIN staff s1 ON t.assigned_to = s1.id " +
                    "LEFT JOIN staff s2 ON t.assigned_by = s2.id " +
                    "LEFT JOIN ships sh ON t.ship_id = sh.id " +
                    "WHERE t.assigned_to = ? OR t.assigned_by = ? " +
                    "ORDER BY t.created_at DESC";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            pstmt.setInt(2, userId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Map<String, Object> task = new HashMap<>();
                task.put("id", rs.getInt("id"));
                task.put("title", rs.getString("title"));
                task.put("description", rs.getString("description"));
                task.put("status", rs.getString("status"));
                task.put("assigned_to", rs.getObject("assigned_to"));
                task.put("assigned_to_name", rs.getString("assigned_to_name"));
                task.put("assigned_by", rs.getInt("assigned_by"));
                task.put("assigned_by_name", rs.getString("assigned_by_name"));
                task.put("ship_id", rs.getObject("ship_id"));
                task.put("ship_name", rs.getString("ship_name"));
                task.put("due_date", rs.getString("due_date"));
                task.put("created_at", rs.getString("created_at"));
                task.put("updated_at", rs.getString("updated_at"));
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
            
            // Add demo staff
            String[][] staffNames = {
                {"Emily", "Chen"}, {"Kevin", "White"}, {"Ava", "Lee"}, 
                {"Liam", "Hall"}, {"Sophia", "Patel"}, {"Noah", "Kim"},
                {"Mia", "Brown"}, {"Ethan", "Davis"}, {"Isabella", "Miller"},
                {"Lucas", "Wilson"}
            };
            
            String[] staffRoles = {"Manager", "Engineer", "Navigator", "Deck Officer", "Chief Officer"};
            String[] staffEmails = {"emily.chen@example.com", "kevin.white@example.com", "ava.lee@example.com", 
                                    "liam.hall@example.com", "sophia.patel@example.com", "noah.kim@example.com",
                                    "mia.brown@example.com", "ethan.davis@example.com", "isabella.miller@example.com",
                                    "lucas.wilson@example.com"};
            String[] staffPhones = {"123-456-7890", "987-654-3210", "555-123-4567", 
                                    "111-222-3333", "444-555-6666", "777-888-9999",
                                    "999-000-1111", "222-333-4444", "666-777-8888",
                                    "888-999-0000"};
            
            for (int i = 0; i < staffNames.length; i++) {
                try (PreparedStatement pstmt = conn.prepareStatement(
                        "INSERT INTO staff (first_name, last_name, username, password, role, ship_id, email, phone) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
                    pstmt.setString(1, staffNames[i][0]);
                    pstmt.setString(2, staffNames[i][1]);
                    pstmt.setString(3, staffNames[i][0].toLowerCase() + staffNames[i][1].toLowerCase());
                    pstmt.setString(4, BCrypt.hashpw("password123", BCrypt.gensalt()));
                    pstmt.setString(5, staffRoles[i % staffRoles.length]);
                    pstmt.setInt(6, (i % 5) + 1); // Assign to one of the 5 ships
                    pstmt.setString(7, staffEmails[i]);
                    pstmt.setString(8, staffPhones[i]);
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

    /**
     * Get all docks from the database
     * @return List of dock objects
     */
    public static List<Map<String, Object>> getAllDocks() throws SQLException {
        String sql = "SELECT * FROM docks ORDER BY name";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            return resultSetToList(rs);
        }
    }
    
    /**
     * Get a dock by its ID
     * @param dockId The ID of the dock
     * @return The dock object or null if not found
     */
    public static Map<String, Object> getDockById(int dockId) throws SQLException {
        String sql = "SELECT * FROM docks WHERE id = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, dockId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return resultSetToMap(rs);
                } else {
                    return null;
                }
            }
        }
    }
    
    /**
     * Add a new dock to the database
     * @param name The name of the dock
     * @param location The location of the dock
     * @param capacity The capacity of the dock
     * @param status The status of the dock
     * @return The ID of the newly created dock
     */
    public static int addDock(String name, String location, int capacity, String status) throws SQLException {
        String sql = "INSERT INTO docks (name, location, capacity, status) VALUES (?, ?, ?, ?)";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, name);
            pstmt.setString(2, location);
            pstmt.setInt(3, capacity);
            pstmt.setString(4, status);
            
            int affectedRows = pstmt.executeUpdate();
            
            if (affectedRows == 0) {
                throw new SQLException("Creating dock failed, no rows affected.");
            }
            
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                } else {
                    throw new SQLException("Creating dock failed, no ID obtained.");
                }
            }
        }
    }
    
    /**
     * Update an existing dock
     * @param id The ID of the dock to update
     * @param name The name of the dock
     * @param location The location of the dock
     * @param capacity The capacity of the dock
     * @param status The status of the dock
     * @return True if the update was successful, false otherwise
     */
    public static boolean updateDock(int id, String name, String location, int capacity, String status) throws SQLException {
        String sql = "UPDATE docks SET name = ?, location = ?, capacity = ?, status = ? WHERE id = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, name);
            pstmt.setString(2, location);
            pstmt.setInt(3, capacity);
            pstmt.setString(4, status);
            pstmt.setInt(5, id);
            
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        }
    }
    
    /**
     * Delete a dock from the database
     * @param id The ID of the dock to delete
     * @return True if the deletion was successful, false otherwise
     */
    public static boolean deleteDock(int id) throws SQLException {
        String sql = "DELETE FROM docks WHERE id = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        }
    }

    /**
     * Get all bookings from the database
     * @return List of booking objects
     */
    public static List<Map<String, Object>> getAllBookings() {
        List<Map<String, Object>> bookings = new ArrayList<>();
        String sql = "SELECT b.*, s.name as ship_name, u.username as user_username " +
                   "FROM bookings b " +
                   "JOIN ships s ON b.ship_id = s.id " +
                   "JOIN users u ON b.user_id = u.id " +
                   "ORDER BY b.id";
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Map<String, Object> booking = new HashMap<>();
                booking.put("id", rs.getInt("id"));
                booking.put("ship_id", rs.getInt("ship_id"));
                booking.put("ship_name", rs.getString("ship_name"));
                booking.put("user_id", rs.getInt("user_id"));
                booking.put("user_username", rs.getString("user_username"));
                booking.put("start_date", rs.getString("start_date"));
                booking.put("end_date", rs.getString("end_date"));
                booking.put("purpose", rs.getString("purpose"));
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
     * Get a booking by its ID
     * @param bookingId The ID of the booking
     * @return The booking object or null if not found
     */
    public static Map<String, Object> getBookingById(int bookingId) {
        Map<String, Object> booking = new HashMap<>();
        String sql = "SELECT b.*, s.name as ship_name, u.username as user_username " +
                   "FROM bookings b " +
                   "JOIN ships s ON b.ship_id = s.id " +
                   "JOIN users u ON b.user_id = u.id " +
                   "WHERE b.id = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, bookingId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                booking.put("id", rs.getInt("id"));
                booking.put("ship_id", rs.getInt("ship_id"));
                booking.put("ship_name", rs.getString("ship_name"));
                booking.put("user_id", rs.getInt("user_id"));
                booking.put("user_username", rs.getString("user_username"));
                booking.put("start_date", rs.getString("start_date"));
                booking.put("end_date", rs.getString("end_date"));
                booking.put("purpose", rs.getString("purpose"));
                booking.put("status", rs.getString("status"));
                booking.put("created_at", rs.getString("created_at"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return booking;
    }
    
    /**
     * Get bookings for a specific user
     * @param userId The ID of the user
     * @return List of booking objects
     */
    public static List<Map<String, Object>> getUserBookings(int userId) {
        List<Map<String, Object>> bookings = new ArrayList<>();
        String sql = "SELECT b.*, s.name as ship_name, s.type as ship_type " +
                   "FROM bookings b " +
                   "JOIN ships s ON b.ship_id = s.id " +
                   "WHERE b.user_id = ? " +
                   "ORDER BY b.created_at DESC";
        
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
                booking.put("purpose", rs.getString("purpose"));
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
     * Add a new booking to the database
     * @param shipId The ID of the ship
     * @param userId The ID of the user
     * @param startDate The start date of the booking
     * @param endDate The end date of the booking
     * @param purpose The purpose of the booking
     * @return The ID of the newly created booking
     */
    public static int addBooking(int shipId, int userId, String startDate, String endDate, String purpose) {
        String sql = "INSERT INTO bookings (ship_id, user_id, start_date, end_date, purpose, status) VALUES (?, ?, ?, ?, ?, ?)";
        int id = -1;
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setInt(1, shipId);
            pstmt.setInt(2, userId);
            pstmt.setString(3, startDate);
            pstmt.setString(4, endDate);
            pstmt.setString(5, purpose);
            pstmt.setString(6, "Pending"); // Default status for new bookings
            
            int affectedRows = pstmt.executeUpdate();
            
            if (affectedRows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        id = rs.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return id;
    }
    
    /**
     * Update the status of a booking
     * @param bookingId The ID of the booking
     * @param status The new status of the booking
     * @return True if the update was successful, false otherwise
     */
    public static boolean updateBookingStatus(int bookingId, String status) {
        String sql = "UPDATE bookings SET status = ? WHERE id = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, status);
            pstmt.setInt(2, bookingId);
            
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Delete a booking from the database
     * @param bookingId The ID of the booking
     * @return True if the deletion was successful, false otherwise
     */
    public static boolean deleteBooking(int bookingId) {
        String sql = "DELETE FROM bookings WHERE id = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, bookingId);
            
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static List<Map<String, Object>> resultSetToList(ResultSet rs) throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        
        while (rs.next()) {
            Map<String, Object> row = new HashMap<>();
            for (int i = 1; i <= columnCount; i++) {
                row.put(metaData.getColumnName(i), rs.getObject(i));
            }
            list.add(row);
        }
        
        return list;
    }

    private static Map<String, Object> resultSetToMap(ResultSet rs) throws SQLException {
        Map<String, Object> map = new HashMap<>();
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        
        for (int i = 1; i <= columnCount; i++) {
            map.put(metaData.getColumnName(i), rs.getObject(i));
        }
        
        return map;
    }

    /**
     * Get all tasks from the database
     * @return List of task objects
     */
    public static List<Map<String, Object>> getAllTasks() {
        List<Map<String, Object>> tasks = new ArrayList<>();
        String sql = "SELECT t.*, " +
                    "s1.first_name || ' ' || s1.last_name as assigned_to_name, " +
                    "s2.first_name || ' ' || s2.last_name as assigned_by_name, " +
                    "sh.name as ship_name " +
                    "FROM tasks t " +
                    "LEFT JOIN staff s1 ON t.assigned_to = s1.id " +
                    "LEFT JOIN staff s2 ON t.assigned_by = s2.id " +
                    "LEFT JOIN ships sh ON t.ship_id = sh.id " +
                    "ORDER BY t.created_at DESC";
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Map<String, Object> task = new HashMap<>();
                task.put("id", rs.getInt("id"));
                task.put("title", rs.getString("title"));
                task.put("description", rs.getString("description"));
                task.put("status", rs.getString("status"));
                task.put("assigned_to", rs.getObject("assigned_to"));
                task.put("assigned_to_name", rs.getString("assigned_to_name"));
                task.put("assigned_by", rs.getInt("assigned_by"));
                task.put("assigned_by_name", rs.getString("assigned_by_name"));
                task.put("ship_id", rs.getObject("ship_id"));
                task.put("ship_name", rs.getString("ship_name"));
                task.put("due_date", rs.getString("due_date"));
                task.put("created_at", rs.getString("created_at"));
                task.put("updated_at", rs.getString("updated_at"));
                tasks.add(task);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return tasks;
    }
    
    /**
     * Get a task by ID
     * @param taskId The ID of the task
     * @return Task object or null if not found
     */
    public static Map<String, Object> getTaskById(int taskId) {
        Map<String, Object> task = null;
        String sql = "SELECT t.*, " +
                    "s1.first_name || ' ' || s1.last_name as assigned_to_name, " +
                    "s2.first_name || ' ' || s2.last_name as assigned_by_name, " +
                    "sh.name as ship_name " +
                    "FROM tasks t " +
                    "LEFT JOIN staff s1 ON t.assigned_to = s1.id " +
                    "LEFT JOIN staff s2 ON t.assigned_by = s2.id " +
                    "LEFT JOIN ships sh ON t.ship_id = sh.id " +
                    "WHERE t.id = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, taskId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                task = new HashMap<>();
                task.put("id", rs.getInt("id"));
                task.put("title", rs.getString("title"));
                task.put("description", rs.getString("description"));
                task.put("status", rs.getString("status"));
                task.put("assigned_to", rs.getObject("assigned_to"));
                task.put("assigned_to_name", rs.getString("assigned_to_name"));
                task.put("assigned_by", rs.getInt("assigned_by"));
                task.put("assigned_by_name", rs.getString("assigned_by_name"));
                task.put("ship_id", rs.getObject("ship_id"));
                task.put("ship_name", rs.getString("ship_name"));
                task.put("due_date", rs.getString("due_date"));
                task.put("created_at", rs.getString("created_at"));
                task.put("updated_at", rs.getString("updated_at"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return task;
    }
    
    /**
     * Add a new task to the database
     * @param title The title of the task
     * @param description The description of the task
     * @param status The status of the task
     * @param assignedTo The ID of the staff member assigned to the task
     * @param assignedBy The ID of the staff member who assigned the task
     * @param shipId The ID of the ship associated with the task
     * @param dueDate The due date of the task
     * @return The ID of the newly created task
     */
    public static int addTask(String title, String description, String status, Integer assignedTo, int assignedBy, Integer shipId, String dueDate) {
        String sql = "INSERT INTO tasks (title, description, status, assigned_to, assigned_by, ship_id, due_date) VALUES (?, ?, ?, ?, ?, ?, ?)";
        int id = -1;
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, title);
            pstmt.setString(2, description);
            pstmt.setString(3, status);
            
            if (assignedTo != null) {
                pstmt.setInt(4, assignedTo);
            } else {
                pstmt.setNull(4, java.sql.Types.INTEGER);
            }
            
            pstmt.setInt(5, assignedBy);
            
            if (shipId != null) {
                pstmt.setInt(6, shipId);
            } else {
                pstmt.setNull(6, java.sql.Types.INTEGER);
            }
            
            pstmt.setString(7, dueDate);
            
            int affectedRows = pstmt.executeUpdate();
            
            if (affectedRows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        id = rs.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return id;
    }
    
    /**
     * Update an existing task in the database
     * @param taskId The ID of the task
     * @param title The title of the task
     * @param description The description of the task
     * @param status The status of the task
     * @param assignedTo The ID of the staff member assigned to the task
     * @param shipId The ID of the ship associated with the task
     * @param dueDate The due date of the task
     * @return True if the update was successful, false otherwise
     */
    public static boolean updateTask(int taskId, String title, String description, String status, Integer assignedTo, Integer shipId, String dueDate) {
        String sql = "UPDATE tasks SET title = ?, description = ?, status = ?, assigned_to = ?, ship_id = ?, due_date = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, title);
            pstmt.setString(2, description);
            pstmt.setString(3, status);
            
            if (assignedTo != null) {
                pstmt.setInt(4, assignedTo);
            } else {
                pstmt.setNull(4, java.sql.Types.INTEGER);
            }
            
            if (shipId != null) {
                pstmt.setInt(5, shipId);
            } else {
                pstmt.setNull(5, java.sql.Types.INTEGER);
            }
            
            pstmt.setString(6, dueDate);
            pstmt.setInt(7, taskId);
            
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Mark a task as complete
     * @param taskId The ID of the task
     * @return True if the update was successful, false otherwise
     */
    public static boolean completeTask(int taskId) {
        String sql = "UPDATE tasks SET status = 'Completed', updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, taskId);
            
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Delete a task from the database
     * @param taskId The ID of the task
     * @return True if the deletion was successful, false otherwise
     */
    public static boolean deleteTask(int taskId) {
        String sql = "DELETE FROM tasks WHERE id = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, taskId);
            
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Count the number of tasks assigned to a staff member
     * @param staffId The ID of the staff member
     * @return The number of tasks assigned to the staff member
     */
    public static int countStaffTasks(int staffId) {
        String sql = "SELECT COUNT(*) FROM tasks WHERE assigned_to = ?";
        int count = 0;
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, staffId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                count = rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return count;
    }
    
    /**
     * Get all problem reports from the database
     * @return List of problem report objects
     */
    public static List<Map<String, Object>> getAllProblems() {
        List<Map<String, Object>> problems = new ArrayList<>();
        String sql = "SELECT p.*, " +
                    "s.first_name || ' ' || s.last_name as reported_by_name, " +
                    "sh.name as ship_name " +
                    "FROM problem_reports p " +
                    "LEFT JOIN staff s ON p.reported_by = s.id " +
                    "LEFT JOIN ships sh ON p.ship_id = sh.id " +
                    "ORDER BY p.created_at DESC";
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Map<String, Object> problem = new HashMap<>();
                problem.put("id", rs.getInt("id"));
                problem.put("title", rs.getString("title"));
                problem.put("description", rs.getString("description"));
                problem.put("status", rs.getString("status"));
                problem.put("priority", rs.getString("priority"));
                problem.put("reported_by", rs.getInt("reported_by"));
                problem.put("reported_by_name", rs.getString("reported_by_name"));
                problem.put("ship_id", rs.getObject("ship_id"));
                problem.put("ship_name", rs.getString("ship_name"));
                problem.put("created_at", rs.getString("created_at"));
                problem.put("updated_at", rs.getString("updated_at"));
                problems.add(problem);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return problems;
    }
    
    /**
     * Get problem reports for a specific user
     * @param userId The ID of the user
     * @return List of problem report objects
     */
    public static List<Map<String, Object>> getUserProblems(int userId) {
        List<Map<String, Object>> problems = new ArrayList<>();
        String sql = "SELECT p.*, " +
                    "s.first_name || ' ' || s.last_name as reported_by_name, " +
                    "sh.name as ship_name " +
                    "FROM problem_reports p " +
                    "LEFT JOIN staff s ON p.reported_by = s.id " +
                    "LEFT JOIN ships sh ON p.ship_id = sh.id " +
                    "WHERE p.reported_by = ? " +
                    "ORDER BY p.created_at DESC";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Map<String, Object> problem = new HashMap<>();
                problem.put("id", rs.getInt("id"));
                problem.put("title", rs.getString("title"));
                problem.put("description", rs.getString("description"));
                problem.put("status", rs.getString("status"));
                problem.put("priority", rs.getString("priority"));
                problem.put("reported_by", rs.getInt("reported_by"));
                problem.put("reported_by_name", rs.getString("reported_by_name"));
                problem.put("ship_id", rs.getObject("ship_id"));
                problem.put("ship_name", rs.getString("ship_name"));
                problem.put("created_at", rs.getString("created_at"));
                problem.put("updated_at", rs.getString("updated_at"));
                problems.add(problem);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return problems;
    }
}