package auction.model.factory;

import auction.model.item.Art;
import auction.model.item.Item;

public class ArtFactory extends ItemFactory {
    @Override
    public Item createItem(String id , String ownerId, String name, double startingPrice) {
        return new Art( ownerId, name, startingPrice);
    }
    @Override
    public Item createNewItem(String ownerId, String name, double startingPrice) {
        return new Art(ownerId, name, startingPrice); // Không truyền ID
    }
}
