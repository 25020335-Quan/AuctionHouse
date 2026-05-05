package auction.exception;

/**
 * Ngoại lệ xảy ra khi một thao tác (đặt giá, sửa đổi)
 */
public class AuctionClosedException extends Exception {

  // Đảm bảo tính ổn định khi gửi đối tượng Exception qua Socket
  private static final long serialVersionUID = 101L;

  public AuctionClosedException(String message) {
    super(message);
  }

  /**
   * Constructor hỗ trợ bọc lỗi (exception chaining)
   * Giúp giữ lại dấu vết nếu lỗi này sinh ra từ một lỗi khác
   */
  public AuctionClosedException(String message, Throwable cause) {
    super(message, cause);
  }
}