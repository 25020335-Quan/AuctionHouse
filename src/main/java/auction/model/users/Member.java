package auction.model.users;

import auction.exception.InvalidBidException;
import auction.model.*;
import auction.model.factory.FactoryProvider;
import auction.model.interfaces.Bidder;
import auction.model.interfaces.Seller;
import auction.model.item.Item;
import auction.model.state.AuctionState;

import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;

public class Member extends User implements Bidder, Seller, Serializable {
    private List<Item> ownedItems = new ArrayList<>();

    public Member(String id, String username, String password) {
        super(id, username, password);
    }

    @Override
    public void bid(Item item, double amount) {
        AuctionManager instance = AuctionManager.getInstance();
        try {
            instance.attemptBid(item, this.id, amount);
        } catch (InvalidBidException e) {
            System.err.println(e.getMessage());
        }
    }

    @Override
    public void createItem(String type, String id, String name, double startingPrice) {
        ownedItems.add(FactoryProvider.createItemByType(type, id, this.id, name, startingPrice));
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
