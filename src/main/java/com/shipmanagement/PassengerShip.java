package com.shipmanagement;
public class PassengerShip extends Ship {
    private int passengers;

    public PassengerShip(String location, String destination, int passengers) {
        super("Passenger", location, destination);
        this.passengers = passengers;
    }

    public int getPassengers() {
        return passengers;
    }

    public void setPassengers(int passengers) {
        this.passengers = passengers;
    }

    @Override
    public String toString() {
        return "PassengerShip traveling from " + location + " to " + destination +
               " with " + passengers + " passengers.";
    }
}
