package com.shipmanagement;

import static spark.Spark.*;
import java.util.HashMap;
import java.util.Map;

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
        });

        // Login endpoint - handle both GET and POST for testing
        post("/api/login", (req, res) -> {
            String username = req.queryParams("username");
            String password = req.queryParams("password");
            
            // For debugging
            System.out.println("Login attempt - Username: " + username + ", Password: " + password);
            
            User user = USERS.get(username);
            if (user != null && user.password.equals(password)) {
                System.out.println("Login successful for user: " + username);
                // Return JSON response instead of redirect
                res.type("application/json");
                return "{\"status\":\"success\", \"role\":\"" + user.role + "\"}";
            } else {
                System.out.println("Login failed - Invalid credentials for user: " + username);
                res.status(401);
                res.type("application/json");
                return "{\"status\":\"error\", \"message\":\"Invalid username or password\"}";
            }
        });

        // Dashboard route
        // Update the dashboard route to serve the static file
        get("/dashboard", (req, res) -> {
            String role = req.queryParams("role");
            if (role == null || (!"admin".equals(role) && !"user".equals(role))) {
                res.redirect("/");
                return null;
            }
            // Serve the dashboard HTML file
            res.redirect("dashboard.html?role=" + role);
            return null;
        });

        // Add a route to serve dashboard.html directly
        get("/dashboard.html", (req, res) -> {
            String role = req.queryParams("role");
            if (role == null || (!"admin".equals(role) && !"user".equals(role))) {
                res.redirect("/");
                return null;
            }
            // Let the static file handler serve the file
            return null;
        });

        // Root redirect to login page
        get("/", (req, res) -> {
            res.redirect("/index.html");
            return null;
        });

        // Handle 404 - Page not found
        notFound((req, res) -> {
            res.redirect("/");
            return null;
        });

        System.out.println("Server running at http://localhost:8080");
        System.out.println("Available users:");
        System.out.println("Admin: username=admin, password=password123");
        System.out.println("User:  username=user1, password=user123");
    }
}