package auction.model.users;

import auction.exception.InvalidBidException;
import auction.model.AuctionManager;
import auction.model.factory.FactoryProvider;
import auction.model.interfaces.Bidder;
import auction.model.interfaces.Seller;
import auction.model.item.Item;
import auction.model.service.DatabaseService;
import auction.model.state.AuctionState;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Lớp Admin (Quản trị viên) kế thừa từ User.
 */
public class Admin extends User implements Serializable, Bidder, Seller {
    private List<Item> ownedItems = new ArrayList<>();

    public Admin(String id, String username, String password, String fullName, String email, double balance) {
        super(id, username, password, fullName, email, balance);
    }

    public Admin(String id, String username, String password, String fullName, String email) {
        super(id, username, password, fullName, email);
    }

    public Admin(String username, String password, String fullName, String email) {
        super(username, password, fullName, email);
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

    public String getRole() { return "ADMIN";}
}