package auction.model.users;

import auction.model.Entity;

public abstract class User extends Entity {
    private String username;
    private String password; // Encapsulation: sử dụng private

    public User(String id, String username, String password) {
        super(id);
        this.username = username;
        this.password = password;
    }

    // Getter và Setter để truy cập thuộc tính private
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

}