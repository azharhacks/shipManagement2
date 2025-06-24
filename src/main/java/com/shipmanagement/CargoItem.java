package com.shipmanagement;

public class CargoItem {
    private String name;
    private int amount;

    public CargoItem(String name, int amount) {
        this.name = name;
        this.amount = amount;
    }

    // Getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    // Optional: toString() is used when you print the item
    @Override
    public String toString() {
        return name + " - " + amount;
    }
}
