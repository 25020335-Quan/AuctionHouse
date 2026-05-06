package auction.model.service;

import auction.model.item.Item;
import auction.model.users.Admin;
import auction.model.users.Member;
import auction.model.users.User;

import java.sql.*;

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

                // Khởi tạo đối tượng theo đúng vai trò (Polymorphism)
                if ("ADMIN".equals(role)) {
                    return new Admin(id, name, password);
                } else {
                    return new Member(id, name, password);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null; // Trả về null nếu không tìm thấy người dùng
    }

    public Item addItem(Item item) {
        // SQL: id thường tự tăng nên không cần chèn vào, state mặc định thường là 'OPEN'
        String sql = "INSERT INTO items (id, name, current_price, state) VALUES (?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, item.getName());
            pstmt.setDouble(2, item.getCurrentPrice());
            pstmt.setString(3, item.getState().name()); // Ví dụ: "OPEN" hoặc "ACTIVE"

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
}
