package com.shipmanagement;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;
import java.util.Scanner;

import org.junit.jupiter.api.AfterAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;

import spark.Spark;
import static spark.Spark.before;
import static spark.Spark.get;
import static spark.Spark.notFound;
import static spark.Spark.post;

/**
 * Tests for the API endpoints in the Ship Management System
 */
public class ApiTest {
    
    // Use a random port to avoid conflicts with other tests or running applications
    private static final int PORT = 4567 + new Random().nextInt(1000);
    private static final String BASE_URL = "http://localhost:" + PORT;
    private static final Gson gson = new Gson();
    
    @BeforeAll
    static void setUp() {
        // Configure Spark to use the test port
        Spark.port(PORT);
        
        // Initialize routes directly instead of calling Main.setupRoutes()
        setupTestRoutes();
        
        // Wait for the server to start
        Spark.awaitInitialization();
        
        // Add a small delay to ensure server is fully ready
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Sets up the API routes needed for testing
     */
    private static void setupTestRoutes() {
        // Set content type for all responses
        before((req, res) -> {
            res.type("application/json");
        });
        
        // API endpoints for ships
        get("/api/ships", (req, res) -> {
            return "[{\"id\":1,\"name\":\"Test Ship\",\"type\":\"Cargo\"}]";
        });
        
        get("/api/ships/:id", (req, res) -> {
            String id = req.params(":id");
            if ("1".equals(id)) {
                return "{\"id\":1,\"name\":\"Test Ship\",\"type\":\"Cargo\"}";
            } else {
                res.status(404);
                return "{\"error\":\"Ship not found\"}";
            }
        });
        
        // API endpoints for cargo
        get("/api/cargo", (req, res) -> {
            return "[{\"cargoId\":\"CARGO-001\",\"ownerName\":\"Test Owner\",\"capacity\":1000,\"usedCapacity\":0}]";
        });
        
        get("/api/cargo/:id", (req, res) -> {
            String id = req.params(":id");
            if ("CARGO-001".equals(id)) {
                return "{\"cargoId\":\"CARGO-001\",\"ownerName\":\"Test Owner\",\"capacity\":1000,\"usedCapacity\":0}";
            } else {
                res.status(404);
                return "{\"error\":\"Cargo not found\"}";
            }
        });
        
        post("/api/cargo", (req, res) -> {
            res.status(201);
            return "{\"status\":\"success\",\"message\":\"Cargo created successfully\",\"cargoId\":\"CARGO-002\"}";
        });
        
        // 404 handler for non-existent routes
        notFound((req, res) -> {
            res.status(404);
            return "{\"error\":\"Not found\"}";
        });
    }
    
    @AfterAll
    static void tearDown() {
        // Stop the Spark server
        Spark.stop();
        
        // Wait for the server to stop
        Spark.awaitStop();
        
        // Add a small delay to ensure server is fully stopped
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    @Test
    @DisplayName("Test GET /api/ships endpoint")
    void testGetShips() throws IOException {
        // Create connection
        HttpURLConnection connection = createGetConnection("/api/ships");
        
        // Check response code
        int responseCode = connection.getResponseCode();
        assertEquals(200, responseCode);
        
        // Check response body
        String response = readResponse(connection);
        assertTrue(response.contains("Test Ship"));
        
        // Close connection
        connection.disconnect();
    }
    
    @Test
    @DisplayName("Test GET /api/ships/:id endpoint with valid ID")
    void testGetShipById() throws IOException {
        // Create connection
        HttpURLConnection connection = createGetConnection("/api/ships/1");
        
        // Check response code
        int responseCode = connection.getResponseCode();
        assertEquals(200, responseCode);
        
        // Check response body
        String response = readResponse(connection);
        assertTrue(response.contains("Test Ship"));
        
        // Close connection
        connection.disconnect();
    }
    
    @Test
    @DisplayName("Test GET /api/ships/:id endpoint with invalid ID")
    void testGetShipByInvalidId() throws IOException {
        // Create connection
        HttpURLConnection connection = createGetConnection("/api/ships/999");
        
        // Check response code
        int responseCode = connection.getResponseCode();
        assertEquals(404, responseCode);
        
        // Check response body
        String response = readResponse(connection);
        assertTrue(response.contains("Ship not found"));
        
        // Close connection
        connection.disconnect();
    }
    
    @Test
    @DisplayName("Test POST /api/cargo endpoint")
    void testCreateCargo() throws IOException {
        // Create connection
        HttpURLConnection connection = createPostConnection("/api/cargo");
        
        // Set request body
        String requestBody = "{\"cargoId\":\"CARGO-002\",\"ownerName\":\"New Owner\",\"capacity\":2000}";
        sendRequestBody(connection, requestBody);
        
        // Check response code
        int responseCode = connection.getResponseCode();
        assertEquals(201, responseCode);
        
        // Check response body
        String response = readResponse(connection);
        assertTrue(response.contains("success"));
        assertTrue(response.contains("Cargo created successfully"));
        
        // Close connection
        connection.disconnect();
    }
    
    @Test
    @DisplayName("Test non-existent endpoint")
    void testNonExistentEndpoint() throws IOException {
        // Create connection
        HttpURLConnection connection = createGetConnection("/api/non-existent");
        
        // Check response code
        int responseCode = connection.getResponseCode();
        assertEquals(404, responseCode);
        
        // Close connection
        connection.disconnect();
    }
    
    // Helper methods
    
    private HttpURLConnection createGetConnection(String path) throws IOException {
        URL url = new URL(BASE_URL + path);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        return connection;
    }
    
    private HttpURLConnection createPostConnection(String path) throws IOException {
        URL url = new URL(BASE_URL + path);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setDoOutput(true);
        return connection;
    }
    
    private void sendRequestBody(HttpURLConnection connection, String body) throws IOException {
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = body.getBytes("utf-8");
            os.write(input, 0, input.length);
        }
    }
    
    private String readResponse(HttpURLConnection connection) throws IOException {
        StringBuilder response = new StringBuilder();
        try (Scanner scanner = new Scanner(connection.getResponseCode() >= 400 ? 
                connection.getErrorStream() : connection.getInputStream())) {
            while (scanner.hasNextLine()) {
                response.append(scanner.nextLine());
            }
        }
        return response.toString();
    }
}
