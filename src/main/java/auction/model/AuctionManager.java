package auction.model;

import auction.exception.InvalidBidException;
import auction.model.item.Item;
import auction.model.state.AuctionState;
import auction.model.transaction.BidTransaction;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.concurrent.locks.ReentrantLock;

public class AuctionManager {
    // Lock để tránh race conditino
    private final ReentrantLock lock = new ReentrantLock();
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

    public synchronized void attemptBid(Item item, String bidderId, double amount) throws InvalidBidException {
        lock.lock(); // Đảm bảo an toàn đa luồng (phần Server)
        try {
            // 1. Kiểm tra trạng thái phiên đấu giá
            if (item.getState() == AuctionState.CLOSED || item.getState() == AuctionState.SOLD) {
                throw new InvalidBidException("Phiên đấu giá đã kết thúc!");
            }

            if (item.getState() == AuctionState.PENDING) {
                throw new InvalidBidException("Lỗi: Phiên đấu giá chưa bắt đầu.");
            }

            // 2. Kiểm tra bước giá (Ví dụ: giá mới phải cao hơn giá cũ)
            if (amount <= item.getCurrentPrice()) {
                throw new InvalidBidException("Giá đặt phải lớn hơn giá hiện tại!");
            }

            // 3. Kiểm tra vai trò (Người bán không được tự đấu giá đồ của mình)
            if (item.getOwnerId().equals(bidderId)) {
                throw new InvalidBidException("Lỗi: Người bán không được tự đấu giá đồ của mình).");
            }


            // Nếu mọi thứ hợp lệ -> Cập nhật Model
            item.setPrice(amount);
            if (item.getState() == AuctionState.OPEN) {
                item.setState(AuctionState.RUNNING);
            }

            // Ghi nhận giao dịch thông qua AuctionManager
            String txId = "TX-" + System.currentTimeMillis();
            BidTransaction tx = new BidTransaction(txId, bidderId, item.getId(), amount);
            AuctionManager.getInstance().addTransaction(tx);


        } finally {
            lock.unlock();
        }
    }
}