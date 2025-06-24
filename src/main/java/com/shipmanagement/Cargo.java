package com.shipmanagement;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import com.shipmanagement.CargoItem;
import com.shipmanagement.DatabaseConnection;

public class Cargo {
    private String cargoId;
    private String ownerName;
    private List<CargoItem> items;
    private double capacity;
    private double usedCapacity;

    public Cargo(String cargoId, String ownerName, double capacity) {
        this.cargoId = cargoId;
        this.ownerName = ownerName;
        this.capacity = capacity;
        this.items = new ArrayList<>();
        this.usedCapacity = 0;
        saveToDatabase();
    }

    public Cargo(double capacity) {
        this("", "", capacity);
    }

    public Cargo() {
        this(0.0);
    }

    private void saveToDatabase() {
        String sql = "INSERT OR REPLACE INTO cargo (cargo_id, owner_name, capacity, used_capacity) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, cargoId);
            pstmt.setString(2, ownerName);
            pstmt.setDouble(3, capacity);
            pstmt.setDouble(4, usedCapacity);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error saving cargo: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void addItem(String name, int amount, double itemWeight) {
        if (usedCapacity + (amount * itemWeight) > capacity) {
            throw new IllegalStateException("Not enough capacity");
        }
        
        for (CargoItem item : items) {
            if (item.getName().equals(name)) {
                item.setAmount(item.getAmount() + amount);
                usedCapacity += amount * itemWeight;
                updateDatabase();
                return;
            }
        }
        
        CargoItem newItem = new CargoItem(name, amount);
        items.add(newItem);
        usedCapacity += amount * itemWeight;
        updateDatabase();
    }
    
    public void removeItem(String name, int amount, double itemWeight) {
        for (CargoItem item : items) {
            if (item.getName().equals(name)) {
                if (item.getAmount() < amount) {
                    throw new IllegalStateException("Not enough items to remove");
                }
                item.setAmount(item.getAmount() - amount);
                usedCapacity -= amount * itemWeight;
                
                if (item.getAmount() == 0) {
                    items.remove(item);
                }
                
                updateDatabase();
                return;
            }
        }
        throw new IllegalArgumentException("Item not found: " + name);
    }
    
    private void updateDatabase() {
        String sql = "UPDATE cargo SET used_capacity = ? WHERE cargo_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, usedCapacity);
            pstmt.setString(2, cargoId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating cargo: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Getters and setters
    public String getCargoId() {
        return cargoId;
    }
    
    public void setCargoId(String cargoId) {
        this.cargoId = cargoId;
    }
    
    public String getOwnerName() {
        return ownerName;
    }
    
    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }
    
    public double getCapacity() {
        return capacity;
    }
    
    public double getUsedCapacity() {
        return usedCapacity;
    }
    
    public double getAvailableCapacity() {
        return capacity - usedCapacity;
    }
    
    public List<CargoItem> getItems() {
        return new ArrayList<>(items);
    }
}
