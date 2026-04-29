package auction.model.item;

// Lớp cho tác phương tiện giao thông
public class Vehicle extends Item {
    public Vehicle(String id, String ownerId, String name, double startingPrice) {
        super(id, ownerId, name, startingPrice);
    }

    @Override
    public String getItemType() {
        return "Vehicle";
    }
}