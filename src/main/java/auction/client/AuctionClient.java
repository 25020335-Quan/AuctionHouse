package auction.client;

import auction.controller.AuctionRoomController;
import auction.controller.MainScreenController;
import auction.model.notification.BidNotification;
import auction.util.NotificationRequest;
import javafx.application.Platform;

import java.io.*;
import java.net.Socket;

public class AuctionClient {
    private static AuctionClient instance;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    // Biến lưu kết quả của các Request bình thường
    private Object currentResponse = null;

    // Lưu màn hình để luồng ngầm tự động vào cập nhật UI
    private AuctionRoomController currentRoom = null;
    private MainScreenController mainScreen = null;

    private AuctionClient() {
        try {
            // Kết nối tới IP của Server (localhost) và Port 1234
            socket = new Socket("localhost", 1234);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            Thread listenerThread = new Thread(() -> {
                try {
                    while (true) {
                        Object response = in.readObject();

                        // Trạng thái A: Nhận được tin báo có người Bid (Realtime)
                        if (response instanceof BidNotification notify) {
                            // Cập nhật giá mới cho món đồ trong kho Client
                            for (auction.model.item.Item i : auction.model.AuctionManager.getInstance().getAllItems()) {
                                if (i.getId().equals(notify.getItemId())) {
                                    i.setPrice(notify.getNewPrice());
                                    i.setHighestBidderId(notify.getHighestBidderId());
                                    i.setEndTime(notify.getNewEndTime());
                                    break;
                                }
                            }

                            // Ghi lại lịch sử giao dịch này vào kho Client để lưu biểu đồ
                            String txId = "TX-CLIENT-" + System.currentTimeMillis();
                            auction.model.transaction.BidTransaction clientTx = new auction.model.transaction.BidTransaction(
                                    txId, notify.getHighestBidderId(), notify.getItemId(), notify.getNewPrice()
                            );
                            auction.model.AuctionManager.getInstance().addTransaction(clientTx);

                            // Cập nhật lên màn hình
                            Platform.runLater(() -> {
                                // Nếu đang đứng đúng phòng của món đồ đó -> Cập nhật giờ và vẽ biểu đồ
                                if (currentRoom != null && currentRoom.getCurrentItem() != null
                                        && currentRoom.getCurrentItem().getId().equals(notify.getItemId())) {

                                    // Đồng bộ thời gian nếu có Anti-Sniping
                                    currentRoom.getCurrentItem().setEndTime(notify.getNewEndTime());
                                    // Cập nhật giá và biểu đồ
                                    currentRoom.refreshPriceFromNetwork(notify.getHighestBidderId(), notify.getNewPrice());
                                }
                                // nếu đã đăng nhập và vào được main screen -> xử lý UI
                                if (mainScreen != null && mainScreen.getCurrentUser() != null) {
                                    String myUserId = mainScreen.getCurrentUser().getId();
                                    if (!notify.getHighestBidderId().equals(myUserId)) {
                                        // Kiểm tra xem mình có đang theo dõi món này không?
                                        // Mình là chủ món đồ hoặc mình đã từng tham gia bid món này
                                        boolean isMyItem = checkIsMyItem(notify.getItemId(), myUserId);
                                        boolean didIBid = checkDidIBid(notify.getItemId(), myUserId);
                                        if (isMyItem) {
                                            mainScreen.showToast("Your item: " + notify.getItemId() + "just got a new bid: " + notify.getNewPrice());
                                            mainScreen.addNotification("Your item: " + notify.getItemId() +" just got a new bid: " + notify.getNewPrice());
                                        } else if (didIBid) {
                                            mainScreen.showToast("Outbid! Someone just placed a higher bid of: " + notify.getNewPrice() + "for item " + notify.getItemId() );
                                            mainScreen.addNotification("Outbid! Someone just placed a higher bid of: " + notify.getNewPrice() + "for item " + notify.getItemId());
                                        }
                                    } else {
                                        String msg = "✅ You have successfully placed a bid of: " + String.format("%,.0f VNĐ", notify.getNewPrice()) + "for item " + notify.getItemId();
                                        mainScreen.showToast(msg);        // Hiện Toast xanh lá báo thành công
                                        mainScreen.addNotification(msg); // Lưu vào hộp thư
                                    }
                                    //Load lại mainscreen
                                    if (mainScreen != null) {
                                        mainScreen.refreshLocalUI();
                                    }
                                }
                            });
                        }
                        // Trạng thái B: Thông báo hệ thống
                        else if (response instanceof NotificationRequest notifyReq) {
                            javafx.application.Platform.runLater(() -> {
                                if (mainScreen != null) {
                                    String msg = "🆕 " + notifyReq.getMsg();
                                    mainScreen.showToast(msg);        // hiện thông báo lướt
                                    mainScreen.addNotification(msg); // lưu vào hộp thư
                                    mainScreen.refreshLocalUI();      // load lại danh sách sản phẩm
                                }
                            });
                        }

                        // Trạng thái C: Là kết quả trả về của một sendRequest bình thường
                        else {
                            synchronized (this) {
                                currentResponse = response;
                                this.notify(); // đánh thức thread khỏi wait() sau sendRequest()
                            }
                        }
                    }
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            });
            listenerThread.setDaemon(true); // Tự động chết khi tắt app
            listenerThread.start();         // Bật công tắc cho luồng chạy
        }catch (IOException e) {
            e.printStackTrace();
        }
    }
    //Singleton pattern -> Mỗi Socket chỉ có 1 đối tượng AuctionClient nối tới Server
    public static AuctionClient getInstance() {
        if (instance == null) instance = new AuctionClient();
        return instance;
    }

    // Hàm gửi yêu cầu và nhận phản hồi đồng bộ
    public synchronized Object sendRequest(Object request) throws IOException, ClassNotFoundException {
        // Dọn dẹp sạch sẽ kết quả từ lần gửi trước (nếu có)
        currentResponse = null;

        // Ném gói tin yêu cầu lên Server
        out.writeObject(request);
        out.flush();

        // Khi nào luồng ngầm ở trên lấy được kết quả, nó sẽ tự động gọi this.notify() để đánh thức hàm này dậy.
        try {
            while (currentResponse == null) {
                this.wait();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Luồng chờ phản hồi bị gián đoạn", e);
        }

        // Trả về kết quả mà luồng ngầm vừa đưa vào biến currentResponse
        return currentResponse;
    }

    public void setCurrentRoom(AuctionRoomController room) {
        this.currentRoom = room;
    }

    public void setMainScreen(MainScreenController screen) {
        this.mainScreen = screen;
    }


    //Kiểm tra xem món đồ có ID này có phải do chính mình đăng bán không
    private boolean checkIsMyItem(String itemId, String myUserId) {
        for (auction.model.item.Item item : auction.model.AuctionManager.getInstance().getAllItems()) {
            // Tìm đúng món đồ đó trong kho
            if (item.getId().equals(itemId)) {
                // So sánh xem ID người bán có trùng với ID của mình không
                return item.getOwnerId().equals(myUserId);
            }
        }
        return false; // Không tìm thấy đồ hoặc không phải của mình
    }

    //Kiểm tra xem mình đã từng đặt giá (bid) cho món đồ này lần nào chưa
    private boolean checkDidIBid(String itemId, String myUserId) {
        // Lôi toàn bộ lịch sử đấu giá của món đồ này ra
        java.util.List<auction.model.transaction.BidTransaction> history =
                auction.model.AuctionManager.getInstance().getHistoryByItem(itemId);

        // Quét từng BidTransaction
        for (auction.model.transaction.BidTransaction tx : history) {
            // nếu có lịch sử trùng -> return true
            if (tx.getBidderId().equals(myUserId)) {
                return true;
            }
        }
        return false; // Quét hết mà không có
    }
}