package auction.model;

/**
 * Lớp Admin (Quản trị viên) kế thừa từ User.
 */
public class Admin extends User {

    public Admin(String id, String username, String password) {
        super(id, username, password);
    }
}