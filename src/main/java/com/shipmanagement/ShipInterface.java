package com.shipmanagement;

public interface ShipInterface {
    int getId();
    void setId(int id);
    String getLocation();
    void setLocation(String location);
    String getDestination();
    void setDestination(String destination);
    String getType();
}