package auction.exception;

/**
 * Ngoại lệ gốc cho mọi vi phạm quy tắc đặt giá.
 * AuctionClosedException kế thừa lớp này để catch (InvalidBidException)
 * vẫn bắt được cả hai loại lỗi.
 */
public class InvalidBidException extends Exception {

    private static final long serialVersionUID = 100L;

    public InvalidBidException(String message) {
        super(message);
    }

    public InvalidBidException(String message, Throwable cause) {
        super(message, cause);
    }
}