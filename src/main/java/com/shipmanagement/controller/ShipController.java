package com.shipmanagement.controller;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.shipmanagement.model.Ship;
import com.shipmanagement.service.ShipService;
import com.shipmanagement.util.AuthUtil;

import spark.Request;
import spark.Response;

public class ShipController {
    private final ShipService shipService;
    private final Gson gson;
    
    public ShipController() {
        this.shipService = new ShipService();
        this.gson = new Gson();
    }
    
    // Get all ships
    public String getAllShips(Request req, Response res) {
        try {
            AuthUtil.requireAuth(req, res);
            
            int page = 1;
            int pageSize = 6; // Default page size
            
            try {
                if (req.queryParams("page") != null) {
                    page = Integer.parseInt(req.queryParams("page"));
                }
                if (req.queryParams("pageSize") != null) {
                    pageSize = Integer.parseInt(req.queryParams("pageSize"));
                }
            } catch (NumberFormatException e) {
                // Use defaults if invalid values provided
            }
            
            return gson.toJson(shipService.getAllShips(page, pageSize));
        } catch (Exception e) {
            res.status(500);
            return errorResponse("Failed to retrieve ships: " + e.getMessage());
        }
    }

    // Get ship by ID
    public String getShipById(Request req, Response res) {
        try {
            AuthUtil.requireAuth(req, res);
            
            int id = Integer.parseInt(req.params(":id"));
            Ship ship = shipService.getShipById(id);
            
            if (ship != null) {
                return gson.toJson(ship);
            } else {
                res.status(404);
                return errorResponse("Ship not found with id: " + id);
            }
        } catch (NumberFormatException e) {
            res.status(400);
            return errorResponse("Invalid ship ID format");
        } catch (Exception e) {
            res.status(500);
            return errorResponse("Failed to retrieve ship: " + e.getMessage());
        }
    }

    // Create a new ship
    public String createShip(Request req, Response res) {
        try {
            AuthUtil.requireAdmin(req, res);
            
            Ship ship = gson.fromJson(req.body(), Ship.class);
            boolean success = shipService.addShip(ship);
            
            if (success) {
                res.status(201);
                return successResponse("Ship added successfully");
            } else {
                res.status(400);
                return errorResponse("Failed to add ship");
            }
        } catch (IllegalArgumentException e) {
            res.status(400);
            return errorResponse(e.getMessage());
        } catch (Exception e) {
            res.status(500);
            return errorResponse("Failed to add ship: " + e.getMessage());
        }
    }

    // Update an existing ship
    public String updateShip(Request req, Response res) {
        try {
            AuthUtil.requireAdmin(req, res);
            
            int id = Integer.parseInt(req.params(":id"));
            Ship ship = gson.fromJson(req.body(), Ship.class);
            ship.setId(id);
            
            boolean success = shipService.updateShip(ship);
            
            if (success) {
                return successResponse("Ship updated successfully");
            } else {
                res.status(404);
                return errorResponse("Ship not found with id: " + id);
            }
        } catch (NumberFormatException e) {
            res.status(400);
            return errorResponse("Invalid ship ID format");
        } catch (IllegalArgumentException e) {
            res.status(400);
            return errorResponse(e.getMessage());
        } catch (Exception e) {
            res.status(500);
            return errorResponse("Failed to update ship: " + e.getMessage());
        }
    }

    // Delete a ship
    public String deleteShip(Request req, Response res) {
        try {
            AuthUtil.requireAdmin(req, res);
            
            int id = Integer.parseInt(req.params(":id"));
            boolean success = shipService.deleteShip(id);
            
            if (success) {
                return successResponse("Ship deleted successfully");
            } else {
                res.status(404);
                return errorResponse("Ship not found with id: " + id);
            }
        } catch (NumberFormatException e) {
            res.status(400);
            return errorResponse("Invalid ship ID format");
        } catch (Exception e) {
            res.status(500);
            return errorResponse("Failed to delete ship: " + e.getMessage());
        }
    }

    // Helper method to create success responses
    private String successResponse(String message) {
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", message);
        return gson.toJson(response);
    }

    // Helper method to create error responses
    private String errorResponse(String message) {
        Map<String, String> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", message);
        return gson.toJson(response);
    }
}
