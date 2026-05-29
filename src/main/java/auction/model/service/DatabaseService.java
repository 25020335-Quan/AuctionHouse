package auction.model.service;

import auction.model.AuctionManager;
import auction.model.factory.FactoryProvider;
import auction.model.item.Item;
import auction.model.state.AuctionState;
import auction.model.users.Admin;
import auction.model.users.Member;
import auction.model.users.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;

public class DatabaseService {
    public User checkLogin(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";

        // Sử dụng try-with-resources để tự động đóng Connection & PreparedStatement
        try {
            Connection conn = DBConnection.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            pstmt.setString(2, password);

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                // Đọc dữ liệu từ dòng kết quả
                String id = rs.getString("id");
                String name = rs.getString("full_name");
                String role = rs.getString("role");
                String email = rs.getString("email_address");
                double balance = rs.getDouble("balance");

                // Khởi tạo đối tượng theo đúng vai trò (Polymorphism)
                if ("ADMIN".equals(role)) {
                    return new Admin(id, username, password, name, email, balance);
                } else {
                    return new Member(id, username, password, name, email, balance);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null; // Trả về null nếu không tìm thấy người dùng
    }

    public User addUser(User user) {
        String sql = "INSERT INTO users (id, username, password, full_name, role, email_address, balance) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, user.getId());
            pstmt.setString(2, user.getUsername());
            pstmt.setString(3, user.getPassword());
            pstmt.setString(4, user.getFullName());
            pstmt.setString(5, "MEMBER");
            pstmt.setString(6, user.getEmail());
            pstmt.setString(7, String.valueOf(user.getBalance()));

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                // Lấy ID vừa được tạo tự động từ Database gán lại cho đối tượng Item
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        // Nếu id trong DB là kiểu INT, dùng getInt, nếu là String dùng getString
                        user.setId(generatedKeys.getString(1));
                    }
                }
                System.out.println("Đã thêm user vào DB: " + user.getFullName());
                return user;
            }


        } catch (SQLException e) {
            System.err.println("Lỗi SQL khi thêm User: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public boolean updateHighestBid(String itemId, double newPrice, String bidderId) {
        // SQL statement targeting specific columns for a single row
        String sql = "UPDATE items SET current_price = ?, highest_bidder_id = ? WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Bind parameters in order of the question marks
            pstmt.setDouble(1, newPrice);
            pstmt.setString(2, bidderId);
            pstmt.setString(3, itemId);

            // executeUpdate() returns the number of rows modified
            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("[Database] Successfully updated bid for item: " + itemId);
                return true;
            } else {
                System.out.println("[Database] Failed to update. Item ID not found: " + itemId);
                return false;
            }

        } catch (SQLException e) {
            System.err.println("[Database] Error executing update: " + e.getMessage());
            return false;
        }
    }

    public boolean updateItemState(String itemId, String currentState) {
        // SQL statement targeting specific columns for a single row
        String sql = "UPDATE items SET state = ? WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Bind parameters in order of the question marks
            pstmt.setString(1, currentState);
            pstmt.setString(2, itemId);

            // executeUpdate() returns the number of rows modified
            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("[Database] Successfully updated state for item: " + itemId);
                return true;
            } else {
                System.out.println("[Database] Failed to update. Item ID not found: " + itemId);
                return false;
            }

        } catch (SQLException e) {
            System.err.println("[Database] Error executing update: " + e.getMessage());
            return false;
        }
    }

    public Item addItem(Item item) {
        // SQL: id thường tự tăng nên không cần chèn vào, state mặc định thường là 'OPEN'
        String sql = "INSERT INTO items (id, owner_id, name, current_price, state, type, description, starting_price, start_time, end_time, highest_bidder_id, image_urls) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, item.getId());
            pstmt.setString(2, item.getOwnerId());
            pstmt.setString(3, item.getName());
            pstmt.setDouble(4, item.getCurrentPrice());
            pstmt.setString(5, item.getState().name()); // Ví dụ: "OPEN" hoặc "ACTIVE"
            pstmt.setString(6, item.getItemType());
            pstmt.setString(7, item.getDescription());
            pstmt.setString(8, String.valueOf(item.getStartingPrice()));
            pstmt.setString(9, String.valueOf(item.getStartTime()));
            pstmt.setString(10, String.valueOf(item.getEndTime()));
            pstmt.setString(11, item.getHighestBidderId());

            // Join list link ảnh thành chuỗi để lưu
            java.util.List<String> links = item.getImageUrls();
            if (links != null && !links.isEmpty()) {
                pstmt.setString(12, String.join(",", links));
            } else {
                pstmt.setString(12, "");
            }

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                // Lấy ID vừa được tạo tự động từ Database gán lại cho đối tượng Item
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        // Nếu id trong DB là kiểu INT, dùng getInt, nếu là String dùng getString
                        item.setId(generatedKeys.getString(1));
                    }
                }
                System.out.println("Đã thêm sản phẩm vào DB: " + item.getName());
                return item;
            }
        } catch (SQLException e) {
            System.err.println("Lỗi SQL khi thêm Item: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public void deleteItemById(String itemId) {
        // SQL query using a placeholder (?) for security
        String sql = "DELETE FROM items WHERE id = ?";

        // Using try-with-resources to automatically close connections and prevent memory leaks
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Bind the itemId parameter to the first question mark
            pstmt.setString(1, itemId);

            // executeUpdate() returns the number of rows affected
            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("[Database] Successfully deleted item with ID: " + itemId);
            } else {
                System.out.println("[Database] No item found with ID: " + itemId);
            }

        } catch (SQLException e) {
            System.err.println("[Database] Error while deleting item: " + e.getMessage());
        }
    }

    public void loadAllItemsToManager() {
        // 1. Truy cập danh sách trong Singleton AuctionManager
        List<Item> managerList = AuctionManager.getInstance().getAllItems();
        managerList.clear(); // Xóa dữ liệu cũ để nạp mới hoàn toàn

        String sql = "SELECT id, owner_id, name, current_price, state, type, description, starting_price, start_time, end_time, highest_bidder_id, image_urls FROM items";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                // Đọc các giá trị từ ResultSet
                String id = rs.getString("id");
                String ownerId = rs.getString("owner_id");
                String name = rs.getString("name");
                double price = rs.getDouble("current_price");
                String state = rs.getString("state");
                String type = rs.getString("type");
                String desc = rs.getString("description");
                LocalDateTime startTime = rs.getObject("start_time", LocalDateTime.class);
                LocalDateTime endTime = rs.getObject("end_time", LocalDateTime.class);
                String highestBidderId = rs.getString("highest_bidder_id");

                // Tạo đối tượng Item (Hãy đảm bảo Constructor của Item nhận các tham số này)
                Item item = FactoryProvider.createItemByType(type, id, ownerId, name, price);
                item.setStartTime(startTime);
                item.setEndTime(endTime);
                item.setHighestBidderId(highestBidderId);
                item.setDescription(desc);

                assert item != null;
                item.setState(AuctionState.valueOf(state));

                String urlsFromDB = rs.getString("image_urls");
                if (urlsFromDB != null && !urlsFromDB.trim().isEmpty()) {
                    String[] urlArray = urlsFromDB.split(",");
                    for (String url : urlArray) {
                        item.addImageUrl(url.trim());
                    }
                }

                // Thêm vào danh sách quản lý
                managerList.add(item);
            }
            AuctionManager.getInstance().updateList(managerList);
            System.out.println("Đã nạp " + managerList.size() + " món đồ vào hệ thống.");

        } catch (SQLException e) {
            System.err.println("Lỗi khi load database: " + e.getMessage());
        }
    }

    public void printAllUsers() {
        String sql = "SELECT * FROM users";

        // Sử dụng try-with-resources để đảm bảo đóng kết nối an toàn theo chuẩn SOLID
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("\n--- DANH SÁCH NGƯỜI DÙNG TRONG DATABASE ---");
            System.out.printf("%-15s %-20s %-20s %-20s %-10s\n",
                    "ID", "Username", "Password", "Full Name", "Role");
            System.out.println("-----------------------------------------------------------------------------------------");

            while (rs.next()) {
                String id = rs.getString("id");
                String username = rs.getString("username");
                String password = rs.getString("password");
                String fullName = rs.getString("full_name");
                String role = rs.getString("role");

                // In định dạng theo cột để dễ theo dõi
                System.out.printf("%-15s %-20s %-20s %-20s %-10s\n",
                        id, username, password, fullName, role);
            }
            System.out.println("-----------------------------------------------------------------------------------------\n");

        } catch (SQLException e) {
            System.err.println("Lỗi khi truy vấn database: " + e.getMessage());
            e.printStackTrace();
        }
    }
    public Member getUserById(String userId) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String username = rs.getString("username"); // Hoặc full_name tùy bác
                    String password = rs.getString("password");
                    String fullName = rs.getString("full_name");
                    String email = rs.getString("email_address");
                    double balance = rs.getDouble("balance");

                    return new Member(userId, username, password, fullName, email, balance);
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi lấy UserById: " + e.getMessage());
        }
        return null;
    }
    public void syncItemCounter() {
        String sql = "SELECT id FROM items";
        int max = 0;
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String dbId = rs.getString("id");
                if (dbId != null && dbId.startsWith("I-")) {
                    try {
                        int num = Integer.parseInt(dbId.substring(2));
                        if (num > max) max = num;
                    } catch (Exception e) {}
                }
            }
            auction.model.item.Item.setItemCounter(max);
            System.out.println("Đã đồng bộ ID Item tiếp theo: I-" + (max + 1));
        } catch (SQLException e) {}
    }

    public void syncUserCounter() {
        String sql = "SELECT id FROM users";
        int max = 0;
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String dbId = rs.getString("id");
                if (dbId != null && dbId.startsWith("U-")) {
                    try {
                        int num = Integer.parseInt(dbId.substring(2));
                        if (num > max) max = num;
                    } catch (Exception e) {}
                }
            }
            auction.model.users.User.setUserCounter(max);
            System.out.println("Đã đồng bộ ID User tiếp theo: U-" + (max + 1));
        } catch (SQLException e) {}
    }
}