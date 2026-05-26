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
                                //Load lại mainscreen
                                if (mainScreen != null) {
                                     mainScreen.refreshLocalUI();
                                }
                            });
                        }
                        // Trạng thái B: Thông báo hệ thống
                        else if (response instanceof NotificationRequest notifyReq) {
                            System.out.println("Hệ thống: " + notifyReq.getMsg());
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
}