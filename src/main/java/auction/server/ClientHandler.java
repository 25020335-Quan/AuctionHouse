package auction.server;

import auction.model.AuctionManager;
import auction.model.item.Item;
import auction.model.service.DatabaseService;
import auction.model.users.User;
import auction.util.AddItemRequest;
import auction.util.GetItemListRequest;
import auction.util.LoginRequest;
import auction.util.NotificationRequest;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class ClientHandler extends Thread {
    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private DatabaseService dbService;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.dbService = new DatabaseService();
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            while (true) {
                // Đọc đối tượng yêu cầu từ Client (Serialization)
                Object request = in.readObject();

                // Logic xử lý (Ví dụ: Kiểm tra đăng nhập hoặc Đấu giá)
                if (request instanceof LoginRequest loginData) {
                    User user = dbService.checkLogin(loginData.getUsername(), loginData.getPassword());
                    out.writeObject(user);
                } else if (request instanceof AddItemRequest itemData) {
                    AuctionServer.broadcast(new NotificationRequest("Có sản phẩm mới: " + itemData.getItem().getName()));
                    dbService.addItem(itemData.getItem());
                    System.out.println("Server: Đang xử lý thêm đồ: " + itemData.getItem().getName());;
                    out.writeObject(itemData.getItem());
                    out.flush();

                } else if (request instanceof GetItemListRequest) {
                    // 1. Server gọi hàm đọc từ DB vào AuctionManager (chỉ máy Server mới làm việc này)
                    dbService.loadAllItemsToManager();

                    // 2. Lấy danh sách từ Manager
                    List<Item> currentItems = AuctionManager.getInstance().getAllItems();

                    // 3. Gửi danh sách này ngược lại cho Client
                    // Gói vào ArrayList để đảm bảo Serializable hoạt động tốt
                    out.writeObject(new ArrayList<>(currentItems));
                    out.flush();
                }

                // Gửi kết quả về Client
                out.flush();
            }
        } catch (Exception e) {
            System.out.println("Client đã ngắt kết nối.");
        }
    }

    public void sendMessage(Object message) {
        try {
            out.writeObject(message);
            out.flush();
        } catch (IOException e) {
            AuctionServer.removeClient(this); // Xóa nếu client ngắt kết nối
        }
    }
}
