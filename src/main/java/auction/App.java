package auction;

import auction.model.users.Member;

public class App {
    public static void main(String[] args) {

        Member userA = new Member("U01", "25020335-1", "password123");
        Member userB = new Member("U02", "25020335-2", "password456");

        userA.createItem("ELECTRONICS", "E01", "Laptop Gaming", 1000);
        userB.bid(userA.findById("E01"), 1100); // Error because item is still pending
        userA.postItem(userA.findById("E01"));
        userA.bid(userA.findById("E01"), 1200); // Can not bid your own item
        userB.bid(userA.findById("E01"), 1500);
    }
}