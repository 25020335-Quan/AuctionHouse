package auction.server;

import java.io.*;
import java.net.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class AuctionServer {
    private static final int PORT = 1234;
    private static List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    public void start() {
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