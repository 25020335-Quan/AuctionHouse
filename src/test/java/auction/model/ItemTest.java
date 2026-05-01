package auction.model;

import auction.exception.InvalidBidException;
import auction.model.item.Item;
import auction.model.item.ItemFactory;
import auction.model.state.AuctionState;
import auction.model.users.Member;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ============================================================
 * File: ItemTest.java
 * Package: auction.model
 *
 * Test logic đặt giá trong AuctionManager.attemptBid()
 * Dựa trên code thực tế của nhóm (AuctionManager.java)
 * ============================================================
 */
public class ItemTest {

    // ---------------------------------------------------------
    // Biến dùng chung — được reset trước MỖI test (@BeforeEach)
    // ---------------------------------------------------------
    private Item   testItem;   // Món đồ đang đấu giá (ownerId = "U02")
    private Member testMember; // Người dùng U01 — đóng vai bidder hợp lệ
    private Member seller;     // Người bán U02  — sở hữu testItem
    private AuctionManager manager;

    @BeforeEach
    void setUp() {
        testMember = new Member("U01", "Quan25020335", "password");
        seller   = new Member("U02", "Viet_seller", "password");
        testItem = ItemFactory.factoryItem("ELECTRONICS", "U02", "I01", "Laptop", 1000.0);

        // postItem() → setState(OPEN): item sẵn sàng để đấu giá
        testMember.postItem(testItem);

        manager = AuctionManager.getInstance();
    }

    // ============================================================
    //  NHÓM 1 — TRẠNG THÁI PHIÊN ĐẤU GIÁ
    // ============================================================

    /**
     * Item sau khi gọi postItem() phải ở trạng thái OPEN.
     * Đây là tiền đề của tất cả các test bên dưới.
     */
    @Test
    @DisplayName("Sau postItem() → trạng thái phải là OPEN")
    void testPostItem_StateShouldBeOpen() {
        assertEquals(AuctionState.OPEN, testItem.getState(),
                "postItem() phải chuyển trạng thái sang OPEN");
    }

    /**
     * Sau bid đầu tiên → state chuyển sang RUNNING.
     */
    @Test
    @DisplayName("Bid hợp lệ khi OPEN → thành công, state → RUNNING")
    void testBid_ValidAmount_WhenOpen_ShouldSucceed() {
        assertDoesNotThrow(() ->
                manager.attemptBid(testItem, testMember.getId(), 1500.0)
        );
        // Sau bid đầu tiên từ OPEN → chuyển sang RUNNING
        assertEquals(AuctionState.RUNNING, testItem.getState());
    }

    /**
     * Đặt giá khi phiên CLOSED → phải ném InvalidBidException.
     */
    @Test
    @DisplayName("Bid khi phiên CLOSED → InvalidBidException")
    void testBid_WhenClosed_ShouldThrow() {
        testItem.setState(AuctionState.CLOSED);

        InvalidBidException ex = assertThrows(
                InvalidBidException.class,
                () -> manager.attemptBid(testItem, testMember.getId(), 2000.0)
        );

        assertEquals("Phiên đấu giá đã kết thúc!", ex.getMessage());
    }

    /**
     * Đặt giá khi phiên SOLD (đã bán xong) → phải ném InvalidBidException.
     */
    @Test
    @DisplayName("Bid khi phiên SOLD → InvalidBidException")
    void testBid_WhenSold_ShouldThrow() {
        testItem.setState(AuctionState.SOLD);

        InvalidBidException ex = assertThrows(
                InvalidBidException.class,
                () -> manager.attemptBid(testItem, testMember.getId(), 2000.0)
        );

        assertEquals("Phiên đấu giá đã kết thúc!", ex.getMessage());
    }

    /**
     * Đặt giá khi phiên PENDING (item chưa được postItem).
     *
     * Cách tái tạo: tạo item MỚI mà không gọi postItem() → mặc định PENDING.
     */
    @Test
    @DisplayName("Bid khi phiên PENDING (chưa mở) → InvalidBidException")
    void testBid_WhenPending_ShouldThrow() {
        // Tạo item mới, KHÔNG gọi postItem() → trạng thái PENDING
        Item pendingItem = ItemFactory.factoryItem("ART", "U02", "I02", "Painting", 500.0);

        InvalidBidException ex = assertThrows(
                InvalidBidException.class,
                () -> manager.attemptBid(pendingItem, testMember.getId(), 600.0)
        );

        assertEquals("Lỗi: Phiên đấu giá chưa bắt đầu.", ex.getMessage());
    }

    // ============================================================
    //  NHÓM 2 — KIỂM TRA MỨC GIÁ ĐẶT
    // ============================================================

    /**
     * Đặt giá THẤP hơn giá khởi điểm 1000 → bị từ chối.
     */
    @Test
    @DisplayName("Bid thấp hơn giá hiện tại → InvalidBidException")
    void testBid_AmountLowerThanCurrent_ShouldThrow() {
        InvalidBidException ex = assertThrows(
                InvalidBidException.class,
                () -> manager.attemptBid(testItem, testMember.getId(), 500.0)
        );

        assertEquals("Giá đặt phải lớn hơn giá hiện tại!", ex.getMessage());
    }

    /**
     * Đặt giá âm → bị từ chối (thấp hơn mọi giá hợp lý).
     */
    @Test
    @DisplayName("Bid giá âm → InvalidBidException")
    void testBid_NegativeAmount_ShouldThrow() {
        assertThrows(
                InvalidBidException.class,
                () -> manager.attemptBid(testItem, testMember.getId(), -999.0)
        );
    }

    /**
     * Sau khi bid thành công → currentPrice phải được cập nhật đúng.
     */
    @Test
    @DisplayName("Sau bid thành công → currentPrice được cập nhật")
    void testBid_AfterSuccess_PriceIsUpdated() throws InvalidBidException {
        manager.attemptBid(testItem, testMember.getId(), 2500.0);

        assertEquals(2500.0, testItem.getCurrentPrice(),
                "getCurrentPrice() phải bằng giá vừa đặt thành công");
    }

    /**
     * Bid nhiều lần liên tiếp, mỗi lần tăng giá → luôn thành công.
     * Mô phỏng cuộc đấu giá nhiều vòng giữa 2 người.
     */
    @Test
    @DisplayName("Bid nhiều vòng tăng dần → giá cập nhật từng bước")
    void testBid_MultipleBids_PriceIncreasesStepByStep() throws InvalidBidException {
        Member bidder2 = new Member("U03", "Thai_bidder", "pass");

        manager.attemptBid(testItem, testMember.getId(), 1500.0);
        assertEquals(1500.0, testItem.getCurrentPrice(), "Vòng 1: phải là 1500");

        manager.attemptBid(testItem, bidder2.getId(), 2000.0);
        assertEquals(2000.0, testItem.getCurrentPrice(), "Vòng 2: phải là 2000");

        manager.attemptBid(testItem, testMember.getId(), 3000.0);
        assertEquals(3000.0, testItem.getCurrentPrice(), "Vòng 3: phải là 3000");
    }

    // ============================================================
    //  NHÓM 3 — KIỂM TRA VAI TRÒ NGƯỜI DÙNG
    // ============================================================

    /**
     * Người bán (ownerId = "U02") tự đặt giá đồ của mình → bị từ chối.
     *
     * Đây là trường hợp gian lận: tự đẩy giá lên.
     */
    @Test
    @DisplayName("Người bán tự bid đồ mình → InvalidBidException")
    void testBid_SellerBidsOwnItem_ShouldThrow() {
        InvalidBidException ex = assertThrows(
                InvalidBidException.class,
                () -> manager.attemptBid(testItem, seller.getId(), 2000.0)
        );

        assertEquals("Lỗi: Người bán không được tự đấu giá đồ của mình).", ex.getMessage());
    }

    /**
     * Người mua hợp lệ (U01, khác ownerId "U02") đặt giá → thành công.
     */
    @Test
    @DisplayName("Người mua hợp lệ (U01) bid đồ của U02 → thành công")
    void testBid_ValidBidder_DifferentFromSeller_ShouldSucceed() {
        // testMember.getId() = "U01" ≠ "U02" → hợp lệ
        assertDoesNotThrow(() ->
                manager.attemptBid(testItem, testMember.getId(), 1500.0)
        );
    }
}