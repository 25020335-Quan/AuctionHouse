package auction.model.users;

import java.io.Serializable;

/**
 * Lớp Admin (Quản trị viên) kế thừa từ User.
 */
public class Admin extends User implements Serializable {

    public Admin(String id, String username, String password) {
        super(id, username, password);
    }
}