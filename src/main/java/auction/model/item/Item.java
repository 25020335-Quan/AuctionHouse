package auction.model.item;

import auction.model.AutoBid;
import auction.model.Entity;
import auction.model.state.AuctionState;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.PriorityQueue;

public abstract class Item extends Entity implements Serializable {
    private final String ownerId;
    private String name;
    private double currentPrice; // Giá hiện tại
    private AuctionState state;
    private String description = "";
    private double startingPrice;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String highestBidderId;

    private static int itemCounter = 1;

    // Khai báo biến lưu url của ảnh
    private java.util.List<String> imageUrls = new java.util.ArrayList<>();

    private transient PriorityQueue<AutoBid> autoBids = new java.util.PriorityQueue<>();

    public Item(String ownerId, String name, double startingPrice) {
        super("I-" + (itemCounter++));
        this.ownerId = ownerId;
        this.name = name;
        this.currentPrice = startingPrice;
        this.startingPrice = startingPrice;
        this.state = AuctionState.PENDING; // Mặc định là PENDING
    }


    public Item(String id, String ownerId, String name, double startingPrice) {
        super(id);
        this.ownerId = ownerId;
        this.name = name;
        this.currentPrice = startingPrice; // Ban đầu giá hiện tại bằng giá khởi điểm
        this.startingPrice = startingPrice;

        // Mặc định khi tạo ra, món hàng ở trạng thái PENDING
        this.state = AuctionState.PENDING;
    }

    public static void setItemCounter(int id) {
        itemCounter = id + 1;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public abstract String getItemType();

    public AuctionState getState() {
        return state;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public String getDescription() {
        return description;
    }

    public double getStartingPrice() {
        return startingPrice;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public String getHighestBidderId() {
        return highestBidderId;
    }
    public java.util.List<String> getImageUrls() {
        return this.imageUrls;
    }

    // Setter
    public void setStartingPrice(double startingPrice) {
        this.startingPrice = startingPrice;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

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

    public void setDescription(String description) {
        this.description = description;
    }

    public void setHighestBidderId(String highestBidderId) {
        this.highestBidderId = highestBidderId;
    }

    // Thêm ảnh
    public void addImageUrl(String url) {
        this.imageUrls.add(url);
    }




    // ________Xử lý AutoBid________
    // Hàm nhận lệnh AutoBid mới

    public synchronized void addAutoBid(AutoBid autoBid) {
        if (autoBids == null) autoBids = new java.util.PriorityQueue<>();
        autoBids.add(autoBid);

        double priceBeforeBot = this.currentPrice;

        // Kích hoạt hệ thống chạy tự động
        boolean autoBidTriggered = false;
        // Kích hoạt auto-bid
        while (processAutoBids()) {
            // Chạy cho đến khi tìm ra người dẫn đầu cuối cùng thì tự dừng
            autoBidTriggered = true;
        }
        if (autoBidTriggered && this.currentPrice > priceBeforeBot) {
            double finalPrice = this.currentPrice;
            String finalHighestBidder = this.highestBidderId;

            // 1. Tạo và lưu transaction vào AuctionManager bộ nhớ tạm
            String autoTxId = "TX-" + System.currentTimeMillis();
            auction.model.transaction.BidTransaction autoTx =
                    new auction.model.transaction.BidTransaction(autoTxId, finalHighestBidder, this.getId(), finalPrice);
            auction.model.AuctionManager.getInstance().addTransaction(autoTx);

            // 2. Ghi nhận xuống Database và cập nhật người giữ giá cao nhất
            auction.model.service.DatabaseService dbService = new auction.model.service.DatabaseService();
            dbService.addTransaction(autoTx);
            dbService.updateHighestBid(this.getId(), finalPrice, finalHighestBidder);

            System.out.println("[AutoBid Mới] Bot đã đẩy giá thành công lên: " + finalPrice + " VNĐ");

            // 3. Phát thông báo cập nhật giá mới cho toàn bộ Client đang kết nối
            auction.model.notification.BidNotification notify = new auction.model.notification.BidNotification(
                    this.getId(),
                    finalHighestBidder,
                    finalPrice,
                    this.getEndTime()
            );
            auction.server.AuctionServer.broadcast(notify);
        }
    }

    public synchronized boolean processAutoBids() {
        if (autoBids == null || autoBids.isEmpty()) return false;

        // Rút bot có maxBid lớn nhất ra khỏi hàng đợi
        AutoBid first = autoBids.poll();

        //  Nếu có bot đang ở ngôi dẫn đầu rồi -> Đưa lại vào queue
        if (this.highestBidderId != null && this.highestBidderId.equals(first.getBidder().getId())) {
            autoBids.add(first);
            return false;
        }

        // Nếu ví tiền Max của Bot số 1 còn thấp hơn cả giá hiện tại ->  gọi hàm đệ quy check bot tiếp theo
        if (first.getMaxBid() <= this.currentPrice) {
            return processAutoBids();
        }

        // Nhìn lén bot đứng thứ hai (Chỉ ngó xem chứ không rút ra khỏi hàng)
        AutoBid second = autoBids.peek();

        double nextPrice;

        // Tính toán giá bid tiếp theo
        if (second == null) {
            // Không có Bot khác cản đường, chỉ đấu với bid thường -> Nhích lên 1 bước giá
            nextPrice = Math.min(first.getMaxBid(), this.currentPrice + first.getIncrement());
        } else {
            // 2 Bot cắn nhau -> Bắn giá thẳng lên kịch trần của Bot thua + 1 bước giá của Bot thắng
            nextPrice = Math.min(first.getMaxBid(), second.getMaxBid() + first.getIncrement());
        }

        // Chốt chặn an toàn cuối cùng
        if (nextPrice <= this.currentPrice) {
            autoBids.add(first);
            return false;
        }

        auction.model.users.User realUser = auction.model.AuctionManager.getInstance().getUserById(first.getBidder().getId());

        // Nếu user bị xóa, hoặc số dư trong ví không đủ để trả cái giá nextPrice này
        if (realUser == null || realUser.getBalance() < nextPrice) {
            System.out.println(" [AutoBid Cảnh báo] Đã hủy AutoBid của User " + first.getBidder().getId() + " do không đủ số dư (" + (realUser != null ? realUser.getBalance() : "null") + " VNĐ).");

            // Ngay lập tức gọi đệ quy để nhường quyền đấu giá cho Bot đứng thứ 2 (nếu có).
            return processAutoBids();
        }

        // Cập nhật thông tin Model nếu qua hết các chốt kiểm duyệt
        this.currentPrice = nextPrice;
        this.highestBidderId = first.getBidder().getId();

        if (this.state == AuctionState.OPEN) {
            this.state = AuctionState.RUNNING;
        }

        // Trả Bot dẫn đầu lại vào Priority Queue để chờ người khác vào đẩy giá
        autoBids.add(first);

        return true; // Trả về true báo hiệu vòng lặp while ở ngoài hãy chạy thêm 1 lần nữa để check chéo
    }
    public static String generateNewId() {
        return "I-" + (itemCounter++);
    }
}