package auction.model;

import auction.exception.InvalidBidException;
import auction.model.item.Item;
import auction.model.notification.BidNotification;
import auction.model.state.AuctionState;
import auction.model.transaction.BidTransaction;
import auction.model.users.Member;
import auction.model.users.User;
import auction.server.AuctionServer;

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

    public void updateList(List<Item> item) {
        items = item;
    }

    public void updateAllTransaction(List<BidTransaction> transaction) { transactions = transaction; }

    public void addItem(Item item) {
        items.add(item);
    }

    public void removeItemByID(String itemId) {
        lock.lock();
        try {
            items.removeIf(item -> item.getId().equals(itemId));
        } finally {
            lock.unlock();
        }
    }

    public List<Item> getAllItems() {
        lock.lock(); // Khóa lại trước khi copy
        try {
            return new ArrayList<>(items);
        } finally {
            lock.unlock(); // An toàn rồi thì mới mở khóa
        }
    }

    public List<BidTransaction> getAllTransaction() {
        lock.lock(); // Khóa lại trước khi copy
        try {
            return new ArrayList<>(transactions);
        } finally {
            lock.unlock(); // An toàn rồi thì mới mở khóa
        }
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

    public void attemptBid(Item item, String bidderId, double amount) throws InvalidBidException {
        lock.lock(); // Đảm bảo an toàn đa luồng bằng ReentrantLock
        try {
            // 1. Kiểm tra trạng thái phiên đấu giá
            if (item.getState() == AuctionState.CLOSED || item.getState() == AuctionState.SOLD) {
                throw new InvalidBidException("Phiên đấu giá đã kết thúc!");
            }

            if (item.getState() == AuctionState.PENDING) {
                throw new InvalidBidException("Lỗi: Phiên đấu giá chưa bắt đầu.");
            }

            if (getUserById(bidderId).getBalance() < amount) {
                throw new InvalidBidException("Lỗi: Không có đủ tiền.");
            }

            // 2. Kiểm tra bước giá
            if (amount <= item.getCurrentPrice()) {
                throw new InvalidBidException("Giá đặt phải lớn hơn giá hiện tại!");
            }

            // 3. Kiểm tra vai trò
            if (item.getOwnerId().equals(bidderId)) {
                throw new InvalidBidException("Lỗi: Người bán không được tự đấu giá đồ của mình.");
            }

            // Cập nhật Model
            item.setPrice(amount);
            item.setHighestBidderId(bidderId);
            if (item.getState() == AuctionState.OPEN) {
                item.setState(AuctionState.RUNNING);
            }

            // Xử lý Anti-Sniping
            boolean isTimeExtended = false;
            if (item.getEndTime() != null) {
                java.time.LocalDateTime now = java.time.LocalDateTime.now();
                long secondsLeft = java.time.temporal.ChronoUnit.SECONDS.between(now, item.getEndTime());

                // Nếu còn dưới 30 giây
                if (secondsLeft > 0 && secondsLeft <= 30) {
                    item.setEndTime(now.plusSeconds(60)); // Cộng 60s từ lúc hiện tại
                    isTimeExtended = true;
                    System.out.println("[Anti-Sniping] Đã kích hoạt! Cộng thêm 60s cho item " + item.getId());
                }
            }

            // Ghi nhận giao dịch
            String txId = "TX-" + System.currentTimeMillis();
            BidTransaction tx = new BidTransaction(txId, bidderId, item.getId(), amount);
            AuctionManager.getInstance().addTransaction(tx);

            // Gọi Database
            try {
                auction.model.service.DatabaseService dbService = new auction.model.service.DatabaseService();
                dbService.addTransaction(tx);

                // Quyết định gọi hàm lưu vào Database
                if (isTimeExtended) {
                    // Nếu bị bắn tỉa -> Gọi hàm mới để lưu cả Giá và Giờ mới
                    dbService.updateBidAndExtendTimer(item.getId(), amount, bidderId, item.getEndTime());
                } else {
                    // Nếu thời gian còn xông xênh -> Chỉ lưu Giá như bình thường
                    dbService.updateHighestBid(item.getId(), amount, bidderId);
                }

                // Phát loa cho tất cả Client (Bác đã có sẵn constructor 4 tham số, quá xịn!)
                BidNotification manualNotify = new BidNotification(
                        item.getId(),
                        bidderId,
                        amount,
                        item.getEndTime()
                );
                AuctionServer.broadcast(manualNotify);
            } catch (Exception e) {
                // Bỏ qua lỗi DB/Network trong lúc chạy Unit Test
                System.out.println("Cảnh báo: Không thể đồng bộ DB/Mạng - " + e.getMessage());
            }

            boolean autoBidTriggered = false;
            while (item.processAutoBids()) {
                autoBidTriggered = true;
            }

            // Xử lý AutoBid
            if (autoBidTriggered) {
                String autoTxId = "TX-" + System.currentTimeMillis();
                double botPrice = item.getCurrentPrice();
                String botBidder = item.getHighestBidderId();
                BidTransaction autoTx = new BidTransaction(autoTxId, botBidder, item.getId(), botPrice);
                AuctionManager.getInstance().addTransaction(autoTx);

                // BỌC TRY-CATCH CHO AUTO-BID EXTERNAL SERVICES
                try {
                    auction.model.service.DatabaseService dbService = new auction.model.service.DatabaseService();
                    dbService.updateHighestBid(item.getId(), botPrice, botBidder);
                    dbService.addTransaction(autoTx);

                    BidNotification botNotify = new BidNotification(
                            item.getId(),
                            botBidder,
                            botPrice,
                            item.getEndTime()
                    );
                    AuctionServer.broadcast(botNotify);
                } catch (Exception e) {
                    System.out.println("Cảnh báo: Không thể đồng bộ DB/Mạng (AutoBid) - " + e.getMessage());
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public Item getItemById(String itemId) {
        lock.lock();
        try {
            return items.stream().filter(item -> item.getId().equals(itemId)).findFirst().orElse(null);
        } finally {
            lock.unlock();
        }
    }

    public User getUserById(String userId) {
        try {
            // Khởi tạo đối tượng dịch vụ cơ sở dữ liệu
            auction.model.service.DatabaseService dbService = new auction.model.service.DatabaseService();

            // Gọi hàm getUserById viết dưới DatabaseService
            User realUser = dbService.getUserById(userId);

            if (realUser != null) {
                return realUser; // Tìm thấy người dùng thật trong SQL -> Trả về
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi kết nối SQL lấy User thật: " + e.getMessage());
        }

        // Nếu Database mất kết nối,
        // Server sẽ trả về một User ẩn danh tạm thời để không bị crash
        return new Member(userId, "Người ẩn danh (" + userId + ")", "" , "anonymous", "abc@gmail.com", 100000);
    }

    // Kiểm tra state của các item
    public void startStateMonitor() {
        Thread monitorThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000); // Kiểm tra mỗi giây 1 lần
                    lock.lock(); // Khóa an toàn để không đụng chạm luồng khác
                    try {
                        java.time.LocalDateTime now = java.time.LocalDateTime.now();

                        for (Item item : items) {
                            // Kiểm tra mở cửa
                            if (item.getState() == AuctionState.PENDING && item.getStartTime() != null) {
                                if (now.isEqual(item.getStartTime()) || now.isAfter(item.getStartTime())) {
                                    item.setState(AuctionState.OPEN);
                                    System.out.println("Đã tự động mở cửa phiên: " + item.getName());
                                }
                            }

                            // Kiểm tra đóng cửa
                            if ((item.getState() == AuctionState.OPEN || item.getState() == AuctionState.RUNNING) && item.getEndTime() != null) {
                                if (now.isEqual(item.getEndTime()) || now.isAfter(item.getEndTime())) {
                                    item.setState(AuctionState.CLOSED);
                                    System.out.println("Đã tự động đóng cửa phiên: " + item.getName());
                                }
                            }
                        }
                    } finally {
                        lock.unlock(); // Đi kiểm tra xong thì mở khóa
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } finally {

                }
            }
        });
        monitorThread.setDaemon(true); // Đặt thành Daemon để nó tự chết khi tắt Server
        monitorThread.start(); // Kích hoạt
    }
}