package auction.model.service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    // Mặc định của XAMPP là để trống

    public static Connection getConnection() throws SQLException {
        String host = "bjsa1yg2p28c1jctzcft-mysql.services.clever-cloud.com";
        String database = "bjsa1yg2p28c1jctzcft";
        String user = "udappkqij1y85myz";
        String password = "xR89VlU5JKGhvSsFOG97";

        String url = "jdbc:mysql://" + host + ":3306/" + database + "?useSSL=false&allowPublicKeyRetrieval=true";

        Connection conn = DriverManager.getConnection(url, user, password);

        // Dòng này xác nhận trạng thái kết nối thành công
//        System.out.println("[Database] Kết nối thành công tới Clever Cloud DB: " + database);

        return conn;
    }
}
