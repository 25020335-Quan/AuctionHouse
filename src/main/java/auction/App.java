package auction;

import auction.model.*;

import java.util.List;

public class App {
    public static void main(String[] args) {
        // 1. Lấy instance duy nhất của AuctionManager (Singleton)
        AuctionManager manager = AuctionManager.getInstance();

        Bidder userA = new Bidder("U01", "25020335-1", "password123");
        Bidder userB = new Bidder("U02", "25020335-2", "password456");
        Item electronics = ItemFactory.createItem("ELECTRONICS", "I01", "Laptop Gaming", 1500.0);

        // Thực hiện đặt giá
        electronics.placeBid(userA, 1600.0); // Thành công -> Tạo transaction
        electronics.placeBid(userB, 1700.0); // Thất bại -> Không tạo transaction

        List<BidTransaction> his = manager.getHistoryByItem("I01");
        for (BidTransaction trans : his) {
            System.out.println(trans);
        }
        System.out.println(manager.getCurrentMaxPrice("I01"));
    }
}