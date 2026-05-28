package auction.model.factory;

import auction.model.item.Item;

public abstract class ItemFactory {
    public abstract Item createItem(String id , String ownerid , String name, double startingPrice);

    public abstract Item createNewItem(String ownerid, String name, double startingPrice);
}
