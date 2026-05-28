package auction.exception;

/**
 * Ngoại lệ xảy ra khi người dùng đặt giá không hợp lệ.
 * Ví dụ: giá thấp hơn giá hiện tại, đặt giá khi phiên đã đóng,
 * người bán tự đấu giá đồ của mình.
 */
public class InvalidBidException extends Exception {

    // Đảm bảo tính ổn định khi gửi đối tượng Exception qua Socket
    private static final long serialVersionUID = 100L;

    public InvalidBidException(String message) {
        super(message);
    }

    /**
     * Constructor hỗ trợ bọc lỗi (exception chaining).
     * Giúp giữ lại dấu vết nếu lỗi này sinh ra từ một lỗi khác.
     */
    public InvalidBidException(String message, Throwable cause) {
        super(message, cause);
    }
}