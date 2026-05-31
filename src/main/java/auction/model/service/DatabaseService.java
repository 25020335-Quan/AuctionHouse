package auction.model.service;

import auction.model.AuctionManager;
import auction.model.factory.FactoryProvider;
import auction.model.item.Item;
import auction.model.state.AuctionState;
import auction.model.transaction.BidTransaction;
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

    public boolean deductBalance(String id, double amount) {
        // Thêm chốt chặn "balance >= ?" để chống âm tiền
        String sql = "UPDATE users SET balance = balance - ? WHERE id = ? AND balance >= ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDouble(1, amount);
            pstmt.setString(2, id);
            pstmt.setDouble(3, amount); //  Đưa amount vào dấu ? thứ 3

            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("[Database] Trừ thành công " + amount + " từ user ID: " + id);
                return true; // Thành công
            } else {
                System.out.println("[Database] Trừ tiền thất bại: User không tồn tại hoặc không đủ tiền!");
                return false; // Thất bại
            }

        } catch (SQLException e) {
            System.err.println("[Database] Lỗi trừ tiền: " + e.getMessage());
            return false;
        }
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
    // Hàm phục vụ Anti-Sniping để lưu thời gian kết thúc mới
    public boolean updateBidAndExtendTimer(String itemId, double newPrice, String bidderId, java.time.LocalDateTime newEndTime) {
        String sql = "UPDATE items SET current_price = ?, highest_bidder_id = ?, end_time = ? WHERE id = ?";
        try (java.sql.Connection conn = DBConnection.getConnection();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDouble(1, newPrice);
            pstmt.setString(2, bidderId);
            // Ép từ LocalDateTime sang Timestamp để MySQL hiểu được
            pstmt.setTimestamp(3, java.sql.Timestamp.valueOf(newEndTime));
            pstmt.setString(4, itemId);

            return pstmt.executeUpdate() > 0;
        } catch (java.sql.SQLException e) {
            System.err.println("[Database] Lỗi gia hạn thời gian: " + e.getMessage());
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

    public void addTransaction(BidTransaction transaction) {
        // SQL statement targeting the bid_transactions table structure
        String sql = "INSERT INTO bid_transactions (id, bidder_id, item_id, bid_amount, bid_time) VALUES (?, ?, ?, ?, ?)";

        // Try-with-resources closes connection & statement automatically to avoid memory leaks
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // 1. Get the transaction ID inherited from the Entity parent class
            pstmt.setString(1, transaction.getId());

            // 2. Get bidderId and itemId from your object fields
            pstmt.setString(2, transaction.getBidderId()); //
            pstmt.setString(3, transaction.getItemId());   //

            // 3. Get the raw numeric bid amount
            pstmt.setDouble(4, transaction.getBidAmount()); //

            // 4. Pass LocalDateTime directly into the MySQL DATETIME column safely
            pstmt.setObject(5, transaction.getBidTime());   //

            // Execute the insertion query
            int rowsInserted = pstmt.executeUpdate();

            if (rowsInserted > 0) {
                System.out.println("[Database] Successfully uploaded transaction record: " + transaction.getId());
            }

        } catch (SQLException e) {
            System.err.println("[Database] Failed to upload transaction details: " + e.getMessage());
        }
    }

    public void loadAllTransactionsToManager() {
        String sql = "SELECT id, bidder_id, item_id, bid_amount, bid_time FROM bid_transactions";
        List<BidTransaction> bidTransactionList = AuctionManager.getInstance().getAllTransaction();
        // Using try-with-resources to clean up database connections automatically
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            // Clear the existing transactions list first if you want to avoid duplicates on reload
            AuctionManager.getInstance().getAllTransaction().clear();

            int count = 0;
            while (rs.next()) {
                String id = rs.getString("id");
                String bidderId = rs.getString("bidder_id");
                String itemId = rs.getString("item_id");
                double bidAmount = rs.getDouble("bid_amount");

                // Read the historical DATETIME from MySQL into LocalDateTime
                LocalDateTime bidTime = rs.getObject("bid_time", LocalDateTime.class);

                // 1. Instantiate the object using your constructor
                BidTransaction transaction = new BidTransaction(id, bidderId, itemId, bidAmount); //

                // 2. IMPORTANT: Override the auto-generated "now()" time with the real historical database time
                // (Ensure you have a setter 'setBidTime(LocalDateTime bidTime)' in your BidTransaction class)
                if (bidTime != null) {
                    transaction.setBidTime(bidTime);
                }

                // 3. Push the populated object into the AuctionManager collection
                bidTransactionList.add(transaction);
                count++;
            }
            AuctionManager.getInstance().updateAllTransaction(bidTransactionList);
            System.out.println("[Database] Successfully loaded " + count + " transactions into AuctionManager.");

        } catch (SQLException e) {
            System.err.println("[Database] Failed to load transaction logs: " + e.getMessage());
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
                double startingPrice = rs.getDouble("starting_price");
                LocalDateTime startTime = rs.getObject("start_time", LocalDateTime.class);
                LocalDateTime endTime = rs.getObject("end_time", LocalDateTime.class);
                String highestBidderId = rs.getString("highest_bidder_id");

                // Tạo đối tượng Item (Hãy đảm bảo Constructor của Item nhận các tham số này)
                Item item = FactoryProvider.createItemByType(type, id, ownerId, name, price);
                item.setStartingPrice(startingPrice);
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

    public boolean addBalance(String userId, double amount) {
        // Câu lệnh SQL: Lấy số tiền cũ (balance) cộng thêm một khoản mới (?) của đúng người đó
        String sql = "UPDATE users SET balance = balance + ? WHERE id = ?";

        // Dùng try-with-resources để tự động đóng kết nối (Connection) sau khi xong việc, tránh tràn RAM
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Nhét dữ liệu vào 2 dấu chấm hỏi (?) ở câu lệnh SQL trên
            pstmt.setDouble(1, amount); // Dấu hỏi 1: Số tiền nạp
            pstmt.setString(2, userId); // Dấu hỏi 2: ID của người dùng

            // Thực thi lệnh và đếm xem có bao nhiêu dòng trong Database bị thay đổi
            int rowsAffected = pstmt.executeUpdate();

            // Nếu > 0 nghĩa là đã tìm thấy user và cộng tiền thành công
            if (rowsAffected > 0) {
                System.out.println("[Database] Nạp thành công " + amount + " VNĐ vào tài khoản ID: " + userId);
                return true;
            } else {
                System.out.println("[Database] Lỗi nạp tiền: Không tìm thấy User có ID là " + userId);
                return false;
            }

        } catch (SQLException e) {
            System.err.println("[Database] Lỗi SQL khi nạp tiền: " + e.getMessage());
            return false;
        }
    }
    public boolean updateItemDetails(Item item) {
        // Cập nhật lại Tên, Giá, Mô tả và toàn bộ chuỗi Link Ảnh
        String sql = "UPDATE items SET name = ?, current_price = ?, description = ?, image_urls = ? WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, item.getName());
            pstmt.setDouble(2, item.getCurrentPrice());
            pstmt.setString(3, item.getDescription());

            // Biến danh sách (List) link ảnh thành 1 chuỗi dài ngăn cách bởi dấu phẩy để lưu vào Database
            java.util.List<String> links = item.getImageUrls();
            if (links != null && !links.isEmpty()) {
                pstmt.setString(4, String.join(",", links));
            } else {
                pstmt.setString(4, ""); // Nếu xóa hết ảnh thì lưu chuỗi rỗng
            }

            pstmt.setString(5, item.getId());

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("[Database] Đã lưu cập nhật cho sản phẩm: " + item.getId());
                return true;
            }

        } catch (SQLException e) {
            System.err.println("[Database] Lỗi khi cập nhật sản phẩm: " + e.getMessage());
        }
        return false;
    }
}