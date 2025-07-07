package com.shipmanagement.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.shipmanagement.dao.ShipDAO;
import com.shipmanagement.dao.StaffDAO;
import com.shipmanagement.model.Ship;
import com.shipmanagement.model.Staff;

public class StaffService {
    
    private final StaffDAO staffDAO;
    private final ShipDAO shipDAO;
    
    public StaffService() {
        this.staffDAO = new StaffDAO();
        this.shipDAO = new ShipDAO();
    }
    
    public List<Staff> getAllStaff() {
        List<Staff> staffList = staffDAO.getAllStaff();
        return enrichStaffData(staffList);
    }
    
    public Staff getStaffById(int id) {
        Staff staff = staffDAO.getStaffById(id);
        if (staff != null && staff.getShipId() != null) {
            try {
                Ship ship = shipDAO.getShipById(staff.getShipId());
                if (ship != null) {
                    staff.setShipName(ship.getName());
                }
            } catch (SQLException e) {
                System.err.println("Error retrieving ship for staff: " + e.getMessage());
            }
        }
        return staff;
    }
    
    public List<Staff> getFilteredStaff(String status, String role, Integer shipId, boolean unassigned, String searchQuery) {
        List<Staff> allStaff = staffDAO.getAllStaff();
        
        // Apply filters
        List<Staff> filteredStaff = allStaff.stream()
            .filter(staff -> status == null || staff.getStatus().equals(status))
            .filter(staff -> role == null || staff.getRole().equals(role))
            .filter(staff -> {
                if (unassigned) {
                    return staff.getShipId() == null;
                } else if (shipId != null) {
                    return shipId.equals(staff.getShipId());
                }
                return true;
            })
            .filter(staff -> {
                if (searchQuery == null || searchQuery.isEmpty()) {
                    return true;
                }
                String query = searchQuery.toLowerCase();
                return staff.getFirstName().toLowerCase().contains(query) || 
                       staff.getLastName().toLowerCase().contains(query) || 
                       staff.getUsername().toLowerCase().contains(query);
            })
            .collect(Collectors.toList());
        
        return enrichStaffData(filteredStaff);
    }
    
    public Staff createStaff(Staff staff) {
        int id = staffDAO.createStaff(staff);
        return getStaffById(id);
    }
    
    public Staff updateStaff(Staff staff) {
        boolean updated = staffDAO.updateStaff(staff);
        if (updated) {
            return getStaffById(staff.getId());
        }
        return null;
    }
    
    public boolean deleteStaff(int id) {
        return staffDAO.deleteStaff(id);
    }
    
    private List<Staff> enrichStaffData(List<Staff> staffList) {
        if (staffList == null || staffList.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Get all ships to avoid multiple database calls
        List<Ship> ships = new ArrayList<>();
        try {
            ships = shipDAO.getAllShips(1, 1000); // Get all ships with pagination
        } catch (SQLException e) {
            System.err.println("Error retrieving ships for staff enrichment: " + e.getMessage());
        }
        
        Map<Integer, String> shipMap = ships.stream()
            .collect(Collectors.toMap(Ship::getId, Ship::getName));
        
        // Enrich staff with ship names
        for (Staff staff : staffList) {
            if (staff.getShipId() != null) {
                staff.setShipName(shipMap.getOrDefault(staff.getShipId(), "Unknown Ship"));
            }
            
            // TODO: Add task count from TaskDAO when implemented
            staff.setTaskCount(0);
        }
        
        return staffList;
    }
}
