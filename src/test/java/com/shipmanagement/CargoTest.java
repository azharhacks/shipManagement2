package com.shipmanagement;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class CargoTest {
    
    private Cargo cargo;
    
    @BeforeEach
    void setUp() {
        // Create a new cargo object before each test
        cargo = new Cargo("TEST-001", "Test Owner", 1000.0);
    }
    
    @Test
    @DisplayName("Test adding an item within capacity")
    void testAddItem_Success() {
        // Test adding an item within capacity
        cargo.addItem("Test Item", 10, 5.0);
        
        assertEquals(50.0, cargo.getUsedCapacity());
        assertEquals(950.0, cargo.getAvailableCapacity());
        
        List<CargoItem> items = cargo.getItems();
        assertEquals(1, items.size());
        assertEquals("Test Item", items.get(0).getName());
        assertEquals(10, items.get(0).getAmount());
    }
    
    @Test
    @DisplayName("Test adding an item that exceeds capacity")
    void testAddItem_ExceedsCapacity() {
        // Test adding an item that exceeds capacity
        assertThrows(IllegalStateException.class, () -> {
            cargo.addItem("Heavy Item", 1000, 2.0);
        });
    }
    
    @Test
    @DisplayName("Test adding multiple items")
    void testAddMultipleItems() {
        // Add first item
        cargo.addItem("Item 1", 10, 5.0);
        // Add second item
        cargo.addItem("Item 2", 20, 10.0);
        
        assertEquals(250.0, cargo.getUsedCapacity()); // 10*5 + 20*10 = 250
        assertEquals(750.0, cargo.getAvailableCapacity());
        assertEquals(2, cargo.getItems().size());
    }
    
    @Test
    @DisplayName("Test adding more of an existing item")
    void testAddExistingItem() {
        // Add an item
        cargo.addItem("Test Item", 10, 5.0);
        // Add more of the same item
        cargo.addItem("Test Item", 5, 5.0);
        
        assertEquals(75.0, cargo.getUsedCapacity()); // 15*5 = 75
        
        List<CargoItem> items = cargo.getItems();
        assertEquals(1, items.size());
        assertEquals(15, items.get(0).getAmount());
    }
    
    @Test
    @DisplayName("Test removing an item")
    void testRemoveItem_Success() {
        // Add an item first
        cargo.addItem("Test Item", 10, 5.0);
        
        // Remove some of the item
        cargo.removeItem("Test Item", 5, 5.0);
        
        assertEquals(25.0, cargo.getUsedCapacity());
        assertEquals(975.0, cargo.getAvailableCapacity());
        
        List<CargoItem> items = cargo.getItems();
        assertEquals(1, items.size());
        assertEquals(5, items.get(0).getAmount());
    }
    
    @Test
    @DisplayName("Test removing more items than available")
    void testRemoveItem_NotEnoughItems() {
        // Add an item first
        cargo.addItem("Test Item", 10, 5.0);
        
        // Try to remove more than available
        assertThrows(IllegalStateException.class, () -> {
            cargo.removeItem("Test Item", 20, 5.0);
        });
    }
    
    @Test
    @DisplayName("Test removing an item that doesn't exist")
    void testRemoveItem_ItemNotFound() {
        // Try to remove an item that doesn't exist
        assertThrows(IllegalArgumentException.class, () -> {
            cargo.removeItem("Non-existent Item", 5, 5.0);
        });
    }
    
    @Test
    @DisplayName("Test removing all of an item")
    void testRemoveAllOfItem() {
        // Add an item
        cargo.addItem("Test Item", 10, 5.0);
        
        // Remove all of it
        cargo.removeItem("Test Item", 10, 5.0);
        
        assertEquals(0.0, cargo.getUsedCapacity());
        assertEquals(1000.0, cargo.getAvailableCapacity());
        assertEquals(0, cargo.getItems().size());
    }
}
