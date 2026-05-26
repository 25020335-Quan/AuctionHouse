package auction.server;

import auction.model.AuctionManager;
import auction.model.item.Art;
import auction.model.item.Item;
import auction.model.state.AuctionState;

import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class AuctionServer {
    private static final int PORT = 1234;
    private static List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    public void start() {
        try {
            auction.model.service.DatabaseService dbService = new auction.model.service.DatabaseService();
            dbService.loadAllItemsToManager();
            System.out.println("[Server] Đã tải toàn bộ dữ liệu từ Database vào kho tổng!");
        } catch (Exception e) {
            System.out.println("Lỗi load Database: " + e.getMessage());
        }
        try {
            Item mockItem = new Art("TEST-999", "363636", "Đồng hồ Rolex (Test)", 100000);
            mockItem.setStartTime(LocalDateTime.now());

            mockItem.setEndTime(LocalDateTime.now().plusSeconds(120));
            mockItem.setState(AuctionState.RUNNING);

            // Bơm vào kho lưu trữ trung tâm của Server
            AuctionManager.getInstance().addItem(mockItem);
            System.out.println("[Server] Đã khởi tạo thành công sản phẩm test: TEST-999");
        } catch (Exception e) {
            System.out.println("Lỗi tạo đồ test: " + e.getMessage());
        }
        // =========================================================



        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server đang chạy tại cổng " + PORT + "...");

            while (true) {
                // Chấp nhận kết nối từ Client
                Socket clientSocket = serverSocket.accept();
                System.out.println("Có Client mới kết nối: " + clientSocket.getInetAddress());
                ClientHandler handler = new ClientHandler(clientSocket);
                // Tạo luồng riêng để xử lý Client này (Multi-threading)
                clients.add(handler);
                handler.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void broadcast(Object message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    public static void removeClient(ClientHandler handler) {
        clients.remove(handler);
    }
}