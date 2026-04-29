package auction.model.item;

import auction.model.Entity;
import auction.model.state.AuctionState;

public abstract class Item extends Entity {
    private final String ownerId;
    private String name;
    private double currentPrice; // Giá hiện tại
    private AuctionState state;

    public Item(String id, String ownerId, String name, double startingPrice) {
        super(id);
        this.ownerId = ownerId;
        this.name = name;
        this.currentPrice = startingPrice; // Ban đầu giá hiện tại bằng giá khởi điểm

        // Mặc định khi tạo ra, món hàng ở trạng thái PENDING
        this.state = AuctionState.PENDING;
    }

//    // Phương thức để thực hiện đặt giá
//    public boolean placeBid(User bidder, double amount) {
//        // Kiểm tra xem giá đặt có cao hơn giá hiện tại không
//        if (amount > this.currentPrice) {
//            this.currentPrice = amount;
//
//            // Tạo một giao dịch mới
//            // ID giao dịch có thể tạo ngẫu nhiên hoặc theo số thứ tự
//            String txId = "TX-" + System.currentTimeMillis();
//            BidTransaction newTransaction = new BidTransaction(txId, bidder.getId(), this.id, amount);
//
//            // Gửi giao dịch này vào AuctionManager để lưu trữ
//            AuctionManager.getInstance().addTransaction(newTransaction);
//
//            System.out.println("Dat gia thanh cong cho: " + this.name);
//            return true;
//        } else {
//            System.out.println("Gia dat phai cao hon gia hien tai: " + this.currentPrice);
//            return false;
//        }
//    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public double getCurrentPrice() { return currentPrice; }
    public abstract String getItemType();
    public AuctionState getState() {
        return state;
    }
    public String getOwnerId() { return ownerId; }

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
}

// Lớp cho đồ điện tử
class Electronics extends Item {
    public Electronics(String id, String ownerId, String name, double startingPrice) {
        super(id, ownerId, name, startingPrice);
    }

    @Override
    public String getItemType() {
        return "Electronics";
    }
}

// Lớp cho tác phẩm nghệ thuật
class Art extends Item {
    public Art(String id, String ownerId, String name, double startingPrice) {

        super(id, ownerId, name, startingPrice);
    }

    @Override
    public String getItemType() {
        return "Art";
    }
}

// Lớp cho tác phương tiện giao thông
class Vehicle extends Item {
    public Vehicle(String id, String ownerId, String name, double startingPrice) {
        super(id, ownerId, name, startingPrice);
    }

    @Override
    public String getItemType() {
        return "Vehicle";
    }
}