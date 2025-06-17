import java.util.ArrayList;
//find a way to store the info inputed by the user to the database
public class Schedule {
    private ArrayList<Ship> scheduledShips;
    public Schedule() {
        scheduledShips = new ArrayList<>();
    }

    // Add any type of ship (CargoShip or PassengerShip)
    public void addShip(Ship ship) {
        scheduledShips.add(ship);
    }

    // Show all scheduled ships
    public void displaySchedule() {
        System.out.println("Ship Schedule:");
        for (int i = 0; i < scheduledShips.size(); i++) {
            Ship ship = scheduledShips.get(i);
            System.out.println("Ship ID: SHIP" + (i + 1));
            System.out.println("Type: " + ship.getType());
            System.out.println("Location: " + ship.getLocation());
            System.out.println("Destination: " + ship.getDestination());
            System.out.println("Details: " + ship.toString());
            System.out.println("---------------");
        }
    }

    //for searching by index
    public Ship getShipByIndex(int index) {
        if (index >= 0 && index < scheduledShips.size()) {
            return scheduledShips.get(index);
        } else {
            return null;
        }
    }

    
    public int totalShips() {
        return scheduledShips.size();
    }
}
