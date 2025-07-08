package com.shipmanagement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for the Cargo class with a real database connection
 */
public class CargoIntegrationTest {
    
    private Connection connection;
    private static final String TEST_CARGO_ID = "TEST-INT-001";
    
    @BeforeEach
    void setUp() throws SQLException {
        // Get a real database connection
        connection = DatabaseConnection.getConnection();
        
        // Ensure the cargo table exists FIRST
        ensureCargoTableExists();
        
        // Clean up any existing test data AFTER table exists
        cleanupTestData();
    }
    
    /**
     * Ensures that the cargo table exists in the database
     */
    private void ensureCargoTableExists() throws SQLException {
        String createTableSQL = 
            "CREATE TABLE IF NOT EXISTS cargo (" +
            "cargo_id TEXT PRIMARY KEY, " +
            "owner_name TEXT, " +
            "capacity REAL, " +
            "used_capacity REAL DEFAULT 0)";
        
        try (Statement stmt = connection.createStatement()) {
            System.out.println("Executing cargo table creation SQL: " + createTableSQL);
            stmt.execute(createTableSQL);
            
            // Commit the changes if auto-commit is disabled
            if (!connection.getAutoCommit()) {
                connection.commit();
            }
            
            // Verify the table exists by checking the schema
            try (ResultSet rs = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='cargo'")) {
                if (!rs.next()) {
                    throw new SQLException("Failed to create cargo table");
                } else {
                    System.out.println("Cargo table exists in schema");
                    
                    // Verify the cargo_id column exists
                    try (ResultSet columns = stmt.executeQuery("PRAGMA table_info(cargo)")) {
                        boolean hasCargoIdColumn = false;
                        while (columns.next()) {
                            String columnName = columns.getString("name");
                            if ("cargo_id".equals(columnName)) {
                                hasCargoIdColumn = true;
                                System.out.println("Found cargo_id column in cargo table");
                                break;
                            }
                        }
                        
                        if (!hasCargoIdColumn) {
                            System.out.println("cargo_id column not found in cargo table. Recreating table...");
                            // Drop and recreate the table if cargo_id column is missing
                            stmt.execute("DROP TABLE IF EXISTS cargo");
                            stmt.execute(createTableSQL);
                            if (!connection.getAutoCommit()) {
                                connection.commit();
                            }
                        }
                    }
                }
            }
        }
    }
    
    @AfterEach
    void tearDown() throws SQLException {
        // Clean up test data
        cleanupTestData();
        
        // Close the connection
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
    
    private void cleanupTestData() throws SQLException {
        // Check if the cargo table exists before attempting to delete from it
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='cargo'")) {
            if (rs.next()) {
                // Table exists, safe to delete
                try (PreparedStatement deleteStmt = connection.prepareStatement("DELETE FROM cargo WHERE cargo_id LIKE 'TEST-INT-%'")) {
                    deleteStmt.executeUpdate();
                    
                    // Commit the changes if auto-commit is disabled
                    if (!connection.getAutoCommit()) {
                        connection.commit();
                    }
                }
            }
        }
    }
    
    @Test
    @DisplayName("Test cargo creation and retrieval from database")
    void testCargoCreationAndRetrieval() throws SQLException {
        // Create a cargo object which should save to the database
        Cargo cargo = new Cargo(TEST_CARGO_ID, "Integration Test", 2000.0);
        
        // Query the database to verify the cargo was saved correctly
        try (PreparedStatement stmt = connection.prepareStatement("SELECT * FROM cargo WHERE cargo_id = ?")) {
            stmt.setString(1, TEST_CARGO_ID);
            ResultSet rs = stmt.executeQuery();
            
            assertTrue(rs.next(), "Cargo record should exist in the database");
            assertEquals("Integration Test", rs.getString("owner_name"));
            assertEquals(2000.0, rs.getDouble("capacity"));
            assertEquals(0.0, rs.getDouble("used_capacity"));
            assertFalse(rs.next(), "Should only be one matching record");
        }
    }
    
    @Test
    @DisplayName("Test adding items updates the database")
    void testAddingItemsUpdatesDatabase() throws SQLException {
        // Create a cargo object
        Cargo cargo = new Cargo(TEST_CARGO_ID, "Integration Test", 2000.0);
        
        // Add some items to the cargo
        cargo.addItem("Test Item 1", 5, 10.0);
        cargo.addItem("Test Item 2", 10, 15.0);
        
        // Query the database to verify the used capacity was updated
        try (PreparedStatement stmt = connection.prepareStatement("SELECT used_capacity FROM cargo WHERE cargo_id = ?")) {
            stmt.setString(1, TEST_CARGO_ID);
            ResultSet rs = stmt.executeQuery();
            
            assertTrue(rs.next(), "Cargo record should exist in the database");
            assertEquals(200.0, rs.getDouble("used_capacity")); // 5*10 + 10*15 = 200
        }
    }
    
    @Test
    @DisplayName("Test removing items updates the database")
    void testRemovingItemsUpdatesDatabase() throws SQLException {
        // Create a cargo object
        Cargo cargo = new Cargo(TEST_CARGO_ID, "Integration Test", 2000.0);
        
        // Add an item to the cargo
        cargo.addItem("Test Item", 10, 10.0);
        
        // Verify initial state in database
        try (PreparedStatement stmt = connection.prepareStatement("SELECT used_capacity FROM cargo WHERE cargo_id = ?")) {
            stmt.setString(1, TEST_CARGO_ID);
            ResultSet rs = stmt.executeQuery();
            
            assertTrue(rs.next());
            assertEquals(100.0, rs.getDouble("used_capacity")); // 10*10 = 100
        }
        
        // Remove some of the item
        cargo.removeItem("Test Item", 5, 10.0);
        
        // Verify updated state in database
        try (PreparedStatement stmt = connection.prepareStatement("SELECT used_capacity FROM cargo WHERE cargo_id = ?")) {
            stmt.setString(1, TEST_CARGO_ID);
            ResultSet rs = stmt.executeQuery();
            
            assertTrue(rs.next());
            assertEquals(50.0, rs.getDouble("used_capacity")); // 5*10 = 50
        }
    }
    
    @Test
    @DisplayName("Test multiple cargo objects in database")
    void testMultipleCargoObjects() throws SQLException {
        // Create multiple cargo objects
        Cargo cargo1 = new Cargo("TEST-INT-001", "Owner 1", 1000.0);
        Cargo cargo2 = new Cargo("TEST-INT-002", "Owner 2", 2000.0);
        Cargo cargo3 = new Cargo("TEST-INT-003", "Owner 3", 3000.0);
        
        // Add items to each cargo
        cargo1.addItem("Item 1", 10, 5.0);
        cargo2.addItem("Item 2", 20, 10.0);
        cargo3.addItem("Item 3", 30, 15.0);
        
        // Query the database to verify all cargos were saved correctly
        try (PreparedStatement stmt = connection.prepareStatement("SELECT * FROM cargo WHERE cargo_id LIKE 'TEST-INT-%' ORDER BY cargo_id")) {
            ResultSet rs = stmt.executeQuery();
            
            // Check first cargo
            assertTrue(rs.next());
            assertEquals("TEST-INT-001", rs.getString("cargo_id"));
            assertEquals("Owner 1", rs.getString("owner_name"));
            assertEquals(1000.0, rs.getDouble("capacity"));
            assertEquals(50.0, rs.getDouble("used_capacity"));
            
            // Check second cargo
            assertTrue(rs.next());
            assertEquals("TEST-INT-002", rs.getString("cargo_id"));
            assertEquals("Owner 2", rs.getString("owner_name"));
            assertEquals(2000.0, rs.getDouble("capacity"));
            assertEquals(200.0, rs.getDouble("used_capacity"));
            
            // Check third cargo
            assertTrue(rs.next());
            assertEquals("TEST-INT-003", rs.getString("cargo_id"));
            assertEquals("Owner 3", rs.getString("owner_name"));
            assertEquals(3000.0, rs.getDouble("capacity"));
            assertEquals(450.0, rs.getDouble("used_capacity"));
            
            // No more records
            assertFalse(rs.next());
        }
    }
}
