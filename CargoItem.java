public class CargoItem {
    private String name;
    private int amount;

    public CargoItem(String name, int amount) {
        this.name = name;
        this.amount = amount;
    }

    
    public String getName() {
        return name;
    }
    
    public void addAmount(int amount) {
        this.amount += amount;
    }

    public void removeAmount(int amount) {
        if (this.amount >= amount) {
            this.amount -= amount;
        } else {
            
            System.out.println("Cannot remove more than available amount.");
        }
    }

    
    public int getAmount() {
        return amount;
    }

    // Optional: toString() is used when you print the item
    public String toString() {
        return name + " - " + amount;
    }
}

