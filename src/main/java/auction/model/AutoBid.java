package auction.model;

import auction.model.users.Member;

public class AutoBid implements Comparable<AutoBid>{
    private Member bidder;
    private double maxBid;
    private double increment;

    public AutoBid(Member bidder, double maxBid, double increment) {
        this.bidder = bidder;
        this.maxBid = maxBid;
        this.increment = increment;
    }

    public Member getBidder() { return bidder; }
    public double getMaxBid() { return maxBid; }
    public double getIncrement() { return increment; }

    @Override
    public int compareTo(AutoBid other) {
        return Double.compare(other.maxBid, this.maxBid); // Ưu tiên maxBid cao hơn xếp trước để đưa vào Priority Queue
    }
}
