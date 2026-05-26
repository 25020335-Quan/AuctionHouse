package auction.util;

import java.io.Serializable;

public class AutoBidRequest implements Serializable {
    private String itemId;
    private String userId;
    private double maxBid;
    private double increment;
    public AutoBidRequest(String itemId, String userId, double maxBid, double increment) {
        this.itemId = itemId;
        this.userId = userId;
        this.maxBid = maxBid;
        this.increment = increment;
    }
    public String getItemId() { return itemId; }
    public String getUserId() { return userId; }
    public double getMaxBid() { return maxBid; }
    public double getIncrement() { return increment; }
}
