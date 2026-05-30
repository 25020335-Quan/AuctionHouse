package auction.model;

import auction.exception.AuctionClosedException;
import auction.exception.InvalidBidException;
import auction.model.factory.FactoryProvider;
import auction.model.item.Item;
import auction.model.state.AuctionState;
import auction.model.transaction.BidTransaction;
import auction.model.users.Member;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

public class AuctionManagerTest {

    private AuctionManager manager;
    private Item item;
    private Member seller;
    private Member bidder;

    @BeforeEach
    void setUp() {
        manager = AuctionManager.getInstance();
        seller  = new Member("S01", "seller", "pass", "Seller Test", "seller@test.com");
        bidder  = new Member("B01", "bidder", "pass", "Bidder Test", "bidder@test.com");
        item    = FactoryProvider.createItemByType(
                "ELECTRONICS", "I_" + System.nanoTime(), "S01", "Test Item", 1000.0);
        seller.postItem(item); // PENDING -> OPEN
        assertNotNull(item, "FactoryProvider không được trả null");
    }

    // --- Singleton ---

    @Test
    @DisplayName("getInstance() luôn trả về cùng một đối tượng")
    void testSingletonInstance() {
        assertSame(manager, AuctionManager.getInstance());
    }

    // --- Quản lý sản phẩm ---

    @Test
    @DisplayName("addItem() tăng danh sách thêm 1")
    void testAddItem() {
        int before = manager.getAllItems().size();
        manager.addItem(item);
        assertEquals(before + 1, manager.getAllItems().size());
    }

    @Test
    @DisplayName("getAllItems() trả về bản sao, không ảnh hưởng list gốc")
    void testGetAllItems_ReturnsDefensiveCopy() {
        manager.addItem(item);
        List<Item> copy = manager.getAllItems();
        int before = copy.size();
        copy.clear();
        assertEquals(before, manager.getAllItems().size());
    }

    // --- Bid hợp lệ ---

    @Test
    @DisplayName("Bid hợp lệ: giá tăng và trạng thái RUNNING")
    void testAttemptBid_Valid() throws InvalidBidException {
        manager.attemptBid(item, bidder.getId(), 1500.0);
        assertEquals(1500.0, item.getCurrentPrice(), 0.001);
        assertEquals(AuctionState.RUNNING, item.getState());
    }

    @Test
    @DisplayName("Bid hợp lệ: giao dịch được ghi vào lịch sử")
    void testAttemptBid_TransactionRecorded() throws InvalidBidException {
        int before = manager.getHistoryByItem(item.getId()).size();
        manager.attemptBid(item, bidder.getId(), 1200.0);
        assertEquals(before + 1, manager.getHistoryByItem(item.getId()).size());
    }

    // --- Bid không hợp lệ ---

    @Test
    @DisplayName("Giá bằng giá hiện tại -> InvalidBidException")
    void testAttemptBid_EqualPrice_Throws() {
        assertThrows(InvalidBidException.class,
                () -> manager.attemptBid(item, bidder.getId(), 1000.0));
    }

    @Test
    @DisplayName("Giá thấp hơn giá hiện tại -> InvalidBidException")
    void testAttemptBid_LowerPrice_Throws() {
        assertThrows(InvalidBidException.class,
                () -> manager.attemptBid(item, bidder.getId(), 500.0));
    }

    @Test
    @DisplayName("Chủ sở hữu tự đặt giá -> InvalidBidException")
    void testAttemptBid_OwnerBid_Throws() {
        assertThrows(InvalidBidException.class,
                () -> manager.attemptBid(item, seller.getId(), 2000.0));
    }

    @Test
    @DisplayName("Phiên PENDING -> InvalidBidException")
    void testAttemptBid_Pending_Throws() {
        Item pending = FactoryProvider.createItemByType(
                "ART", "PEND_" + System.nanoTime(), "S01", "Art", 500.0);
        assertNotNull(pending, "FactoryProvider không được trả null");
        assertEquals(AuctionState.PENDING, pending.getState());
        assertThrows(InvalidBidException.class,
                () -> manager.attemptBid(pending, bidder.getId(), 600.0));
    }

    // --- AuctionClosedException ---

    @Test
    @DisplayName("Phiên CLOSED -> ném AuctionClosedException (không phải InvalidBidException thường)")
    void testAttemptBid_Closed_ThrowsAuctionClosedException() {
        item.setState(AuctionState.CLOSED);

        // Phải là AuctionClosedException cụ thể, không phải lớp cha chung
        AuctionClosedException ex = assertThrows(AuctionClosedException.class,
                () -> manager.attemptBid(item, bidder.getId(), 2000.0));

        assertEquals("Phiên đấu giá đã kết thúc!", ex.getMessage());
    }

    @Test
    @DisplayName("Phiên SOLD -> ném AuctionClosedException")
    void testAttemptBid_Sold_ThrowsAuctionClosedException() {
        item.setState(AuctionState.SOLD);
        assertThrows(AuctionClosedException.class,
                () -> manager.attemptBid(item, bidder.getId(), 2000.0));
    }

    @Test
    @DisplayName("AuctionClosedException là subclass của InvalidBidException")
    void testAuctionClosedException_IsInvalidBidException() {
        item.setState(AuctionState.CLOSED);

        // catch (InvalidBidException) vẫn bắt được AuctionClosedException
        assertThrows(InvalidBidException.class,
                () -> manager.attemptBid(item, bidder.getId(), 2000.0));
    }

    // --- Bid thất bại không làm thay đổi trạng thái ---

    @Test
    @DisplayName("Bid thất bại không thay đổi giá và trạng thái")
    void testAttemptBid_Failed_NoStateChange() throws InvalidBidException {
        manager.attemptBid(item, bidder.getId(), 1500.0);
        double priceBefore = item.getCurrentPrice();

        assertThrows(InvalidBidException.class,
                () -> manager.attemptBid(item, bidder.getId(), 1000.0));

        assertEquals(priceBefore, item.getCurrentPrice(), 0.001);
        assertEquals(AuctionState.RUNNING, item.getState());
    }

    // --- Lịch sử giao dịch ---

    @Test
    @DisplayName("getHistoryByItem() chỉ trả về giao dịch của đúng item đó")
    void testGetHistoryByItem_NotMixed() throws InvalidBidException {
        Item other = FactoryProvider.createItemByType(
                "VEHICLE", "OTHER_" + System.nanoTime(), "S01", "Car", 5000.0);
        assertNotNull(other, "FactoryProvider không được trả null");
        seller.postItem(other);

        manager.attemptBid(item,  bidder.getId(), 1200.0);
        manager.attemptBid(item,  bidder.getId(), 1400.0);
        manager.attemptBid(other, bidder.getId(), 6000.0);

        List<BidTransaction> history = manager.getHistoryByItem(item.getId());
        assertEquals(2, history.size());
        for (BidTransaction tx : history) {
            assertEquals(item.getId(), tx.getItemId());
        }
    }

    @Test
    @DisplayName("getCurrentMaxPrice() trả về giá cao nhất đã bid")
    void testGetCurrentMaxPrice() throws InvalidBidException {
        manager.attemptBid(item, bidder.getId(), 1200.0);
        manager.attemptBid(item, bidder.getId(), 1800.0);
        assertEquals(1800.0, manager.getCurrentMaxPrice(item.getId()), 0.001);
    }

    @Test
    @DisplayName("getCurrentMaxPrice() trả về 0.0 nếu chưa có bid")
    void testGetCurrentMaxPrice_NoBids_ReturnsZero() {
        Item fresh = FactoryProvider.createItemByType(
                "ART", "FRESH_" + System.nanoTime(), "S01", "Fresh", 500.0);
        assertNotNull(fresh, "FactoryProvider không được trả null");
        assertEquals(0.0, manager.getCurrentMaxPrice(fresh.getId()), 0.001);
    }

    // --- Đa luồng: nhiều người bid cùng lúc ---

    @Test
    @DisplayName("20 thread bid cùng giá 1001 -> chỉ đúng 1 thread thắng")
    void testConcurrentBid_SamePrice_OnlyOneWins() throws InterruptedException {
        AtomicInteger success = new AtomicInteger(0);
        AtomicInteger fail    = new AtomicInteger(0);
        Thread[] threads = new Thread[20];

        for (int i = 0; i < 20; i++) {
            final String id = "BIDDER_" + i;
            threads[i] = new Thread(() -> {
                try {
                    manager.attemptBid(item, id, 1001.0);
                    success.incrementAndGet();
                } catch (InvalidBidException e) {
                    fail.incrementAndGet();
                }
            });
        }
        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        assertEquals(1, success.get(), "Chỉ 1 thread được đặt giá thành công");
        assertEquals(19, fail.get(),   "19 thread còn lại bị từ chối");
        assertEquals(1001.0, item.getCurrentPrice(), 0.001);
    }

    @Test
    @DisplayName("Số giao dịch phải bằng số bid thành công")
    void testConcurrentBid_TransactionCount() throws InterruptedException {
        AtomicInteger success = new AtomicInteger(0);
        Thread[] threads = new Thread[10];
        int before = manager.getHistoryByItem(item.getId()).size();

        for (int i = 0; i < 10; i++) {
            final double price = 1001.0 + i * 200;
            final String id    = "TX_BIDDER_" + i;
            threads[i] = new Thread(() -> {
                try {
                    manager.attemptBid(item, id, price);
                    success.incrementAndGet();
                } catch (InvalidBidException e) { /* bình thường */ }
            });
        }
        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        int added = manager.getHistoryByItem(item.getId()).size() - before;
        assertEquals(success.get(), added, "Số giao dịch phải bằng số bid thành công");
    }
}