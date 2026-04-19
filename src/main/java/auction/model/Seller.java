package auction.model;

/**
 * Lớp Seller (Người bán) kế thừa từ User.
 */
public class Seller extends User {

    public Seller(String id, String username, String password) {
        super(id, username, password);
    }

    @Override
    public void displayRole() {
        System.out.println("[Vai trò] Người bán: Tôi có thể đăng sản phẩm và quản lý phiên đấu giá của mình.");
    }
}