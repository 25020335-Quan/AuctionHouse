package auction.model.factory;

import auction.model.item.Item;

public abstract class ItemFactory {
    public abstract Item createItem(String id, String ownerId, String name, double startingPrice);
}
