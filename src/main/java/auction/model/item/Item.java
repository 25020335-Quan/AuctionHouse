package auction.model.item;

import auction.model.Entity;
import auction.model.state.AuctionState;

import java.io.Serializable;

public abstract class Item extends Entity implements Serializable {
    private final String ownerId;
    private String name;
    private double currentPrice; // Giá hiện tại
    private AuctionState state;
    private String description = "";

    public Item(String id, String ownerId, String name, double startingPrice) {
        super(id);
        this.ownerId = ownerId;
        this.name = name;
        this.currentPrice = startingPrice; // Ban đầu giá hiện tại bằng giá khởi điểm

        // Mặc định khi tạo ra, món hàng ở trạng thái PENDING
        this.state = AuctionState.PENDING;
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public double getCurrentPrice() { return currentPrice; }
    public abstract String getItemType();
    public AuctionState getState() {
        return state;
    }
    public String getOwnerId() { return ownerId; }
    public String getDescription() {
        return description;
    }

    // Setter
    public void setState(AuctionState state) {
        this.state = state;
        System.out.println("[Hệ thống] Sản phẩm " + getName() + " chuyển sang trạng thái: " + state);
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPrice(double newPrice) {
        this.currentPrice = newPrice;
    }
    public void setDescription(String description) { this.description = description; }
}