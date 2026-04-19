package auction.model;

public abstract class Item extends Entity {
    private String name;
    private double currentPrice; // Giá hiện tại

    public Item(String id, String name, double startingPrice) {
        super(id);
        this.name = name;
        this.currentPrice = startingPrice; // Ban đầu giá hiện tại bằng giá khởi điểm
    }

    // Phương thức để thực hiện đặt giá
    public boolean placeBid(User bidder, double amount) {
        // Kiểm tra xem giá đặt có cao hơn giá hiện tại không
        if (amount > this.currentPrice) {
            this.currentPrice = amount;

            // Tạo một giao dịch mới
            // ID giao dịch có thể tạo ngẫu nhiên hoặc theo số thứ tự
            String txId = "TX-" + System.currentTimeMillis();
            BidTransaction newTransaction = new BidTransaction(txId, bidder.getId(), this.id, amount);

            // Gửi giao dịch này vào AuctionManager để lưu trữ
            AuctionManager.getInstance().addTransaction(newTransaction);

            System.out.println("Dat gia thanh cong cho: " + this.name);
            return true;
        } else {
            System.out.println("Gia dat phai cao hon gia hien tai: " + this.currentPrice);
            return false;
        }
    }

    // Getters
    public String getName() { return name; }
    public double getCurrentPrice() { return currentPrice; }
    public abstract String getItemType();
}

// Lớp cho đồ điện tử
class Electronics extends Item {
    public Electronics(String id, String name, double startingPrice) {
        super(id, name, startingPrice);
    }

    @Override
    public String getItemType() {
        return "Electronics";
    }
}

// Lớp cho tác phẩm nghệ thuật
class Art extends Item {
    public Art(String id, String name, double startingPrice) {
        super(id, name, startingPrice);
    }

    @Override
    public String getItemType() {
        return "Art";
    }
}

// Lớp cho tác phương tiện giao thông
class Vehicle extends Item {
    public Vehicle(String id, String name, double startingPrice) {
        super(id, name, startingPrice);
    }

    @Override
    public String getItemType() {
        return "Vehicle";
    }
}