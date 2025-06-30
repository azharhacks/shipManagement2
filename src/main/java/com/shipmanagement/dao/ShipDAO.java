package com.shipmanagement.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.shipmanagement.DatabaseConnection;
import com.shipmanagement.model.Ship;

public class ShipDAO {
    
    // Get all ships with pagination
    public List<Ship> getAllShips(int page, int pageSize) throws SQLException {
        List<Ship> ships = new ArrayList<>();
        String sql = "SELECT * FROM ships ORDER BY id DESC LIMIT ? OFFSET ?";
        int offset = (page - 1) * pageSize;
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, pageSize);
            pstmt.setInt(2, offset);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    ships.add(extractShipFromResultSet(rs));
                }
            }
        }
        
        // Set pagination info if we have results
        if (!ships.isEmpty()) {
            int totalRecords = getTotalShipCount();
            ships.get(0).setTotalRecords(totalRecords);
            ships.get(0).setCurrentPage(page);
            ships.get(0).setPageSize(pageSize);
        }
        
        return ships;
    }
    
    // Get ship by ID
    public Ship getShipById(int id) throws SQLException {
        String sql = "SELECT * FROM ships WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return extractShipFromResultSet(rs);
                }
            }
        }
        return null;
    }
    
    // Add a new ship
    public boolean addShip(Ship ship) throws SQLException {
        String sql = "INSERT INTO ships (name, imo_number, type, status, capacity, current_location, last_maintenance_date) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, ship.getName());
            pstmt.setString(2, ship.getImoNumber());
            pstmt.setString(3, ship.getType());
            pstmt.setString(4, ship.getStatus());
            pstmt.setInt(5, ship.getCapacity());
            pstmt.setString(6, ship.getCurrentLocation());
            pstmt.setString(7, ship.getLastMaintenanceDate() != null ? ship.getLastMaintenanceDate().toString() : null);
            
            int affectedRows = pstmt.executeUpdate();
            
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        ship.setId(generatedKeys.getInt(1));
                        return true;
                    }
                }
            }
            return false;
        }
    }
    
    // Update an existing ship
    public boolean updateShip(Ship ship) throws SQLException {
        String sql = "UPDATE ships SET name = ?, imo_number = ?, type = ?, status = ?, " +
                    "capacity = ?, current_location = ?, last_maintenance_date = ? WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, ship.getName());
            pstmt.setString(2, ship.getImoNumber());
            pstmt.setString(3, ship.getType());
            pstmt.setString(4, ship.getStatus());
            pstmt.setInt(5, ship.getCapacity());
            pstmt.setString(6, ship.getCurrentLocation());
            pstmt.setString(7, ship.getLastMaintenanceDate() != null ? ship.getLastMaintenanceDate().toString() : null);
            pstmt.setInt(8, ship.getId());
            
            return pstmt.executeUpdate() > 0;
        }
    }
    
    // Delete a ship
    public boolean deleteShip(int id) throws SQLException {
        String sql = "DELETE FROM ships WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }
    
    // Get total ship count
    private int getTotalShipCount() throws SQLException {
        String sql = "SELECT COUNT(*) as count FROM ships";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getInt("count") : 0;
        }
    }
    
    // Helper method to extract Ship object from ResultSet
    private Ship extractShipFromResultSet(ResultSet rs) throws SQLException {
        Ship ship = new Ship();
        ship.setId(rs.getInt("id"));
        ship.setName(rs.getString("name"));
        ship.setImoNumber(rs.getString("imo_number"));
        ship.setType(rs.getString("type"));
        ship.setStatus(rs.getString("status"));
        ship.setCapacity(rs.getInt("capacity"));
        ship.setCurrentLocation(rs.getString("current_location"));
        
        Date maintenanceDate = rs.getDate("last_maintenance_date");
        if (maintenanceDate != null) {
            ship.setLastMaintenanceDate(maintenanceDate.toLocalDate());
        }
        
        ship.setCreatedAt(rs.getString("created_at"));
        return ship;
    }
}
