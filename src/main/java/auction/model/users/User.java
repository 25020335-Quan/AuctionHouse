package auction.model.users;

import auction.model.Entity;

public abstract class User extends Entity {
    private String username;
    private String password; // Encapsulation: sử dụng private
    private String email;

    public User(String id, String username, String password, String email) {
        super(id);
        this.username = username;
        this.password = password;
        this.email = email;
    }

    // Getter và Setter để truy cập thuộc tính private
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email;}

}