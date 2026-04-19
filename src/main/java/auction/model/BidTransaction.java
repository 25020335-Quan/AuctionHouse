package auction.model;

import java.time.LocalDateTime;

public class BidTransaction extends Entity {
    private String bidderId;      // ID người đặt giá
    private String itemId;        // ID sản phẩm được đấu giá
    private double bidAmount;     // Số tiền đặt
    private LocalDateTime bidTime; // Thời điểm đặt giá

    public BidTransaction(String id, String bidderId, String itemId, double bidAmount) {
        super(id);
        this.bidderId = bidderId;
        this.itemId = itemId;
        this.bidAmount = bidAmount;
        this.bidTime = LocalDateTime.now(); // Tự động lấy thời gian hiện tại
    }

    // Getters
    public String getBidderId() { return bidderId; }
    public String getItemId() { return itemId; }
    public double getBidAmount() { return bidAmount; }
    public LocalDateTime getBidTime() { return bidTime; }

    @Override
    public String toString() {
        return String.format("Bid: %.2f by %s at %s", bidAmount, bidderId, bidTime);
    }
}