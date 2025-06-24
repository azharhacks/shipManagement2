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
        // Configure server
        port(8080);
        staticFiles.location("/public");
        
        // Enable CORS for development
        before((req, res) -> {
            res.header("Access-Control-Allow-Origin", "*");
            res.header("Access-Control-Request-Method", "*");
            res.header("Access-Control-Allow-Headers", "*");
            res.header("Access-Control-Allow-Credentials", "true");
            res.type("application/json");
        });

        // Handle preflight requests
        options("/*", (req, res) -> {
            return "OK";
        });

        // Login endpoint - handle JSON POST requests
        post("/api/login", (req, res) -> {
            try {
                // Parse JSON body
                String body = req.body();
                String username = null;
                String password = null;
                
                // Simple JSON parsing
                if (body != null && body.contains("{")) {
                    username = body.replaceAll(".*\"username\"\\s*:\\s*\"([^\"]+)\".*", "$1");
                    password = body.replaceAll(".*\"password\"\\s*:\\s*\"([^\"]+)\".*", "$1");
                }
                
                System.out.println("Login attempt - Username: " + username);
                
                User user = USERS.get(username);
                if (user != null && user.password.equals(password)) {
                    System.out.println("Login successful for user: " + username);
                    // Create a simple token (in a real app, use JWT or similar)
                    String token = username + ":" + System.currentTimeMillis();
                    return "{\"status\":\"success\", \"token\":\"" + token + "\", \"role\":\"" + user.role + "\"}";
                } else {
                    System.out.println("Login failed - Invalid credentials for user: " + username);
                    res.status(401);
                    return "{\"status\":\"error\", \"message\":\"Invalid username or password\"}";
                }
            } catch (Exception e) {
                System.err.println("Login error: " + e.getMessage());
                e.printStackTrace();
                res.status(500);
                return "{\"status\":\"error\", \"message\":\"Internal server error\"}";
            }
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

        post("/api/reports/create", (req, res) -> {
            try {
                String body = req.body();
                Gson gson = new GsonBuilder().create();
                Map<String, String> data = gson.fromJson(body, Map.class);
                String username = req.session().attribute("username");
                
                if (username == null) {
                    res.status(401);
                    return "{\"status\":\"error\", \"message\":\"Not authenticated\"}";
                }
                
                boolean success = DatabaseConnection.addReport(
                    data.get("title"),
                    data.get("content"),
                    username
                );
                
                if (success) {
                    return "{\"status\":\"success\", \"message\":\"Report created successfully\"}";
                } else {
                    res.status(400);
                    return "{\"status\":\"error\", \"message\":\"Failed to create report\"}";
                }
            } catch (Exception e) {
                e.printStackTrace();
                res.status(500);
                return "{\"status\":\"error\", \"message\":\"Server error\"}";
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

        System.out.println("Server running at http://localhost:8080");
        System.out.println("Available users:");
        System.out.println("Admin: username=admin, password=password123");
        System.out.println("User:  username=user1, password=user123");
    }
}