package auction;

import auction.server.AuctionServer;

public class Server {
    public static void main(String[] args) {
        AuctionServer server = new AuctionServer();
        server.start();
    }
}
