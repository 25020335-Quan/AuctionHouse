package auction.model.factory;

import auction.model.item.Item;
import auction.model.item.Vehicle;

public class VehicleFactory extends ItemFactory {
    @Override
    public Item createItem(String id , String ownerId, String name, double startingPrice) {
        return new Vehicle(id, ownerId, name, startingPrice);
    }
    @Override
    public Item createNewItem(String ownerId, String name, double startingPrice) {
        return new Vehicle(ownerId, name, startingPrice); // Không truyền ID
    }
}
