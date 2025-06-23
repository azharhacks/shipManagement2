package com.shipmanagement;

import com.google.gson.Gson;
import spark.Spark;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Main {
    private static final Gson gson = new Gson();
    
    public static void main(String[] args) {
        // Configure CORS
        Spark.port(4567);
        enableCORS();
        
        // Initialize database
        DatabaseConnection.initializeDatabase();
        
        // Serve static files from src/main/resources/public
        Spark.staticFiles.location("/public");
        
        // API Routes
        
        // Get all ships
        Spark.get("/api/ships", (req, res) -> {
            res.type("application/json");
            // This is a placeholder - you'll need to implement the actual database query
            List<Map<String, Object>> ships = new ArrayList<>();
            return gson.toJson(ships);
        });
        
        // Add a new ship
        Spark.post("/api/ships", (req, res) -> {
            try {
                Map<String, String> shipData = gson.fromJson(req.body(), Map.class);
                // TODO: Create and save ship to database
                res.status(201);
                return gson.toJson(Map.of("status", "success", "message", "Ship added successfully"));
            } catch (Exception e) {
                res.status(400);
                return gson.toJson(Map.of("status", "error", "message", e.getMessage()));
            }
        });
        
        // Update a ship
        Spark.put("/api/ships/:id", (req, res) -> {
            try {
                String id = req.params(":id");
                Map<String, String> shipData = gson.fromJson(req.body(), Map.class);
                // TODO: Update ship in database
                return gson.toJson(Map.of("status", "success", "message", "Ship updated successfully"));
            } catch (Exception e) {
                res.status(400);
                return gson.toJson(Map.of("status", "error", "message", e.getMessage()));
            }
        });
        
        // Delete a ship
        Spark.delete("/api/ships/:id", (req, res) -> {
            try {
                String id = req.params(":id");
                // TODO: Delete ship from database
                return gson.toJson(Map.of("status", "success", "message", "Ship deleted successfully"));
            } catch (Exception e) {
                res.status(400);
                return gson.toJson(Map.of("status", "error", "message", e.getMessage()));
            }
        });
    }
    
    // Enable CORS for all routes
    private static void enableCORS() {
        Spark.options("/*", (request, response) -> {
            String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");
            if (accessControlRequestHeaders != null) {
                response.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
            }
            String accessControlRequestMethod = request.headers("Access-Control-Request-Method");
            if (accessControlRequestMethod != null) {
                response.header("Access-Control-Allow-Methods", accessControlRequestMethod);
            }
            return "OK";
        });
        Spark.before((request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Request-Method", "*");
            response.header("Access-Control-Allow-Headers", "*");
            response.type("application/json");
        });
    }
}
