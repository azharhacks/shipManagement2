package com.shipmanagement;


import java.util.List;
import java.util.Map;

import org.mindrot.jbcrypt.BCrypt;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.shipmanagement.model.User;

import static spark.Spark.before;
import static spark.Spark.get;
import static spark.Spark.halt;
import static spark.Spark.options;
import static spark.Spark.port;
import static spark.Spark.post;
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
            post("/api/logout", (req, res) -> {
                req.session().removeAttribute("user");
                req.session().invalidate();
                return gson.toJson(Map.of(
                    "status", "success",
                    "message", "Logged out successfully"
                ));
            });

            // ... rest of your existing routes ...

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
}