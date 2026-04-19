package auction.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Lớp Bidder (Người đặt giá) kế thừa từ User.
 */
public class Bidder extends User {
    public Bidder(String id, String username, String password) {
        super(id, username, password);
    }

    @Override
    public void displayRole() {
        System.out.println("[Vai trò] Người đấu giá: Tôi có thể tham gia đặt giá cho các sản phẩm.");
    }

}