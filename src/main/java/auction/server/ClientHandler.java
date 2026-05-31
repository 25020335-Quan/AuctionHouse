package auction.server;

import auction.model.AuctionManager;
import auction.model.item.Item;
import auction.model.service.DatabaseService;
import auction.model.transaction.BidTransaction;
import auction.model.users.User;
import auction.util.*;
import auction.model.notification.BidNotification;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

// Kế thừa Thread chạy song song
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

                // Logic xử lý
                if (request instanceof RegisterRequest) {
                    RegisterRequest regReq = (RegisterRequest) request;
                    User newUser = regReq.getUser();

                    newUser.setId(auction.model.users.User.generateNewId());

                    // Gọi DB lưu lại
                    auction.model.service.DatabaseService dbService = new auction.model.service.DatabaseService();
                    User savedUser = dbService.addUser(newUser);

                    if (savedUser != null) {
                        sendMessage("SUCCESS");
                    } else {
                        sendMessage("FAILED");
                    }

                }
                else if (request instanceof LoginRequest loginData) {
                    try {
                        User user = dbService.checkLogin(loginData.getUsername(), loginData.getPassword());
                        this.sendMessage(user);
                    } catch (Exception e) {
                        this.sendMessage("LỖI: " + e.getMessage());
                    }

                } else if (request instanceof AddItemRequest itemData) {
                    itemData.getItem().setId(auction.model.item.Item.generateNewId());
                    dbService.addItem(itemData.getItem());
                    AuctionManager.getInstance().addItem(itemData.getItem());
                    this.sendMessage(itemData.getItem());
                    System.out.println("Server: Đang xử lý thêm đồ: " + itemData.getItem().getName());
                    AuctionServer.broadcast(new NotificationRequest("New items up for bids!: " + itemData.getItem().getName()));

                } else if (request instanceof GetItemListRequest) {
                    // Lấy kho mới nhất gửi về Client
                    List<Item> currentItems = AuctionManager.getInstance().getAllItems();
                    this.sendMessage(new ArrayList<>(currentItems));

                } else if (request instanceof GetItemHistoryRequest) {
                    GetItemHistoryRequest req = (GetItemHistoryRequest) request;
                    List<BidTransaction> history = AuctionManager.getInstance().getHistoryByItem(req.getItemId());
                    this.sendMessage(new java.util.ArrayList<>(history));

                } else if (request instanceof auction.util.BidRequest bidReq) {
                    try {
                        Item item = null;
                        for (Item i : AuctionManager.getInstance().getAllItems()) {
                            if (i.getId().equals(bidReq.getItemId())) {
                                item = i;
                                break;
                            }
                        }

                        if (item != null) {
                            // Chạy đấu giá (Bao gồm cả đặt tay và kích hoạt Bot bên trong)
                            AuctionManager.getInstance().attemptBid(item, bidReq.getBidderId(), bidReq.getAmount());
                            // Trả về báo cáo thành công cho người đặt
                            this.sendMessage("SUCCESS");
                            // Lấy thông tin TỪ ITEM

                        } else {
                            this.sendMessage("LỖI: Không tìm thấy sản phẩm!");
                        }
                    } catch (auction.exception.InvalidBidException e) {
                        this.sendMessage("LỖI:" + e.getMessage());
                    }
                }
                else if (request instanceof auction.util.AutoBidRequest autoReq) {
                    auction.model.item.Item item = auction.model.AuctionManager.getInstance().getItemById(autoReq.getItemId());
                    auction.model.users.User member = auction.model.AuctionManager.getInstance().getUserById(autoReq.getUserId());

                    if (item != null && member != null) {
                        synchronized (item) {
                            auction.model.AutoBid newAutoBid = new auction.model.AutoBid(member, autoReq.getMaxBid(), autoReq.getIncrement());

                            // Lưu lại giá cũ để so sánh

                            // Thêm vào hàng đợi của món đồ
                            item.addAutoBid(newAutoBid);
                            sendMessage("AUTOBID_SUCCESS"); // Báo về cho Client
                            // Nếu bot làm giá tăng lên, lưu lịch sử và thông báo!
                        }

                    } else {
                        sendMessage("LỖI: Không tìm thấy sản phẩm/người dùng!");
                    }
                }
            }

        } catch (Exception e) {
            System.out.println("Client đã ngắt kết nối.");
        }
    }

    public synchronized void sendMessage(Object message) {
        try {
            out.writeObject(message);
            out.flush();
            out.reset();
        } catch (IOException e) {
            AuctionServer.removeClient(this); // Xóa nếu client ngắt kết nối
        }
    }
}