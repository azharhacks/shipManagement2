package com.shipmanagement.model;

public class Staff {
    private int id;
    private String firstName;
    private String lastName;
    private String username;
    private String password;
    private String role;
    private String status;
    private Integer shipId;
    private String shipName;
    private int taskCount;
    private String email;
    private String phone;
    
    public Staff() {
    }
    
    public Staff(int id, String firstName, String lastName, String username, String password, String role, String status, Integer shipId, String email, String phone) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.username = username;
        this.password = password;
        this.role = role;
        this.status = status;
        this.shipId = shipId;
        this.email = email;
        this.phone = phone;
    }
    
    // Getters and Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getFirstName() {
        return firstName;
    }
    
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    
    public String getLastName() {
        return lastName;
    }
    
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public Integer getShipId() {
        return shipId;
    }
    
    public void setShipId(Integer shipId) {
        this.shipId = shipId;
    }
    
    public String getShipName() {
        return shipName;
    }
    
    public void setShipName(String shipName) {
        this.shipName = shipName;
    }
    
    public int getTaskCount() {
        return taskCount;
    }
    
    public void setTaskCount(int taskCount) {
        this.taskCount = taskCount;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
}
