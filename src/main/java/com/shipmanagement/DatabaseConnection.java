package com.shipmanagement;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DatabaseConnection {
    private static final String DB_URL = "jdbc:sqlite:ships.db";
    private static Connection connection = null;

    public static Connection getConnection() {
        if (connection == null) {
            try {
                connection = DriverManager.getConnection(DB_URL);
                initializeDatabase();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return connection;
    }

    private static void initializeDatabase() {
        try (Statement stmt = getConnection().createStatement()) {
            // Create tables if they don't exist
            stmt.execute("CREATE TABLE IF NOT EXISTS crew_members (\n" +
                "    id INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                "    name TEXT NOT NULL,\n" +
                "    role TEXT NOT NULL,\n" +
                "    email TEXT UNIQUE,\n" +
                "    phone TEXT,\n" +
                "    hire_date TEXT,\n" +
                "    status TEXT DEFAULT 'active'\n" +
                ")");

            stmt.execute("CREATE TABLE IF NOT EXISTS reports (\n" +
                "    id INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                "    title TEXT NOT NULL,\n" +
                "    content TEXT NOT NULL,\n" +
                "    created_by TEXT NOT NULL,\n" +
                "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\n" +
                "    status TEXT DEFAULT 'pending',\n" +
                "    priority TEXT DEFAULT 'medium',\n" +
                "    assigned_to TEXT\n" +
                ")");

            stmt.execute("CREATE TABLE IF NOT EXISTS ships (\n" +
                "    id INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                "    type TEXT NOT NULL,\n" +
                "    location TEXT,\n" +
                "    destination TEXT,\n" +
                "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP\n" +
                ")");

            stmt.execute("CREATE TABLE IF NOT EXISTS cargo (\n" +
                "    cargo_id TEXT PRIMARY KEY,\n" +
                "    owner_name TEXT NOT NULL,\n" +
                "    capacity REAL NOT NULL,\n" +
                "    used_capacity REAL NOT NULL DEFAULT 0,\n" +
                "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP\n" +
                ")");

            stmt.execute("CREATE TABLE IF NOT EXISTS cargo_items (\n" +
                "    id INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                "    cargo_id TEXT NOT NULL,\n" +
                "    name TEXT NOT NULL,\n" +
                "    amount INTEGER NOT NULL,\n" +
                "    item_weight REAL NOT NULL,\n" +
                "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\n" +
                "    FOREIGN KEY (cargo_id) REFERENCES cargo(cargo_id) ON DELETE CASCADE,\n" +
                "    UNIQUE(cargo_id, name)\n" +
                ")");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Add methods for CRUD operations
    public static boolean addCrewMember(String name, String role, String email, String phone) {
        String sql = "INSERT INTO crew_members (name, role, email, phone) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, role);
            pstmt.setString(3, email);
            pstmt.setString(4, phone);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean addReport(String title, String content, String createdBy) {
        String sql = "INSERT INTO reports (title, content, created_by) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, title);
            pstmt.setString(2, content);
            pstmt.setString(3, createdBy);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static List<String> getAllCrewMembers() {
        List<String> crew = new ArrayList<>();
        String sql = "SELECT id, name, role FROM crew_members WHERE status = 'active'";
        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                crew.add(String.format("%d. %s - %s", 
                    rs.getInt("id"), 
                    rs.getString("name"), 
                    rs.getString("role")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return crew;
    }

    public static List<String> getAllReports() {
        List<String> reports = new ArrayList<>();
        String sql = "SELECT id, title, created_by, created_at, status FROM reports ORDER BY created_at DESC";
        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                reports.add(String.format("ID: %d | %s | By: %s | %s | Status: %s", 
                    rs.getInt("id"),
                    rs.getString("title"),
                    rs.getString("created_by"),
                    rs.getString("created_at"),
                    rs.getString("status")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return reports;
    }

    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database connection closed");
            }
        } catch (SQLException e) {
            System.err.println("Error closing connection: " + e.getMessage());
            e.printStackTrace();
        }
    }
}