package com.shipmanagement;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import static spark.Spark.before;
import static spark.Spark.get;
import static spark.Spark.options;
import static spark.Spark.port;
import static spark.Spark.post;
import static spark.Spark.staticFiles;

public class Main {
    // Simple user store with roles
    private static final Map<String, User> USERS = new HashMap<>();
    
    static class User {
        String password;
        String role;
        
        User(String password, String role) {
            this.password = password;
            this.role = role;
        }
    }
    
    static {
        // Initialize with some users
        USERS.put("admin", new User("password123", "admin"));
        USERS.put("user1", new User("user123", "user"));
    }

    public static void main(String[] args) {
        // Initialize database
        DatabaseConnection.getConnection();
        
        // Enable CORS
        enableCORS();
        
        // Serve static files from public directory
        staticFiles.location("/public");
        
        // JSON transformer
        Gson gson = new Gson();
        
        // Login endpoint
        post("/api/login", (req, res) -> {
            try {
                Map<String, String> credentials = gson.fromJson(req.body(), Map.class);
                String username = credentials.get("username");
                String password = credentials.get("password");
                
                DatabaseConnection.User user = DatabaseConnection.authenticateUser(username, password);
                if (user != null) {
                    // Create session
                    req.session(true);
                    req.session().attribute("username", username);
                    req.session().attribute("role", user.getRole());
                    
                    // Create response
                    Map<String, String> response = new HashMap<>();
                    response.put("status", "success");
                    response.put("role", user.getRole());
                    response.put("message", "Login successful");
                    return gson.toJson(response);
                } else {
                    res.status(401);
                    return "{\"status\":\"error\",\"message\":\"Invalid credentials\"}";
                }
            } catch (Exception e) {
                e.printStackTrace();
                res.status(500);
                return "{\"status\":\"error\",\"message\":\"Server error\"}";
            }
        });
        
        // Create report endpoint
        post("/api/reports", (req, res) -> {
            try {
                // Check authentication
                if (req.session().attribute("username") == null) {
                    res.status(401);
                    return "{\"status\":\"error\",\"message\":\"Not authenticated\"}";
                }
                
                Map<String, String> reportData = gson.fromJson(req.body(), Map.class);
                String title = reportData.get("title");
                String content = reportData.get("content");
                String createdBy = req.session().attribute("username");
                
                if (title == null || title.trim().isEmpty() || content == null || content.trim().isEmpty()) {
                    res.status(400);
                    return "{\"status\":\"error\",\"message\":\"Title and content are required\"}";
                }
                
                // Save report to database
                boolean success = DatabaseConnection.addReport(title, content, createdBy);
                
                if (success) {
                    return "{\"status\":\"success\",\"message\":\"Report created successfully\"}";
                } else {
                    res.status(500);
                    return "{\"status\":\"error\",\"message\":\"Failed to create report\"}";
                }
            } catch (Exception e) {
                e.printStackTrace();
                res.status(500);
                return "{\"status\":\"error\",\"message\":\"Server error\"}";
            }
        });
        
        // Add user endpoint (admin only)
        post("/api/users", (req, res) -> {
            try {
                // Check if user is admin
                if (!"admin".equals(req.session().attribute("role"))) {
                    res.status(403);
                    return "{\"status\":\"error\",\"message\":\"Unauthorized\"}";
                }
                
                Map<String, String> userData = gson.fromJson(req.body(), Map.class);
                String username = userData.get("username");
                String password = userData.get("password");
                String role = userData.get("role");
                
                if (username == null || password == null || role == null) {
                    res.status(400);
                    return "{\"status\":\"error\",\"message\":\"Username, password, and role are required\"}";
                }
                
                // Add user to database
                boolean success = DatabaseConnection.addUser(username, password, role);
                
                if (success) {
                    return "{\"status\":\"success\",\"message\":\"User created successfully\"}";
                } else {
                    res.status(400);
                    return "{\"status\":\"error\",\"message\":\"Username already exists\"}";
                }
            } catch (Exception e) {
                e.printStackTrace();
                res.status(500);
                return "{\"status\":\"error\",\"message\":\"Server error\"}";
            }
        });
        
        // Get current user info
        get("/api/me", (req, res) -> {
            String username = req.session().attribute("username");
            if (username == null) {
                res.status(401);
                return "";
            }
            
            Map<String, String> userInfo = new HashMap<>();
            userInfo.put("username", username);
            userInfo.put("role", req.session().attribute("role"));
            return gson.toJson(userInfo);
        });
        
        // Logout endpoint
        post("/api/logout", (req, res) -> {
            req.session().invalidate();
            return "{\"status\":\"success\",\"message\":\"Logged out successfully\"}";
        });
        
        // Crew Management Endpoints
        get("/api/crew", (req, res) -> {
            try {
                List<String> crew = DatabaseConnection.getAllCrewMembers();
                Gson gson = new GsonBuilder().create();
                return gson.toJson(crew);
            } catch (Exception e) {
                res.status(500);
                return "{\"status\":\"error\", \"message\":\"Error fetching crew members\"}";
            }
        });

        post("/api/crew/add", (req, res) -> {
            try {
                String body = req.body();
                Gson gson = new GsonBuilder().create();
                Map<String, String> data = gson.fromJson(body, Map.class);
                
                boolean success = DatabaseConnection.addCrewMember(
                    data.get("name"),
                    data.get("role"),
                    data.get("email"),
                    data.get("phone")
                );
                
                if (success) {
                    return "{\"status\":\"success\", \"message\":\"Crew member added successfully\"}";
                } else {
                    res.status(400);
                    return "{\"status\":\"error\", \"message\":\"Failed to add crew member\"}";
                }
            } catch (Exception e) {
                res.status(500);
                return "{\"status\":\"error\", \"message\":\"Server error\"}";
            }
        });

        // Report Management Endpoints
        get("/api/reports", (req, res) -> {
            try {
                List<String> reports = DatabaseConnection.getAllReports();
                Gson gson = new GsonBuilder().create();
                return gson.toJson(reports);
            } catch (Exception e) {
                res.status(500);
                return "{\"status\":\"error\", \"message\":\"Error fetching reports\"}";
            }
        });

        // Dashboard route - serve the dashboard HTML
        get("/dashboard", (req, res) -> {
            String role = req.queryParams("role");
            if (role == null || (!"admin".equals(role) && !"user".equals(role))) {
                res.redirect("/");
                return null;
            }
            // Serve the dashboard.html file
            res.redirect("/dashboard.html?role=" + role);
            return null;
        });

        // Root redirect to login page
        get("/", (req, res) -> {
            // Check if user is already logged in
            String authHeader = req.headers("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                // In a real app, validate the token and extract user role
                res.redirect("/dashboard?role=user");
            } else {
                res.redirect("/index.html");
            }
            return null;
        });

        // Add a catch-all route for SPA routing
        get("*", (req, res) -> {
            if (req.pathInfo().startsWith("/api/")) {
                res.status(404);
                return "{\"status\":\"error\", \"message\":\"Endpoint not found\"}";
            }
            // For all other routes, serve the main page (handled by the frontend router)
            res.redirect("/");
            return null;
        });

        // Start server
        port(4567);
        System.out.println("Server running on http://localhost:4567");
    }
    
    private static void enableCORS() {
        options("/*", (request, response) -> {
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
        before((request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Request-Method", "*");
            response.type("application/json");
        });
    }
}