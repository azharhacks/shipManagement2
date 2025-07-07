package com.shipmanagement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mindrot.jbcrypt.BCrypt;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.shipmanagement.controller.ShipController;
import com.shipmanagement.controller.StaffController;
import com.shipmanagement.dao.StaffDAO;
import com.shipmanagement.model.Staff;
import com.shipmanagement.model.User;

import static spark.Spark.before;
import static spark.Spark.delete;
import static spark.Spark.get;
import static spark.Spark.halt;
import static spark.Spark.options;
import static spark.Spark.port;
import static spark.Spark.post;
import static spark.Spark.put;
import static spark.Spark.staticFiles;

public class Main {
    public static void main(String[] args) {
        try {
            // Initialize database connection
            DatabaseConnection.getConnection();
            
            // Add demo data for testing
            DatabaseConnection.addDemoData();
            
            // Configure Spark
            port(8080);
            staticFiles.location("/public");
            staticFiles.expireTime(600L); // 10 minutes cache for static files
            
            // Enable CORS
            enableCORS();
            
            // Configure session
            sessionConfig();
            
            // Initialize controllers
            new ShipController();
            new StaffController();
            
            // JSON serialization
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            
            // Routes
            before((req, res) -> {
                // Set content type for all responses
                res.type("application/json");
            });

            // Dashboard route
            get("/dashboard", (req, res) -> {
                res.redirect("/dashboard.html");
                return null;
            });

            // Authentication routes
            post("/api/login", (req, res) -> {
                System.out.println("Login attempt received");
                System.out.println("Request body: " + req.body());
                
                try {
                    // Parse request body
                    JsonObject json = JsonParser.parseString(req.body()).getAsJsonObject();
                    String username = json.get("username").getAsString();
                    String password = json.get("password").getAsString();
                    
                    System.out.println("Username: " + username);
                    System.out.println("Attempting to get user from database");
                    
                    // Get user from database
                    User user = DatabaseConnection.getUserByUsername(username);
                    
                    System.out.println("User found: " + (user != null));
                    
                    if (user != null) {
                        System.out.println("User role: " + user.getRole());
                        System.out.println("Verifying password");
                        
                        boolean passwordMatch = BCrypt.checkpw(password, user.getPassword());
                        System.out.println("Password match: " + passwordMatch);
                        
                        if (passwordMatch) {
                            // Create session
                            req.session(true);
                            req.session().attribute("user", user);
                            
                            System.out.println("Session created with ID: " + req.session().id());
                            
                            // Return success response with user role
                            Map<String, Object> response = new java.util.HashMap<>();
                            response.put("status", "success");
                            response.put("message", "Login successful");
                            response.put("role", user.getRole());
                            response.put("redirect", user.getRole().equals("admin") ? "/admin-dashboard.html" : "/user-dashboard.html");
                            
                            String jsonResponse = gson.toJson(response);
                            System.out.println("Response: " + jsonResponse);
                            
                            return jsonResponse;
                        }
                    }
                    
                    // If we get here, either user doesn't exist or password is wrong
                    System.out.println("Authentication failed");
                    res.status(401);
                    return gson.toJson(Map.of(
                        "status", "error",
                        "message", "Invalid username or password"
                    ));
                } catch (JsonSyntaxException e) {
                    System.err.println("JSON parsing error: " + e.getMessage());
                    e.printStackTrace();
                    res.status(400);
                    return gson.toJson(Map.of(
                        "status", "error",
                        "message", "Invalid JSON format"
                    ));
                } catch (Exception e) {
                    System.err.println("Login error: " + e.getMessage());
                    e.printStackTrace();
                    res.status(500);
                    return gson.toJson(Map.of(
                        "status", "error",
                        "message", "An error occurred during login: " + e.getMessage()
                    ));
                }
            });
            
            // Signup endpoint
            post("/api/signup", (req, res) -> {
                System.out.println("Signup attempt received");
                System.out.println("Request body: " + req.body());
                
                try {
                    // Parse request body
                    JsonObject json = JsonParser.parseString(req.body()).getAsJsonObject();
                    String username = json.get("username").getAsString();
                    String password = json.get("password").getAsString();
                    String role = json.has("role") ? json.get("role").getAsString() : "staff";
                    
                    // Validate input
                    if (username == null || username.trim().isEmpty()) {
                        res.status(400);
                        return gson.toJson(Map.of(
                            "status", "error",
                            "message", "Username is required"
                        ));
                    }
                    
                    if (username.length() < 3) {
                        res.status(400);
                        return gson.toJson(Map.of(
                            "status", "error",
                            "message", "Username must be at least 3 characters"
                        ));
                    }
                    
                    if (password == null || password.trim().isEmpty()) {
                        res.status(400);
                        return gson.toJson(Map.of(
                            "status", "error",
                            "message", "Password is required"
                        ));
                    }
                    
                    if (password.length() < 6) {
                        res.status(400);
                        return gson.toJson(Map.of(
                            "status", "error",
                            "message", "Password must be at least 6 characters"
                        ));
                    }
                    
                    // Validate role (only allow admin or staff)
                    if (!"admin".equals(role) && !"staff".equals(role)) {
                        role = "staff"; // Default to staff if invalid role
                    }
                    
                    // Check if username already exists
                    if (DatabaseConnection.usernameExists(username)) {
                        res.status(400);
                        return gson.toJson(Map.of(
                            "status", "error",
                            "message", "Username already exists"
                        ));
                    }
                    
                    // Create user
                    boolean success = DatabaseConnection.createUser(username, password, role);
                    
                    if (success) {
                        return gson.toJson(Map.of(
                            "status", "success",
                            "message", "User registered successfully"
                        ));
                    } else {
                        res.status(500);
                        return gson.toJson(Map.of(
                            "status", "error",
                            "message", "Failed to create user"
                        ));
                    }
                } catch (JsonSyntaxException e) {
                    System.err.println("JSON parsing error: " + e.getMessage());
                    e.printStackTrace();
                    res.status(400);
                    return gson.toJson(Map.of(
                        "status", "error",
                        "message", "Invalid JSON format"
                    ));
                } catch (Exception e) {
                    System.err.println("Signup error: " + e.getMessage());
                    e.printStackTrace();
                    res.status(500);
                    return gson.toJson(Map.of(
                        "status", "error",
                        "message", "An error occurred during signup: " + e.getMessage()
                    ));
                }
            });
            
            // Password change endpoint
            post("/api/change-password", (req, res) -> {
                try {
                    // Check if user is authenticated
                    User currentUser = req.session().attribute("user");
                    if (currentUser == null) {
                        res.status(401);
                        return gson.toJson(Map.of(
                            "status", "error",
                            "message", "You must be logged in to change your password"
                        ));
                    }
                    
                    // Parse request body
                    JsonObject json = new JsonParser().parse(req.body()).getAsJsonObject();
                    String currentPassword = json.get("currentPassword").getAsString();
                    String newPassword = json.get("newPassword").getAsString();
                    
                    // Verify current password
                    if (!DatabaseConnection.verifyUserPassword(currentUser.getUsername(), currentPassword)) {
                        res.status(400);
                        return gson.toJson(Map.of(
                            "status", "error",
                            "message", "Current password is incorrect"
                        ));
                    }
                    
                    // Update password
                    if (DatabaseConnection.updateUserPassword(currentUser.getUsername(), newPassword)) {
                        // Update the user in session with new password
                        currentUser = DatabaseConnection.getUserByUsername(currentUser.getUsername());
                        req.session().attribute("user", currentUser);
                        
                        return gson.toJson(Map.of(
                            "status", "success",
                            "message", "Password updated successfully"
                        ));
                    } else {
                        res.status(500);
                        return gson.toJson(Map.of(
                            "status", "error",
                            "message", "Failed to update password"
                        ));
                    }
                } catch (JsonSyntaxException e) {
                    System.err.println("JSON parsing error: " + e.getMessage());
                    e.printStackTrace();
                    res.status(400);
                    return gson.toJson(Map.of(
                        "status", "error",
                        "message", "Invalid JSON format"
                    ));
                } catch (Exception e) {
                    System.err.println("Password change error: " + e.getMessage());
                    e.printStackTrace();
                    res.status(500);
                    return gson.toJson(Map.of(
                        "status", "error",
                        "message", "An error occurred while changing password"
                    ));
                }
            });
            
            // Authentication check endpoint
            get("/api/check-auth", (req, res) -> {
                try {
                    User user = req.session().attribute("user");
                    
                    // Debug logging
                    System.out.println("Check-auth endpoint called");
                    System.out.println("Session exists: " + (req.session(false) != null));
                    System.out.println("User in session: " + (user != null));
                    if (user != null) {
                        System.out.println("Username: " + user.getUsername());
                        System.out.println("Role: " + user.getRole());
                    }
                    
                    if (user != null) {
                        // Set content type explicitly
                        res.type("application/json");
                        
                        Map<String, Object> response = new java.util.HashMap<>();
                        response.put("authenticated", true);
                        response.put("username", user.getUsername());
                        response.put("role", user.getRole());
                        return gson.toJson(response);
                    } else {
                        // Set content type explicitly
                        res.type("application/json");
                        res.status(401);
                        
                        return gson.toJson(Map.of(
                            "authenticated", false,
                            "message", "Not authenticated"
                        ));
                    }
                } catch (Exception e) {
                    System.err.println("Auth check error: " + e.getMessage());
                    e.printStackTrace();
                    
                    // Set content type explicitly
                    res.type("application/json");
                    res.status(500);
                    
                    return gson.toJson(Map.of(
                        "authenticated", false,
                        "message", "Error checking authentication: " + e.getMessage()
                    ));
                }
            });
            
            // API endpoints for data
            
            // Get all ships
            get("/api/ships", (req, res) -> {
                try {
                    List<Map<String, Object>> ships = DatabaseConnection.getAllShips();
                    res.type("application/json");
                    return gson.toJson(ships);
                } catch (Exception e) {
                    e.printStackTrace();
                    res.status(500);
                    res.type("application/json");
                    return gson.toJson(Map.of("error", "Failed to retrieve ships: " + e.getMessage()));
                }
            });
            
            // Get ship details by ID
            get("/api/ships/:id", (req, res) -> {
                try {
                    int shipId = Integer.parseInt(req.params(":id"));
                    Map<String, Object> ship = DatabaseConnection.getShipById(shipId);
                    
                    if (ship != null) {
                        res.type("application/json");
                        return gson.toJson(ship);
                    } else {
                        res.status(404);
                        res.type("application/json");
                        return gson.toJson(Map.of("error", "Ship not found"));
                    }
                } catch (NumberFormatException e) {
                    res.status(400);
                    res.type("application/json");
                    return gson.toJson(Map.of("error", "Invalid ship ID"));
                } catch (Exception e) {
                    e.printStackTrace();
                    res.status(500);
                    res.type("application/json");
                    return gson.toJson(Map.of("error", "Failed to retrieve ship details: " + e.getMessage()));
                }
            });
            
            // Get user bookings
            get("/api/bookings", (req, res) -> {
                try {
                    User user = req.session().attribute("user");
                    if (user == null) {
                        res.status(401);
                        res.type("application/json");
                        return gson.toJson(Map.of("error", "Not authenticated"));
                    }
                    
                    List<Map<String, Object>> bookings = DatabaseConnection.getUserBookings(user.getId());
                    res.type("application/json");
                    return gson.toJson(bookings);
                } catch (Exception e) {
                    e.printStackTrace();
                    res.status(500);
                    res.type("application/json");
                    return gson.toJson(Map.of("error", "Failed to retrieve bookings: " + e.getMessage()));
                }
            });
            
            // Get user tasks
            get("/api/tasks", (req, res) -> {
                try {
                    User user = req.session().attribute("user");
                    if (user == null) {
                        res.status(401);
                        res.type("application/json");
                        return gson.toJson(Map.of("error", "Not authenticated"));
                    }
                    
                    List<Map<String, Object>> tasks = DatabaseConnection.getUserTasks(user.getId());
                    res.type("application/json");
                    return gson.toJson(tasks);
                } catch (Exception e) {
                    e.printStackTrace();
                    res.status(500);
                    res.type("application/json");
                    return gson.toJson(Map.of("error", "Failed to retrieve tasks: " + e.getMessage()));
                }
            });
            
            // Get all reports (admin only)
            get("/api/reports", (req, res) -> {
                try {
                    User user = req.session().attribute("user");
                    if (user == null) {
                        res.status(401);
                        res.type("application/json");
                        return gson.toJson(Map.of("error", "Not authenticated"));
                    }
                    
                    if (!"admin".equals(user.getRole())) {
                        res.status(403);
                        res.type("application/json");
                        return gson.toJson(Map.of("error", "Access denied"));
                    }
                    
                    List<Map<String, Object>> reports = DatabaseConnection.getAllReports();
                    res.type("application/json");
                    return gson.toJson(reports);
                } catch (Exception e) {
                    e.printStackTrace();
                    res.status(500);
                    res.type("application/json");
                    return gson.toJson(Map.of("error", "Failed to retrieve reports: " + e.getMessage()));
                }
            });
            
            // Get all docks
            get("/api/docks", (req, res) -> {
                try {
                    List<Map<String, Object>> docks = DatabaseConnection.getAllDocks();
                    res.type("application/json");
                    return gson.toJson(docks);
                } catch (Exception e) {
                    e.printStackTrace();
                    res.status(500);
                    res.type("application/json");
                    return gson.toJson(Map.of("error", "Failed to retrieve docks: " + e.getMessage()));
                }
            });
            
            // Get dock details by ID
            get("/api/docks/:id", (req, res) -> {
                try {
                    int dockId = Integer.parseInt(req.params(":id"));
                    Map<String, Object> dock = DatabaseConnection.getDockById(dockId);
                    
                    if (dock != null) {
                        res.type("application/json");
                        return gson.toJson(dock);
                    } else {
                        res.status(404);
                        res.type("application/json");
                        return gson.toJson(Map.of("error", "Dock not found"));
                    }
                } catch (NumberFormatException e) {
                    res.status(400);
                    res.type("application/json");
                    return gson.toJson(Map.of("error", "Invalid dock ID"));
                } catch (Exception e) {
                    e.printStackTrace();
                    res.status(500);
                    res.type("application/json");
                    return gson.toJson(Map.of("error", "Failed to retrieve dock details: " + e.getMessage()));
                }
            });
            
            // Add a new dock
            post("/api/docks", (req, res) -> {
                try {
                    // Parse request body
                    Map<String, Object> requestBody = gson.fromJson(req.body(), Map.class);
                    
                    // Extract dock data
                    String name = (String) requestBody.get("name");
                    String location = (String) requestBody.get("location");
                    int capacity = ((Number) requestBody.get("capacity")).intValue();
                    String status = (String) requestBody.get("status");
                    
                    // Validate required fields
                    if (name == null || name.isEmpty()) {
                        res.status(400);
                        res.type("application/json");
                        return gson.toJson(Map.of("error", "Dock name is required"));
                    }
                    
                    // Add dock to database
                    int dockId = DatabaseConnection.addDock(name, location, capacity, status);
                    
                    // Return success response
                    res.status(201);
                    res.type("application/json");
                    return gson.toJson(Map.of(
                        "id", dockId,
                        "message", "Dock added successfully"
                    ));
                } catch (Exception e) {
                    e.printStackTrace();
                    res.status(500);
                    res.type("application/json");
                    return gson.toJson(Map.of("error", "Failed to add dock: " + e.getMessage()));
                }
            });
            
            // Update an existing dock
            put("/api/docks/:id", (req, res) -> {
                try {
                    int dockId = Integer.parseInt(req.params(":id"));
                    
                    // Parse request body
                    Map<String, Object> requestBody = gson.fromJson(req.body(), Map.class);
                    
                    // Extract dock data
                    String name = (String) requestBody.get("name");
                    String location = (String) requestBody.get("location");
                    int capacity = ((Number) requestBody.get("capacity")).intValue();
                    String status = (String) requestBody.get("status");
                    
                    // Validate required fields
                    if (name == null || name.isEmpty()) {
                        res.status(400);
                        res.type("application/json");
                        return gson.toJson(Map.of("error", "Dock name is required"));
                    }
                    
                    // Check if dock exists
                    Map<String, Object> existingDock = DatabaseConnection.getDockById(dockId);
                    if (existingDock == null) {
                        res.status(404);
                        res.type("application/json");
                        return gson.toJson(Map.of("error", "Dock not found"));
                    }
                    
                    // Update dock in database
                    boolean success = DatabaseConnection.updateDock(dockId, name, location, capacity, status);
                    
                    if (success) {
                        res.type("application/json");
                        return gson.toJson(Map.of("message", "Dock updated successfully"));
                    } else {
                        res.status(500);
                        res.type("application/json");
                        return gson.toJson(Map.of("error", "Failed to update dock"));
                    }
                } catch (NumberFormatException e) {
                    res.status(400);
                    res.type("application/json");
                    return gson.toJson(Map.of("error", "Invalid dock ID"));
                } catch (Exception e) {
                    e.printStackTrace();
                    res.status(500);
                    res.type("application/json");
                    return gson.toJson(Map.of("error", "Failed to update dock: " + e.getMessage()));
                }
            });
            
            // Delete a dock
            delete("/api/docks/:id", (req, res) -> {
                try {
                    int dockId = Integer.parseInt(req.params(":id"));
                    
                    // Check if dock exists
                    Map<String, Object> existingDock = DatabaseConnection.getDockById(dockId);
                    if (existingDock == null) {
                        res.status(404);
                        res.type("application/json");
                        return gson.toJson(Map.of("error", "Dock not found"));
                    }
                    
                    // Delete dock from database
                    boolean success = DatabaseConnection.deleteDock(dockId);
                    
                    if (success) {
                        res.type("application/json");
                        return gson.toJson(Map.of("message", "Dock deleted successfully"));
                    } else {
                        res.status(500);
                        res.type("application/json");
                        return gson.toJson(Map.of("error", "Failed to delete dock"));
                    }
                } catch (NumberFormatException e) {
                    res.status(400);
                    res.type("application/json");
                    return gson.toJson(Map.of("error", "Invalid dock ID"));
                } catch (Exception e) {
                    e.printStackTrace();
                    res.status(500);
                    res.type("application/json");
                    return gson.toJson(Map.of("error", "Failed to delete dock: " + e.getMessage()));
                }
            });
            
            // Get all users (admin only)
            get("/api/users", (req, res) -> {
                try {
                    User user = req.session().attribute("user");
                    if (user == null) {
                        res.status(401);
                        res.type("application/json");
                        return gson.toJson(Map.of("error", "Not authenticated"));
                    }
                    
                    if (!"admin".equals(user.getRole())) {
                        res.status(403);
                        res.type("application/json");
                        return gson.toJson(Map.of("error", "Access denied"));
                    }
                    
                    List<Map<String, Object>> users = DatabaseConnection.getAllUsers();
                    res.type("application/json");
                    return gson.toJson(users);
                } catch (Exception e) {
                    e.printStackTrace();
                    res.status(500);
                    res.type("application/json");
                    return gson.toJson(Map.of("error", "Failed to retrieve users: " + e.getMessage()));
                }
            });
            
            // Logout endpoint
            get("/api/logout", (req, res) -> {
                req.session().removeAttribute("user");
                return gson.toJson(Map.of("status", "success"));
            });
            
            // Check authentication endpoint
            get("/api/check-auth", (req, res) -> {
                User user = req.session().attribute("user");
                if (user == null) {
                    res.status(401);
                    return gson.toJson(Map.of("error", "Not authenticated"));
                }
                
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("id", user.getId());
                userInfo.put("username", user.getUsername());
                userInfo.put("role", user.getRole());
                
                return gson.toJson(userInfo);
            });

            // API endpoints for bookings management
            // Get all bookings (admin only)
            get("/api/bookings", (req, res) -> {
                // Check if user is authenticated and is admin
                if (!isAuthenticated(req) || !isAdmin(req)) {
                    res.status(401);
                    return gson.toJson(Map.of("error", "Unauthorized"));
                }
                
                List<Map<String, Object>> bookings = DatabaseConnection.getAllBookings();
                return gson.toJson(bookings);
            });
            
            // Get current user's bookings
            get("/api/user/bookings", (req, res) -> {
                // Check if user is authenticated
                if (!isAuthenticated(req)) {
                    res.status(401);
                    return gson.toJson(Map.of("error", "Unauthorized"));
                }
                
                int userId = req.session().attribute("user_id");
                List<Map<String, Object>> bookings = DatabaseConnection.getUserBookings(userId);
                return gson.toJson(bookings);
            });
            
            // Get booking by ID
            get("/api/bookings/:id", (req, res) -> {
                // Check if user is authenticated
                if (!isAuthenticated(req)) {
                    res.status(401);
                    return gson.toJson(Map.of("error", "Unauthorized"));
                }
                
                int bookingId = Integer.parseInt(req.params(":id"));
                Map<String, Object> booking = DatabaseConnection.getBookingById(bookingId);
                
                if (booking.isEmpty()) {
                    res.status(404);
                    return gson.toJson(Map.of("error", "Booking not found"));
                }
                
                // Check if the booking belongs to the current user or user is admin
                int userId = req.session().attribute("user_id");
                boolean isAdmin = req.session().attribute("is_admin");
                
                if (!isAdmin && (int)booking.get("user_id") != userId) {
                    res.status(403);
                    return gson.toJson(Map.of("error", "You don't have permission to view this booking"));
                }
                
                return gson.toJson(booking);
            });
            
            // Create new booking
            post("/api/bookings", (req, res) -> {
                // Check if user is authenticated
                if (!isAuthenticated(req)) {
                    res.status(401);
                    return gson.toJson(Map.of("error", "Unauthorized"));
                }
                
                String requestBody = req.body();
                JsonObject jsonObject = JsonParser.parseString(requestBody).getAsJsonObject();
                
                int shipId = jsonObject.get("ship_id").getAsInt();
                String startDate = jsonObject.get("start_date").getAsString();
                String endDate = jsonObject.get("end_date").getAsString();
                String purpose = jsonObject.has("purpose") ? jsonObject.get("purpose").getAsString() : "";
                
                int userId = req.session().attribute("user_id");
                
                // Validate ship exists and is active
                Map<String, Object> ship = DatabaseConnection.getShipById(shipId);
                if (ship.isEmpty()) {
                    res.status(404);
                    return gson.toJson(Map.of("error", "Ship not found"));
                }
                
                if (!"Active".equals(ship.get("status"))) {
                    res.status(400);
                    return gson.toJson(Map.of("error", "Ship is not available for booking"));
                }
                
                // Create booking
                int bookingId = DatabaseConnection.addBooking(shipId, userId, startDate, endDate, purpose);
                
                if (bookingId == -1) {
                    res.status(500);
                    return gson.toJson(Map.of("error", "Failed to create booking"));
                }
                
                res.status(201);
                return gson.toJson(Map.of(
                    "id", bookingId,
                    "message", "Booking created successfully"
                ));
            });
            
            // Update booking status (admin only)
            put("/api/bookings/:id/status", (req, res) -> {
                // Check if user is authenticated and is admin
                if (!isAuthenticated(req) || !isAdmin(req)) {
                    res.status(401);
                    return gson.toJson(Map.of("error", "Unauthorized"));
                }
                
                int bookingId = Integer.parseInt(req.params(":id"));
                String requestBody = req.body();
                JsonObject jsonObject = JsonParser.parseString(requestBody).getAsJsonObject();
                
                String status = jsonObject.get("status").getAsString();
                
                // Validate status
                List<String> validStatuses = Arrays.asList("Pending", "Approved", "Rejected", "Completed", "Cancelled");
                if (!validStatuses.contains(status)) {
                    res.status(400);
                    return gson.toJson(Map.of("error", "Invalid status. Must be one of: " + String.join(", ", validStatuses)));
                }
                
                boolean updated = DatabaseConnection.updateBookingStatus(bookingId, status);
                
                if (!updated) {
                    res.status(404);
                    return gson.toJson(Map.of("error", "Booking not found or update failed"));
                }
                
                return gson.toJson(Map.of("message", "Booking status updated successfully"));
            });
            
            // Cancel booking (user can cancel their own bookings)
            put("/api/bookings/:id/cancel", (req, res) -> {
                // Check if user is authenticated
                if (!isAuthenticated(req)) {
                    res.status(401);
                    return gson.toJson(Map.of("error", "Unauthorized"));
                }
                
                int bookingId = Integer.parseInt(req.params(":id"));
                Map<String, Object> booking = DatabaseConnection.getBookingById(bookingId);
                
                if (booking.isEmpty()) {
                    res.status(404);
                    return gson.toJson(Map.of("error", "Booking not found"));
                }
                
                // Check if the booking belongs to the current user or user is admin
                int userId = req.session().attribute("user_id");
                boolean isAdmin = req.session().attribute("is_admin");
                
                if (!isAdmin && (int)booking.get("user_id") != userId) {
                    res.status(403);
                    return gson.toJson(Map.of("error", "You don't have permission to cancel this booking"));
                }
                
                // Check if booking can be cancelled (not already completed or cancelled)
                String currentStatus = (String)booking.get("status");
                if ("Completed".equals(currentStatus) || "Cancelled".equals(currentStatus)) {
                    res.status(400);
                    return gson.toJson(Map.of("error", "Cannot cancel a booking that is already " + currentStatus));
                }
                
                boolean updated = DatabaseConnection.updateBookingStatus(bookingId, "Cancelled");
                
                if (!updated) {
                    res.status(500);
                    return gson.toJson(Map.of("error", "Failed to cancel booking"));
                }
                
                return gson.toJson(Map.of("message", "Booking cancelled successfully"));
            });
            
            // Delete booking (admin only)
            delete("/api/bookings/:id", (req, res) -> {
                // Check if user is authenticated and is admin
                if (!isAuthenticated(req) || !isAdmin(req)) {
                    res.status(401);
                    return gson.toJson(Map.of("error", "Unauthorized"));
                }
                
                int bookingId = Integer.parseInt(req.params(":id"));
                boolean deleted = DatabaseConnection.deleteBooking(bookingId);
                
                if (!deleted) {
                    res.status(404);
                    return gson.toJson(Map.of("error", "Booking not found or delete failed"));
                }
                
                return gson.toJson(Map.of("message", "Booking deleted successfully"));
            });

            // API endpoints for staff management
            // Get all staff members
            get("/api/staff", (req, res) -> {
                try {
                    User user = req.session().attribute("user");
                    if (user == null) {
                        res.status(401);
                        res.type("application/json");
                        return gson.toJson(Map.of("error", "Not authenticated"));
                    }
                    
                    if (!"admin".equals(user.getRole())) {
                        res.status(403);
                        res.type("application/json");
                        return gson.toJson(Map.of("error", "Access denied"));
                    }
                    
                    // Use the StaffDAO to get all staff members
                    StaffDAO staffDAO = new StaffDAO();
                    List<Staff> staffList = staffDAO.getAllStaff();
                    
                    // Convert to format expected by frontend
                    List<Map<String, Object>> result = new ArrayList<>();
                    for (Staff staff : staffList) {
                        Map<String, Object> staffMap = new HashMap<>();
                        staffMap.put("id", staff.getId());
                        staffMap.put("firstName", staff.getFirstName());
                        staffMap.put("lastName", staff.getLastName());
                        staffMap.put("username", staff.getUsername());
                        staffMap.put("role", staff.getRole());
                        staffMap.put("status", staff.getStatus());
                        staffMap.put("shipId", staff.getShipId());
                        
                        // Get ship name if assigned
                        if (staff.getShipId() != null) {
                            Map<String, Object> ship = DatabaseConnection.getShipById(staff.getShipId());
                            if (ship != null) {
                                staffMap.put("shipName", ship.get("name"));
                            }
                        }
                        
                        staffMap.put("email", staff.getEmail());
                        staffMap.put("phone", staff.getPhone());
                        
                        // Count tasks assigned to this staff member
                        int taskCount = DatabaseConnection.countStaffTasks(staff.getId());
                        staffMap.put("taskCount", taskCount);
                        
                        result.add(staffMap);
                    }
                    
                    res.type("application/json");
                    return gson.toJson(result);
                } catch (Exception e) {
                    e.printStackTrace();
                    res.status(500);
                    res.type("application/json");
                    return gson.toJson(Map.of("error", "Failed to retrieve staff: " + e.getMessage()));
                }
            });
            
            // Get staff member by ID
            get("/api/staff/:id", (req, res) -> {
                try {
                    User user = req.session().attribute("user");
                    if (user == null) {
                        res.status(401);
                        res.type("application/json");
                        return gson.toJson(Map.of("error", "Not authenticated"));
                    }
                    
                    if (!"admin".equals(user.getRole())) {
                        res.status(403);
                        res.type("application/json");
                        return gson.toJson(Map.of("error", "Access denied"));
                    }
                    
                    int staffId = Integer.parseInt(req.params(":id"));
                    
                    // Use the StaffDAO to get the staff member
                    StaffDAO staffDAO = new StaffDAO();
                    Staff staff = staffDAO.getStaffById(staffId);
                    
                    if (staff == null) {
                        res.status(404);
                        res.type("application/json");
                        return gson.toJson(Map.of("error", "Staff not found"));
                    }
                    
                    // Convert to format expected by frontend
                    Map<String, Object> staffMap = new HashMap<>();
                    staffMap.put("id", staff.getId());
                    staffMap.put("firstName", staff.getFirstName());
                    staffMap.put("lastName", staff.getLastName());
                    staffMap.put("username", staff.getUsername());
                    staffMap.put("role", staff.getRole());
                    staffMap.put("status", staff.getStatus());
                    staffMap.put("shipId", staff.getShipId());
                    
                    // Get ship name if assigned
                    if (staff.getShipId() != null) {
                        Map<String, Object> ship = DatabaseConnection.getShipById(staff.getShipId());
                        if (ship != null) {
                            staffMap.put("shipName", ship.get("name"));
                        }
                    }
                    
                    staffMap.put("email", staff.getEmail());
                    staffMap.put("phone", staff.getPhone());
                    
                    res.type("application/json");
                    return gson.toJson(staffMap);
                } catch (NumberFormatException e) {
                    res.status(400);
                    res.type("application/json");
                    return gson.toJson(Map.of("error", "Invalid staff ID"));
                } catch (Exception e) {
                    e.printStackTrace();
                    res.status(500);
                    res.type("application/json");
                    return gson.toJson(Map.of("error", "Failed to retrieve staff: " + e.getMessage()));
                }
            });
            
            // Create new staff member
            post("/api/staff", (req, res) -> {
                try {
                    User user = req.session().attribute("user");
                    if (user == null) {
                        res.status(401);
                        res.type("application/json");
                        return gson.toJson(Map.of("error", "Not authenticated"));
                    }
                    
                    if (!"admin".equals(user.getRole())) {
                        res.status(403);
                        res.type("application/json");
                        return gson.toJson(Map.of("error", "Access denied"));
                    }
                    
                    // Parse request body
                    Map<String, Object> requestBody = gson.fromJson(req.body(), Map.class);
                    
                    // Create staff object
                    Staff staff = new Staff();
                    staff.setFirstName((String) requestBody.get("firstName"));
                    staff.setLastName((String) requestBody.get("lastName"));
                    staff.setUsername((String) requestBody.get("username"));
                    staff.setPassword(BCrypt.hashpw((String) requestBody.get("password"), BCrypt.gensalt()));
                    staff.setRole((String) requestBody.get("role"));
                    staff.setStatus((String) requestBody.get("status"));
                    
                    // Handle ship ID (might be null)
                    if (requestBody.containsKey("shipId") && requestBody.get("shipId") != null) {
                        staff.setShipId(((Number) requestBody.get("shipId")).intValue());
                    }
                    
                    staff.setEmail((String) requestBody.get("email"));
                    staff.setPhone((String) requestBody.get("phone"));
                    
                    // Use the StaffDAO to create the staff member
                    StaffDAO staffDAO = new StaffDAO();
                    int staffId = staffDAO.createStaff(staff);
                    
                    if (staffId == -1) {
                        res.status(500);
                        res.type("application/json");
                        return gson.toJson(Map.of("error", "Failed to create staff member"));
                    }
                    
                    res.status(201);
                    res.type("application/json");
                    return gson.toJson(Map.of(
                        "id", staffId,
                        "message", "Staff member created successfully"
                    ));
                } catch (Exception e) {
                    e.printStackTrace();
                    res.status(500);
                    res.type("application/json");
                    return gson.toJson(Map.of("error", "Failed to create staff member: " + e.getMessage()));
                }
            });
            
            // Update staff member
            put("/api/staff/:id", (req, res) -> {
                try {
                    User user = req.session().attribute("user");
                    if (user == null) {
                        res.status(401);
                        res.type("application/json");
                        return gson.toJson(Map.of("error", "Not authenticated"));
                    }
                    
                    if (!"admin".equals(user.getRole())) {
                        res.status(403);
                        res.type("application/json");
                        return gson.toJson(Map.of("error", "Access denied"));
                    }
                    
                    int staffId = Integer.parseInt(req.params(":id"));
                    
                    // Check if staff exists
                    StaffDAO staffDAO = new StaffDAO();
                    Staff existingStaff = staffDAO.getStaffById(staffId);
                    
                    if (existingStaff == null) {
                        res.status(404);
                        res.type("application/json");
                        return gson.toJson(Map.of("error", "Staff not found"));
                    }
                    
                    // Parse request body
                    Map<String, Object> requestBody = gson.fromJson(req.body(), Map.class);
                    
                    // Update staff object
                    existingStaff.setFirstName((String) requestBody.get("firstName"));
                    existingStaff.setLastName((String) requestBody.get("lastName"));
                    existingStaff.setUsername((String) requestBody.get("username"));
                    existingStaff.setRole((String) requestBody.get("role"));
                    existingStaff.setStatus((String) requestBody.get("status"));
                    
                    // Handle ship ID (might be null)
                    if (requestBody.containsKey("shipId")) {
                        if (requestBody.get("shipId") != null) {
                            existingStaff.setShipId(((Number) requestBody.get("shipId")).intValue());
                        } else {
                            existingStaff.setShipId(null);
                        }
                    }
                    
                    existingStaff.setEmail((String) requestBody.get("email"));
                    existingStaff.setPhone((String) requestBody.get("phone"));
                    
                    // Use the StaffDAO to update the staff member
                    boolean success = staffDAO.updateStaff(existingStaff);
                    
                    if (success) {
                        res.type("application/json");
                        return gson.toJson(Map.of("message", "Staff member updated successfully"));
                    } else {
                        res.status(500);
                        res.type("application/json");
                        return gson.toJson(Map.of("error", "Failed to update staff member"));
                    }
                } catch (NumberFormatException e) {
                    res.status(400);
                    res.type("application/json");
                    return gson.toJson(Map.of("error", "Invalid staff ID"));
                } catch (Exception e) {
                    e.printStackTrace();
                    res.status(500);
                    res.type("application/json");
                    return gson.toJson(Map.of("error", "Failed to update staff member: " + e.getMessage()));
                }
            });
            
            // Delete staff member
            delete("/api/staff/:id", (req, res) -> {
                try {
                    User user = req.session().attribute("user");
                    if (user == null) {
                        res.status(401);
                        res.type("application/json");
                        return gson.toJson(Map.of("error", "Not authenticated"));
                    }
                    
                    if (!"admin".equals(user.getRole())) {
                        res.status(403);
                        res.type("application/json");
                        return gson.toJson(Map.of("error", "Access denied"));
                    }
                    
                    int staffId = Integer.parseInt(req.params(":id"));
                    
                    // Check if staff exists
                    StaffDAO staffDAO = new StaffDAO();
                    Staff existingStaff = staffDAO.getStaffById(staffId);
                    
                    if (existingStaff == null) {
                        res.status(404);
                        res.type("application/json");
                        return gson.toJson(Map.of("error", "Staff not found"));
                    }
                    
                    // Use the StaffDAO to delete the staff member
                    boolean success = staffDAO.deleteStaff(staffId);
                    
                    if (success) {
                        res.type("application/json");
                        return gson.toJson(Map.of("message", "Staff member deleted successfully"));
                    } else {
                        res.status(500);
                        res.type("application/json");
                        return gson.toJson(Map.of("error", "Failed to delete staff member"));
                    }
                } catch (NumberFormatException e) {
                    res.status(400);
                    res.type("application/json");
                    return gson.toJson(Map.of("error", "Invalid staff ID"));
                } catch (Exception e) {
                    e.printStackTrace();
                    res.status(500);
                    res.type("application/json");
                    return gson.toJson(Map.of("error", "Failed to delete staff member: " + e.getMessage()));
                }
            });
            
            // API endpoints for tasks management
            // Get all tasks
            get("/api/tasks", (req, res) -> {
                try {
                    User user = req.session().attribute("user");
                    if (user == null) {
                        res.status(401);
                        res.type("application/json");
                        return gson.toJson(Map.of("error", "Not authenticated"));
                    }
                    
                    List<Map<String, Object>> tasks;
                    if ("admin".equals(user.getRole())) {
                        tasks = DatabaseConnection.getAllTasks();
                    } else {
                        tasks = DatabaseConnection.getUserTasks(user.getId());
                    }
                    
                    res.type("application/json");
                    return gson.toJson(tasks);
                } catch (Exception e) {
                    e.printStackTrace();
                    res.status(500);
                    res.type("application/json");
                    return gson.toJson(Map.of("error", "Failed to retrieve tasks: " + e.getMessage()));
                }
            });
            
            // Get task by ID
            get("/api/tasks/:id", (req, res) -> {
                try {
                    User user = req.session().attribute("user");
                    if (user == null) {
                        res.status(401);
                        res.type("application/json");
                        return gson.toJson(Map.of("error", "Not authenticated"));
                    }
                    
                    int taskId = Integer.parseInt(req.params(":id"));
                    Map<String, Object> task = DatabaseConnection.getTaskById(taskId);
                    
                    if (task == null) {
                        res.status(404);
                        res.type("application/json");
                        return gson.toJson(Map.of("error", "Task not found"));
                    }
                    
                    // Check if user has access to this task
                    if (!"admin".equals(user.getRole()) && 
                        !Integer.valueOf(user.getId()).equals(task.get("assigned_to")) && 
                        !Integer.valueOf(user.getId()).equals(task.get("assigned_by"))) {
                        res.status(403);
                        res.type("application/json");
                        return gson.toJson(Map.of("error", "Access denied"));
                    }
                    
                    res.type("application/json");
                    return gson.toJson(task);
                } catch (NumberFormatException e) {
                    res.status(400);
                    res.type("application/json");
                    return gson.toJson(Map.of("error", "Invalid task ID"));
                } catch (Exception e) {
                    e.printStackTrace();
                    res.status(500);
                    res.type("application/json");
                    return gson.toJson(Map.of("error", "Failed to retrieve task: " + e.getMessage()));
                }
            });
            
            // Create new task
            post("/api/tasks", (req, res) -> {
                try {
                    User user = req.session().attribute("user");
                    if (user == null) {
                        res.status(401);
                        res.type("application/json");
                        return gson.toJson(Map.of("error", "Not authenticated"));
                    }
                    
                    // Parse request body
                    Map<String, Object> requestBody = gson.fromJson(req.body(), Map.class);
                    
                    String title = (String) requestBody.get("title");
                    String description = (String) requestBody.get("description");
                    String status = (String) requestBody.get("status");
                    Integer assignedTo = requestBody.get("assigned_to") != null ? 
                        ((Number) requestBody.get("assigned_to")).intValue() : null;
                    Integer shipId = requestBody.get("ship_id") != null ? 
                        ((Number) requestBody.get("ship_id")).intValue() : null;
                    String dueDate = (String) requestBody.get("due_date");
                    
                    // Add task to database
                    int taskId = DatabaseConnection.addTask(title, description, status, assignedTo, user.getId(), shipId, dueDate);
                    
                    if (taskId == -1) {
                        res.status(500);
                        res.type("application/json");
                        return gson.toJson(Map.of("error", "Failed to create task"));
                    }
                    
                    res.status(201);
                    res.type("application/json");
                    return gson.toJson(Map.of(
                        "id", taskId,
                        "message", "Task created successfully"
                    ));
                } catch (Exception e) {
                    e.printStackTrace();
                    res.status(500);
                    res.type("application/json");
                    return gson.toJson(Map.of("error", "Failed to create task: " + e.getMessage()));
                }
            });
            
            // Update task
            put("/api/tasks/:id", (req, res) -> {
                try {
                    User user = req.session().attribute("user");
                    if (user == null) {
                        res.status(401);
                        res.type("application/json");
                        return gson.toJson(Map.of("error", "Not authenticated"));
                    }
                    
                    int taskId = Integer.parseInt(req.params(":id"));
                    
                    // Check if task exists
                    Map<String, Object> existingTask = DatabaseConnection.getTaskById(taskId);
                    if (existingTask == null) {
                        res.status(404);
                        res.type("application/json");
                        return gson.toJson(Map.of("error", "Task not found"));
                    }
                    
                    // Check if user has permission to update this task
                    if (!"admin".equals(user.getRole()) && 
                        !Integer.valueOf(user.getId()).equals(existingTask.get("assigned_by")) &&
                        !Integer.valueOf(user.getId()).equals(existingTask.get("assigned_to"))) {
                        res.status(403);
                        res.type("application/json");
                        return gson.toJson(Map.of("error", "You don't have permission to update this task"));
                    }
                    
                    // Parse request body
                    Map<String, Object> requestBody = gson.fromJson(req.body(), Map.class);
                    
                    String title = (String) requestBody.get("title");
                    String description = (String) requestBody.get("description");
                    String status = (String) requestBody.get("status");
                    Integer assignedTo = requestBody.get("assigned_to") != null ? 
                        ((Number) requestBody.get("assigned_to")).intValue() : null;
                    Integer shipId = requestBody.get("ship_id") != null ? 
                        ((Number) requestBody.get("ship_id")).intValue() : null;
                    String dueDate = (String) requestBody.get("due_date");
                    
                    // Update task in database
                    boolean success = DatabaseConnection.updateTask(taskId, title, description, status, assignedTo, shipId, dueDate);
                    
                    if (success) {
                        res.type("application/json");
                        return gson.toJson(Map.of("message", "Task updated successfully"));
                    } else {
                        res.status(500);
                        res.type("application/json");
                        return gson.toJson(Map.of("error", "Failed to update task"));
                    }
                } catch (NumberFormatException e) {
                    res.status(400);
                    res.type("application/json");
                    return gson.toJson(Map.of("error", "Invalid task ID"));
                } catch (Exception e) {
                    e.printStackTrace();
                    res.status(500);
                    res.type("application/json");
                    return gson.toJson(Map.of("error", "Failed to update task: " + e.getMessage()));
                }
            });
            
            // Mark task as complete
            put("/api/tasks/:id/complete", (req, res) -> {
                try {
                    User user = req.session().attribute("user");
                    if (user == null) {
                        res.status(401);
                        res.type("application/json");
                        return gson.toJson(Map.of("error", "Not authenticated"));
                    }
                    
                    int taskId = Integer.parseInt(req.params(":id"));
                    
                    // Check if task exists
                    Map<String, Object> existingTask = DatabaseConnection.getTaskById(taskId);
                    if (existingTask == null) {
                        res.status(404);
                        res.type("application/json");
                        return gson.toJson(Map.of("error", "Task not found"));
                    }
                    
                    // Check if user has permission to complete this task
                    if (!"admin".equals(user.getRole()) && 
                        !Integer.valueOf(user.getId()).equals(existingTask.get("assigned_to")) && 
                        !Integer.valueOf(user.getId()).equals(existingTask.get("assigned_by"))) {
                        res.status(403);
                        res.type("application/json");
                        return gson.toJson(Map.of("error", "You don't have permission to complete this task"));
                    }
                    
                    // Complete task in database
                    boolean success = DatabaseConnection.completeTask(taskId);
                    
                    if (success) {
                        res.type("application/json");
                        return gson.toJson(Map.of("message", "Task completed successfully"));
                    } else {
                        res.status(500);
                        res.type("application/json");
                        return gson.toJson(Map.of("error", "Failed to complete task"));
                    }
                } catch (NumberFormatException e) {
                    res.status(400);
                    res.type("application/json");
                    return gson.toJson(Map.of("error", "Invalid task ID"));
                } catch (Exception e) {
                    e.printStackTrace();
                    res.status(500);
                    res.type("application/json");
                    return gson.toJson(Map.of("error", "Failed to complete task: " + e.getMessage()));
                }
            });
            
            // Delete task
            delete("/api/tasks/:id", (req, res) -> {
                try {
                    User user = req.session().attribute("user");
                    if (user == null) {
                        res.status(401);
                        res.type("application/json");
                        return gson.toJson(Map.of("error", "Not authenticated"));
                    }
                    
                    int taskId = Integer.parseInt(req.params(":id"));
                    
                    // Check if task exists
                    Map<String, Object> existingTask = DatabaseConnection.getTaskById(taskId);
                    if (existingTask == null) {
                        res.status(404);
                        res.type("application/json");
                        return gson.toJson(Map.of("error", "Task not found"));
                    }
                    
                    // Only admin or the person who assigned the task can delete it
                    if (!"admin".equals(user.getRole()) && 
                        !Integer.valueOf(user.getId()).equals(existingTask.get("assigned_by"))) {
                        res.status(403);
                        res.type("application/json");
                        return gson.toJson(Map.of("error", "You don't have permission to delete this task"));
                    }
                    
                    // Delete task from database
                    boolean success = DatabaseConnection.deleteTask(taskId);
                    
                    if (success) {
                        res.type("application/json");
                        return gson.toJson(Map.of("message", "Task deleted successfully"));
                    } else {
                        res.status(500);
                        res.type("application/json");
                        return gson.toJson(Map.of("error", "Failed to delete task"));
                    }
                } catch (NumberFormatException e) {
                    res.status(400);
                    res.type("application/json");
                    return gson.toJson(Map.of("error", "Invalid task ID"));
                } catch (Exception e) {
                    e.printStackTrace();
                    res.status(500);
                    res.type("application/json");
                    return gson.toJson(Map.of("error", "Failed to delete task: " + e.getMessage()));
                }
            });
            
            // API endpoints for problem reports
            get("/api/problems", (req, res) -> {
                try {
                    User user = req.session().attribute("user");
                    if (user == null) {
                        res.status(401);
                        res.type("application/json");
                        return gson.toJson(Map.of("error", "Not authenticated"));
                    }
                    
                    List<Map<String, Object>> problems;
                    if ("admin".equals(user.getRole())) {
                        problems = DatabaseConnection.getAllProblems();
                    } else {
                        problems = DatabaseConnection.getUserProblems(user.getId());
                    }
                    
                    res.type("application/json");
                    return gson.toJson(problems);
                } catch (Exception e) {
                    e.printStackTrace();
                    res.status(500);
                    res.type("application/json");
                    return gson.toJson(Map.of("error", "Failed to retrieve problems: " + e.getMessage()));
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to start server: " + e.getMessage());
        }
    }

    /**
     * Configure session settings for the application
     */
    private static void sessionConfig() {
        // Set session timeout to 30 minutes
        int maxAge = 30 * 60; // 30 minutes in seconds
        
        // Configure session settings
        staticFiles.expireTime(maxAge);
        
        // Set up session handling
        before((req, res) -> {
            if (req.session(true).isNew()) {
                req.session().maxInactiveInterval(maxAge);
            }
        });
        
        // Add before filter to check authentication for protected routes
        before("/admin-dashboard.html", (req, res) -> {
            User user = req.session().attribute("user");
            if (user == null) {
                res.redirect("/login.html");
                halt();
            } else if (!"admin".equals(user.getRole())) {
                res.redirect("/user-dashboard.html");
                halt();
            }
        });
        
        before("/user-dashboard.html", (req, res) -> {
            User user = req.session().attribute("user");
            if (user == null) {
                res.redirect("/login.html");
                halt();
            }
        });
    }
    
    /**
     * Enable CORS for the application
     */
    private static void enableCORS() {
        options("/*", (request, response) -> {
            String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");
            if (accessControlRequestHeaders != null) {
                response.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
            }

            String accessControlRequestMethod = request.headers("Access-Control-Request-Method");
            if (accessControlRequestMethod != null) {
                response.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            }

            return "OK";
        });

        before((request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            response.header("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With");
            response.header("Access-Control-Allow-Credentials", "true");
            
            // Handle preflight requests
            if (request.requestMethod().equals("OPTIONS")) {
                response.status(200);
                halt(200);
            }
        });
    }

    private static String successResponse(String message, Gson gson) {
        return gson.toJson(Map.of(
            "status", "success",
            "message", message
        ));
    }

    private static String errorResponse(String message, Gson gson) {
        return gson.toJson(Map.of(
            "status", "error",
            "message", message
        ));
    }
    
    private static boolean isAuthenticated(spark.Request req) {
        return req.session().attribute("user") != null;
    }
    
    private static boolean isAdmin(spark.Request req) {
        User user = req.session().attribute("user");
        return user != null && "admin".equals(user.getRole());
    }
}