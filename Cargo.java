import java.util.ArrayList;

public class Cargo {
    private String cargoId;
    private String ownerName;
    private ArrayList<CargoItem> items;

    public Cargo(String cargoId, String ownerName) {
        this.cargoId = cargoId;
        this.ownerName = ownerName;
        this.items = new ArrayList<>();
    }
public Cargo(double cargoCapacity) {
        //TODO Auto-generated constructor stub
    }
    //adding new or existing stock items
    public void addItem(String name, int amount) {
        for (CargoItem item : items) {
            if (item.getName().equalsIgnoreCase(name)) {
                item.addAmount(amount);
                return;
            }
        }
        items.add(new CargoItem(name, amount));
    }
// removing stock items
    public boolean removeItem(String name, int amount) {
        for (CargoItem item : items) {
            if (item.getName().equalsIgnoreCase(name)) {
                if (item.getAmount() >= amount) {
                    item.removeAmount(amount);
                    if (item.getAmount() == 0) {
                        items.remove(item);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    public int getTotalCargo() {
        int total = 0;
        for (CargoItem item : items) {
            total += item.getAmount();
        }
        return total;
    }

    public void showItems() {
        System.out.println("Cargo ID: " + cargoId);
        System.out.println("Owner: " + ownerName);
        System.out.println("Items:");
        for (CargoItem item : items) {
            System.out.println("- " + item);
        }
        System.out.println("Total Cargo: " + getTotalCargo());
    }

    // Getters
    public String getCargoId() {
        return cargoId;
    }

    public String getOwnerName() {
        return ownerName;
    }
}
