package com.shipmanagement.model;

import java.time.LocalDate;

public class Cargo {
    private int id;
    private Integer shipId;
    private String description;
    private double weight;
    private String status;
    private LocalDate loadingDate;
    private LocalDate unloadingDate;

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public Integer getShipId() { return shipId; }
    public void setShipId(Integer shipId) { this.shipId = shipId; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public double getWeight() { return weight; }
    public void setWeight(double weight) { this.weight = weight; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public LocalDate getLoadingDate() { return loadingDate; }
    public void setLoadingDate(LocalDate loadingDate) { this.loadingDate = loadingDate; }
    
    public LocalDate getUnloadingDate() { return unloadingDate; }
    public void setUnloadingDate(LocalDate unloadingDate) { this.unloadingDate = unloadingDate; }
}
