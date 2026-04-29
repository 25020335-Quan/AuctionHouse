package auction.model.interfaces;

import auction.model.item.Item;

/**
 * Interface Seller
 */

public interface Seller {

    void createItem(String type, String id, String name, double startingPrice);
    void postItem(Item item);
}