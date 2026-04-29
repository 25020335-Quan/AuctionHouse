package auction.client;

import java.io.*;
import java.net.Socket;

public class AuctionClient {
    private static AuctionClient instance;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    private AuctionClient() {
        try {
            // Kết nối tới IP của Server (localhost) và Port 25565
            socket = new Socket("localhost", 1234);
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