 //i will try to connect this class to the user class which will be the one to input the data
           //find a way to store the info inputed by the user to the database   
     public abstract class Ship {        
    protected String type;            
    protected String location;
    protected String destination;

    public Ship(String type, String location, String destination) {
        this.type = type;
        this.location = location;
        this.destination = destination;
    }

    

    // Common behavior
    public void setLocation(String location) { this.location = location; }
    public void setDestination(String destination) { this.destination = destination; }

    public String getLocation() { return location; }
    public String getDestination() { return destination; }
    public String getType() { return type; }
}

