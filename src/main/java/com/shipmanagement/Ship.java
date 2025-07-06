           package com.shipmanagement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class Ship implements ShipInterface {   
    protected int id; // Unique identifier for the ship
    protected String type;            
    protected String location;
    protected String destination;

    public Ship(String type, String location, String destination) {
        this.type = type;
        this.location = location;
        this.destination = destination;
    }

    // Getters and setters
    @Override
    public int getId() { return id; }
    
    @Override
    public void setId(int id) { this.id = id; }
    
    @Override
    public String getLocation() { return location; }
    
    @Override
    public void setLocation(String location) { this.location = location; }
    
    @Override
    public String getDestination() { return destination; }
    
    @Override
    public void setDestination(String destination) { this.destination = destination; }
    
    @Override
    public String getType() { return type; }

    // Database methods
    public void save(){
        if (id == 0) {
            // Insert new ship into the database
            String sql = "INSERT INTO ships (type, location, destination) OUTPUT INSERTED.id VALUES (?, ?, ?)";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, type);
                pstmt.setString(2, location);
                pstmt.setString(3, destination);
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        this.id = rs.getInt(1);
                    }
                }
            } catch (SQLException e) {
                System.err.println("Error saving ship: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            // Update existing ship
            String sql = "UPDATE ships SET location = ?, destination = ? WHERE id = ?";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, location);
                pstmt.setString(2, destination);
                pstmt.setInt(3, id);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                System.err.println("Error updating ship: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public static Ship findById(int id) {
        String sql = "SELECT * FROM ships WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String type = rs.getString("type");
                    String location = rs.getString("location");
                    String destination = rs.getString("destination");
                    
                    Ship ship;
                    if ("Cargo".equalsIgnoreCase(type)) {
                        double cargoCapacity = rs.getDouble("cargo_capacity");
                        ship = new CargoShip(type, location, destination, cargoCapacity);
                    } else {
                        int passengerCapacity = rs.getInt("passenger_capacity");
                        ship = new PassengerShip(type, location, passengerCapacity);
                    }
                    ship.setId(id);
                    return ship;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding ship: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public void delete() {
        if (id != 0) {
            String sql = "DELETE FROM ships WHERE id = ?";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, id);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                System.err.println("Error deleting ship: " + e.getMessage());
                e.printStackTrace();
            }
            this.id = 0;
        }
    }
     public String toString() {
        return "CargoShip traveling from " + getLocation() ;
    }
}
