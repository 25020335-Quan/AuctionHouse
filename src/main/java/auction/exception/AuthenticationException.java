package auction.exception;

/**
 * Ngoại lệ xảy ra trong quá trình xác thực người dùng.
 * Ví dụ: Sai tên đăng nhập, sai mật khẩu hoặc tài khoản bị khóa.
 */
public class AuthenticationException extends Exception {

    // ID duy nhất phục vụ việc Serialization qua mạng
    private static final long serialVersionUID = 102L;

    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}