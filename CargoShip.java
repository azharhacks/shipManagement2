public class CargoShip extends Ship {
    private Cargo cargo;

    public CargoShip(String location, String destination, Cargo cargo) {
        super("Cargo", location, destination);
        this.cargo = cargo;
    }

    public CargoShip(String type, String location, String destination, double cargoCapacity) {
        super(type, location, destination);
        this.cargo = new Cargo(cargoCapacity);
    }

    public Cargo getCargo() {
        return cargo;
    }

    public void setCargo(Cargo cargo) {
        this.cargo = cargo;
    }

    @Override
    public String toString() {
        return "CargoShip traveling from " + location + " to " + destination +
               " carrying " + cargo.getTotalCargo() + " units of goods.";
    }
}

