import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Cargo {
    private String cargoId;
    private String ownerName;
    private List<CargoItem> items;

    public Cargo(String cargoId, String ownerName) {
        this.cargoId = cargoId;
        this.ownerName = ownerName;
        this.items = new ArrayList<>();
        saveToDatabase();
    }

    public Cargo(double cargoCapacity) {
        //TODO Auto-generated constructor stub
    }

    private void saveToDatabase() {
        String sql = "INSERT OR REPLACE INTO cargo (cargo_id, owner_name) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, cargoId);
            pstmt.setString(2, ownerName);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error saving cargo: " + e.getMessage());
        }
    }

    public void addItem(String name, int amount) {
        // Update in-memory list
        for (CargoItem item : items) {
            if (item.getName().equalsIgnoreCase(name)) {
                item.addAmount(amount);
                updateItemInDatabase(item);
                return;
            }
        }
        CargoItem newItem = new CargoItem(name, amount);
        items.add(newItem);
        saveItemToDatabase(newItem);
    }

    private void saveItemToDatabase(CargoItem item) {
        String sql = "INSERT INTO cargo_items (cargo_id, name, amount) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, cargoId);
            pstmt.setString(2, item.getName());
            pstmt.setInt(3, item.getAmount());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error saving cargo item: " + e.getMessage());
        }
    }

    private void updateItemInDatabase(CargoItem item) {
        String sql = "UPDATE cargo_items SET amount = ? WHERE cargo_id = ? AND LOWER(name) = LOWER(?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, item.getAmount());
            pstmt.setString(2, cargoId);
            pstmt.setString(3, item.getName());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating cargo item: " + e.getMessage());
        }
    }

    public boolean removeItem(String name, int amount) {
        for (CargoItem item : items) {
            if (item.getName().equalsIgnoreCase(name)) {
                if (item.getAmount() >= amount) {
                    item.removeAmount(amount);
                    if (item.getAmount() == 0) {
                        items.remove(item);
                        removeItemFromDatabase(name);
                    } else {
                        updateItemInDatabase(item);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    private void removeItemFromDatabase(String name) {
        String sql = "DELETE FROM cargo_items WHERE cargo_id = ? AND LOWER(name) = LOWER(?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, cargoId);
            pstmt.setString(2, name);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error removing cargo item: " + e.getMessage());
        }
    }

    public int getTotalCargo() {
        int total = 0;
        for (CargoItem item : items) {
            total += item.getAmount();
        }
        return total;
    }

    // Add this method to load cargo items from database
    public void loadItemsFromDatabase() {
        String sql = "SELECT name, amount FROM cargo_items WHERE cargo_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, cargoId);
            ResultSet rs = pstmt.executeQuery();
            
            items.clear(); // Clear existing items
            while (rs.next()) {
                String name = rs.getString("name");
                int amount = rs.getInt("amount");
                items.add(new CargoItem(name, amount));
            }
        } catch (SQLException e) {
            System.err.println("Error loading cargo items: " + e.getMessage());
        }
    }

    // Getters
    public String getCargoId() { return cargoId; }
    public String getOwnerName() { return ownerName; }
    public List<CargoItem> getItems() { return new ArrayList<>(items); }
}