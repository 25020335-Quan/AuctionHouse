package auction.model;

import auction.exception.InvalidBidException;

import java.util.List;
import java.util.ArrayList;

public class Member extends User implements Bidder, Seller {
    private List<Item> ownedItems = new ArrayList<>();

    public Member(String id, String username, String password) {
        super(id, username, password);
    }

    @Override
    public void bid(Item item, double amount) {
        AuctionManager instance = AuctionManager.getInstance();
        try {
            instance.attemptBid(item, id, amount);
        } catch (InvalidBidException e) {
            System.err.println(e.getMessage());
        }
    }

    @Override
    public void createItem(String type, String id, String name, double startingPrice) {
        ownedItems.add(ItemFactory.factoryItem(type, this.id, id, name, startingPrice));
        // this.id là ownerId
    }

    @Override
    public void postItem(Item item) {
        item.setState(AuctionState.OPEN);
    }

    public Item findById(String id) {
        for (Item item : ownedItems) {
            if (item.getId().equals(id)) {
                return item;
            }
        }

        return null;
    }

}
