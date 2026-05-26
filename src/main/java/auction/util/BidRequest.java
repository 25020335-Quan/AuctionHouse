package auction.util;

import java.io.Serializable;

public class BidRequest implements Serializable {
    private String itemId;
    private String bidderId;
    private double amount;

    public BidRequest(String itemId, String bidderId, double amount) {
        this.itemId = itemId;
        this.bidderId = bidderId;
        this.amount = amount;
    }

    public String getItemId() { return itemId; }
    public String getBidderId() { return bidderId; }
    public double getAmount() { return amount; }
}

