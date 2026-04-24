package auction.model;

public enum AuctionState {
    PENDING,    // Chờ duyệt
    OPEN,       // Đang mở, chưa ai đặt giá
    RUNNING,    // Đang có người tranh giá
    CLOSED,     // Đã kết thúc
    SOLD        // Đã bán thành công
}