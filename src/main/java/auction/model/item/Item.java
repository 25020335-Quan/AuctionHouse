package auction.model.item;

import auction.model.Entity;
import auction.model.state.AuctionState;

import java.io.Serializable;
import java.time.LocalDateTime;

public abstract class Item extends Entity implements Serializable {
    private final String ownerId;
    private String name;
    private double currentPrice; // Giá hiện tại
    private AuctionState state;
    private String description = "";
    private double startingPrice;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String highestBidderId;

    public Item(String id, String ownerId, String name, double startingPrice) {
        super(id);
        this.ownerId = ownerId;
        this.name = name;
        this.currentPrice = startingPrice; // Ban đầu giá hiện tại bằng giá khởi điểm
        this.startingPrice = startingPrice;

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
    public double getStartingPrice() { return startingPrice; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public String getHighestBidderId() { return highestBidderId; }

    // Setter
    public void setStartingPrice(double startingPrice) { this.startingPrice = startingPrice; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
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
    public void setHighestBidderId(String highestBidderId) {
        this.highestBidderId = highestBidderId;
    }
}