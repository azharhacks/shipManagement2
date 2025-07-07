package com.shipmanagement.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.shipmanagement.DatabaseConnection;
import com.shipmanagement.model.Staff;

public class StaffDAO {
    
    public List<Staff> getAllStaff() {
        List<Staff> staffList = new ArrayList<>();
        String sql = "SELECT * FROM staff";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Staff staff = mapResultSetToStaff(rs);
                staffList.add(staff);
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving all staff: " + e.getMessage());
        }
        
        return staffList;
    }
    
    public Staff getStaffById(int id) {
        String sql = "SELECT * FROM staff WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToStaff(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving staff by ID: " + e.getMessage());
        }
        
        return null;
    }
    
    public int createStaff(Staff staff) {
        String sql = "INSERT INTO staff (first_name, last_name, username, password, role, status, ship_id, email, phone) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, staff.getFirstName());
            pstmt.setString(2, staff.getLastName());
            pstmt.setString(3, staff.getUsername());
            pstmt.setString(4, staff.getPassword());
            pstmt.setString(5, staff.getRole());
            pstmt.setString(6, staff.getStatus());
            if (staff.getShipId() != null) {
                pstmt.setInt(7, staff.getShipId());
            } else {
                pstmt.setNull(7, java.sql.Types.INTEGER);
            }
            pstmt.setString(8, staff.getEmail());
            pstmt.setString(9, staff.getPhone());
            
            int affectedRows = pstmt.executeUpdate();
            
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return generatedKeys.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error creating staff: " + e.getMessage());
        }
        
        return -1;
    }
    
    public boolean updateStaff(Staff staff) {
        String sql = "UPDATE staff SET first_name = ?, last_name = ?, username = ?, role = ?, status = ?, " +
                     "ship_id = ?, email = ?, phone = ? WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, staff.getFirstName());
            pstmt.setString(2, staff.getLastName());
            pstmt.setString(3, staff.getUsername());
            pstmt.setString(4, staff.getRole());
            pstmt.setString(5, staff.getStatus());
            if (staff.getShipId() != null) {
                pstmt.setInt(6, staff.getShipId());
            } else {
                pstmt.setNull(6, java.sql.Types.INTEGER);
            }
            pstmt.setString(7, staff.getEmail());
            pstmt.setString(8, staff.getPhone());
            pstmt.setInt(9, staff.getId());
            
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Error updating staff: " + e.getMessage());
            return false;
        }
    }
    
    public boolean deleteStaff(int id) {
        String sql = "DELETE FROM staff WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting staff: " + e.getMessage());
            return false;
        }
    }
    
    private Staff mapResultSetToStaff(ResultSet rs) throws SQLException {
        Staff staff = new Staff();
        staff.setId(rs.getInt("id"));
        staff.setFirstName(rs.getString("first_name"));
        staff.setLastName(rs.getString("last_name"));
        staff.setUsername(rs.getString("username"));
        staff.setPassword(rs.getString("password"));
        staff.setRole(rs.getString("role"));
        staff.setStatus(rs.getString("status"));
        
        int shipId = rs.getInt("ship_id");
        if (!rs.wasNull()) {
            staff.setShipId(shipId);
        }
        
        staff.setEmail(rs.getString("email"));
        staff.setPhone(rs.getString("phone"));
        
        return staff;
    }
}
