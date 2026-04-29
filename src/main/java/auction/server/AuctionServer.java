package auction.server;

import java.io.*;
import java.net.*;

public class AuctionServer {
    private static final int PORT = 1234;

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server đang chạy tại cổng " + PORT + "...");

            while (true) {
                // Chấp nhận kết nối từ Client
                Socket clientSocket = serverSocket.accept();
                System.out.println("Có Client mới kết nối: " + clientSocket.getInetAddress());

                // Tạo luồng riêng để xử lý Client này (Multi-threading)
                new ClientHandler(clientSocket).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}