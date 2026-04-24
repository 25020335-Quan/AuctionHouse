package auction.model;

/**
 * Interface Seller
 */

interface Seller {

    void createItem(String type, String id, String name, double startingPrice);
    void postItem(Item item);
}