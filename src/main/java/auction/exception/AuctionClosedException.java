package auction.exception;

/**
 * Ngoại lệ khi cố đặt giá vào phiên đã kết thúc (CLOSED hoặc SOLD).
 * Kế thừa InvalidBidException để code cũ dùng
 * catch (InvalidBidException e) vẫn hoạt động đúng.
 */
public class AuctionClosedException extends InvalidBidException {

  private static final long serialVersionUID = 101L;

  public AuctionClosedException(String message) {
    super(message);
  }

  public AuctionClosedException(String message, Throwable cause) {
    super(message, cause);
  }
}