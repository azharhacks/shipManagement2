package com.shipmanagement.service;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.shipmanagement.dao.ShipDAO;
import com.shipmanagement.model.Ship;

public class ShipService {
    private final ShipDAO shipDAO;
    private final Gson gson;
    
    public ShipService() {
        this.shipDAO = new ShipDAO();
        this.gson = new Gson();
    }
    
    // Get all ships with pagination
    public Map<String, Object> getAllShips(int page, int pageSize) {
        try {
            List<Ship> ships = shipDAO.getAllShips(page, pageSize);
            int totalRecords = ships.isEmpty() ? 0 : ships.get(0).getTotalRecords();
            int totalPages = (int) Math.ceil((double) totalRecords / pageSize);
            
            Map<String, Object> response = new HashMap<>();
            response.put("data", ships);
            response.put("currentPage", page);
            response.put("pageSize", pageSize);
            response.put("totalRecords", totalRecords);
            response.put("totalPages", totalPages);
            
            return response;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to retrieve ships", e);
        }
    }

    // Get ship by ID
    public Ship getShipById(int id) {
        try {
            return shipDAO.getShipById(id);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to retrieve ship with id: " + id, e);
        }
    }

    // Add a new ship
    public boolean addShip(Ship ship) {
        try {
            validateShip(ship);
            return shipDAO.addShip(ship);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to add ship", e);
        } catch (IllegalArgumentException e) {
            throw e; // Re-throw validation exceptions
        }
    }

    // Update an existing ship
    public boolean updateShip(Ship ship) {
        try {
            validateShip(ship);
            return shipDAO.updateShip(ship);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to update ship", e);
        } catch (IllegalArgumentException e) {
            throw e; // Re-throw validation exceptions
        }
    }

    // Delete a ship
    public boolean deleteShip(int id) {
        try {
            if (id <= 0) {
                throw new IllegalArgumentException("Invalid ship ID");
            }
            return shipDAO.deleteShip(id);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to delete ship", e);
        }
    }
    
    private void validateShip(Ship ship) {
        if (ship.getName() == null || ship.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Ship name is required");
        }
        if (ship.getImoNumber() == null || ship.getImoNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("IMO number is required");
        }
        if (ship.getType() == null || ship.getType().trim().isEmpty()) {
            throw new IllegalArgumentException("Ship type is required");
        }
        if (ship.getCapacity() <= 0) {
            throw new IllegalArgumentException("Capacity must be greater than 0");
        }
    }
}
