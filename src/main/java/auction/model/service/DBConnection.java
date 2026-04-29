package auction.model.service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/auction_house?useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=UTC";
    private static final String USER = "root"; // Mặc định của XAMPP
    private static final String PASS = "";     // Mặc định của XAMPP là để trống

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }
}
