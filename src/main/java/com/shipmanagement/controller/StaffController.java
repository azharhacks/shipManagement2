package com.shipmanagement.controller;

import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.shipmanagement.model.Staff;
import com.shipmanagement.service.StaffService;

import static spark.Spark.delete;
import static spark.Spark.get;
import static spark.Spark.post;
import static spark.Spark.put;

public class StaffController {
    
    private final StaffService staffService;
    private final Gson gson;
    
    public StaffController() {
        this.staffService = new StaffService();
        this.gson = new Gson();
        
        setupRoutes();
    }
    
    private void setupRoutes() {
        // Get all staff with optional filters
        get("/api/staff", (req, res) -> {
            try {
                // Extract query parameters for filtering
                String status = req.queryParams("status");
                String role = req.queryParams("role");
                String searchQuery = req.queryParams("search");
                
                Integer shipId = null;
                if (req.queryParams("shipId") != null) {
                    try {
                        shipId = Integer.parseInt(req.queryParams("shipId"));
                    } catch (NumberFormatException e) {
                        res.status(400);
                        return gson.toJson(Map.of("error", "Invalid ship ID format"));
                    }
                }
                
                boolean unassigned = "true".equals(req.queryParams("unassigned"));
                
                List<Staff> staffList = staffService.getFilteredStaff(status, role, shipId, unassigned, searchQuery);
                return gson.toJson(staffList);
            } catch (Exception e) {
                res.status(500);
                return gson.toJson(Map.of("error", "Failed to retrieve staff: " + e.getMessage()));
            }
        });
        
        // Get staff by ID
        get("/api/staff/:id", (req, res) -> {
            try {
                int id = Integer.parseInt(req.params("id"));
                Staff staff = staffService.getStaffById(id);
                
                if (staff == null) {
                    res.status(404);
                    return gson.toJson(Map.of("error", "Staff not found"));
                }
                
                return gson.toJson(staff);
            } catch (NumberFormatException e) {
                res.status(400);
                return gson.toJson(Map.of("error", "Invalid staff ID format"));
            } catch (Exception e) {
                res.status(500);
                return gson.toJson(Map.of("error", "Failed to retrieve staff: " + e.getMessage()));
            }
        });
        
        // Create new staff
        post("/api/staff", (req, res) -> {
            try {
                Staff staff = gson.fromJson(req.body(), Staff.class);
                Staff createdStaff = staffService.createStaff(staff);
                
                if (createdStaff == null) {
                    res.status(500);
                    return gson.toJson(Map.of("error", "Failed to create staff"));
                }
                
                res.status(201);
                return gson.toJson(createdStaff);
            } catch (Exception e) {
                res.status(400);
                return gson.toJson(Map.of("error", "Invalid staff data: " + e.getMessage()));
            }
        });
        
        // Update staff
        put("/api/staff/:id", (req, res) -> {
            try {
                int id = Integer.parseInt(req.params("id"));
                Staff staff = gson.fromJson(req.body(), Staff.class);
                staff.setId(id);
                
                Staff updatedStaff = staffService.updateStaff(staff);
                
                if (updatedStaff == null) {
                    res.status(404);
                    return gson.toJson(Map.of("error", "Staff not found or update failed"));
                }
                
                return gson.toJson(updatedStaff);
            } catch (NumberFormatException e) {
                res.status(400);
                return gson.toJson(Map.of("error", "Invalid staff ID format"));
            } catch (Exception e) {
                res.status(400);
                return gson.toJson(Map.of("error", "Invalid staff data: " + e.getMessage()));
            }
        });
        
        // Delete staff
        delete("/api/staff/:id", (req, res) -> {
            try {
                int id = Integer.parseInt(req.params("id"));
                boolean deleted = staffService.deleteStaff(id);
                
                if (!deleted) {
                    res.status(404);
                    return gson.toJson(Map.of("error", "Staff not found or delete failed"));
                }
                
                return gson.toJson(Map.of("success", true));
            } catch (NumberFormatException e) {
                res.status(400);
                return gson.toJson(Map.of("error", "Invalid staff ID format"));
            } catch (Exception e) {
                res.status(500);
                return gson.toJson(Map.of("error", "Failed to delete staff: " + e.getMessage()));
            }
        });
    }
}
