           //find a way to store the info inputed by the user to the database   

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class Ship {   
    protected int id; // Unique identifier for the ship
    protected String type;            
    protected String location;
    protected String destination;

    public Ship(String type, String location, String destination) {
        this.type = type;
        this.location = location;
        this.destination = destination;
    }

    //getters amd setters public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
    public String getType() { return type; }

    //database methods
    public void save(){
        if (id == 0) {
            // Insert new ship into the database
            String sql = "INSERT INTO ships (type, location, destination) OUTPUT INSERTED.id VALUES (?, ?, ?)";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, type);
                pstmt.setString(2, location);
                pstmt.setString(3, destination);
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        this.id = rs.getInt(1);
                    }
                }
            } catch (SQLException e) {
                System.err.println("Error saving ship: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            // Update existing ship
            String sql = "UPDATE ships SET location = ?, destination = ? WHERE id = ?";
            DatabaseHelper.executeUpdate(sql, location, destination, id);
        }
    }
   public static Ship findById(int id) {
        String sql = "SELECT * FROM ships WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String type = rs.getString("type");
                    String location = rs.getString("location");
                    String destination = rs.getString("destination");
                    
                    Ship ship;
                    if ("Cargo".equalsIgnoreCase(type)) {
                        double cargoCapacity = rs.getDouble("cargo_capacity");
                        ship = new CargoShip(type, location, destination, cargoCapacity);
                    } else {
                        int passengerCapacity = rs.getInt("passenger_capacity");
                        ship = new PassengerShip(type, location, passengerCapacity);
                    }
                    ship.setId(id);
                    return ship;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding ship: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public void delete() {
        if (id != 0) {
            String sql = "DELETE FROM ships WHERE id = ?";
            DatabaseHelper.executeUpdate(sql, id);
            this.id = 0;
        }
    }
}
