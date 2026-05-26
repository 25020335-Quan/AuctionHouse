package auction.model.notification;

import java.io.Serializable;
import java.time.LocalDateTime;

public class BidNotification implements Serializable {
    private String itemId;
    private String highestBidderId;
    private double newPrice;
    private LocalDateTime newEndTime; // Đồng bộ giờ cho Anti-Sniping

    public BidNotification(String itemId, String highestBidderId, double newPrice, LocalDateTime newEndTime) {
        this.itemId = itemId;
        this.highestBidderId = highestBidderId;
        this.newPrice = newPrice;
        this.newEndTime = newEndTime;
    }

    public String getItemId() { return itemId; }
    public String getHighestBidderId() { return highestBidderId; }
    public double getNewPrice() { return newPrice; }
    public LocalDateTime getNewEndTime() { return newEndTime; }
}
