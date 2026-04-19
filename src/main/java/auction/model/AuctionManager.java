package auction.model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AuctionManager {
    // 1. Singleton Instance
    private static AuctionManager instance;

    // 2. Danh sách quản lý dữ liệu
    private List<Item> items;
    private List<BidTransaction> transactions;

    // Constructor private để ngăn việc khởi tạo từ bên ngoài
    private AuctionManager() {
        items = new ArrayList<>();
        transactions = new ArrayList<>();
    }

    // 3. Phương thức lấy Instance duy nhất
    public static synchronized AuctionManager getInstance() {
        if (instance == null) {
            instance = new AuctionManager();
        }
        return instance;
    }

    // --- QUẢN LÝ SẢN PHẨM ---

    public void addItem(Item item) {
        items.add(item);
    }

    public List<Item> getAllItems() {
        return new ArrayList<>(items); // Trả về bản sao để bảo vệ dữ liệu gốc
    }

    // --- QUẢN LÝ GIAO DỊCH ĐẶT GIÁ ---

    public void addTransaction(BidTransaction tx) {
        transactions.add(tx);
        System.out.println("Giao dịch mới: " + tx.toString());
    }

    public List<BidTransaction> printTransaction() {
        for (BidTransaction trans : transactions) {
            System.out.println(trans);
        }
        return null;
    }

    // Lấy lịch sử đặt giá của một sản phẩm cụ thể
    public List<BidTransaction> getHistoryByItem(String itemId) {
        return transactions.stream()
                .filter(tx -> tx.getItemId().equals(itemId))
                .collect(Collectors.toList());
    }

    // Tìm giá cao nhất hiện tại của một sản phẩm
    public double getCurrentMaxPrice(String itemId) {
        return transactions.stream()
                .filter(tx -> tx.getItemId().equals(itemId))
                .mapToDouble(BidTransaction::getBidAmount)
                .max()
                .orElse(0.0);
    }
}