package auction.model.interfaces;


import auction.model.item.Item;

/**
 * Interface Bidder
 */
public interface Bidder {
    void bid(Item item, double amount);

}