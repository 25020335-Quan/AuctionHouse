package auction.model.factory;

import auction.model.item.Item;
import auction.model.item.Electronics;

public class ElectronicsFactory extends ItemFactory {
    @Override
    public Item createItem(String id , String ownerId, String name, double startingPrice) {
        return new Electronics(id, ownerId, name, startingPrice);
    }
    @Override
    public Item createNewItem(String ownerId, String name, double startingPrice) {
        return new Electronics(ownerId, name, startingPrice); // Không truyền ID
    }
}
