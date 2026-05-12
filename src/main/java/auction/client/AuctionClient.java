package auction.client;

import auction.util.NotificationRequest;
import javafx.application.Platform;

import java.io.*;
import java.net.Socket;

public class AuctionClient {
    private static AuctionClient instance;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    private AuctionClient() {
        try {
            // Kết nối tới IP của Server (localhost) và Port 1234
            socket = new Socket("10.11.64.219", 1234);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static AuctionClient getInstance() {
        if (instance == null) instance = new AuctionClient();
        return instance;
    }

    // Hàm gửi yêu cầu và nhận phản hồi đồng bộ
    public synchronized Object sendRequest(Object request) throws IOException, ClassNotFoundException {
        out.writeObject(request);
        out.flush();
        return in.readObject();
    }
}