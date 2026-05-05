package auction.model;

import auction.model.item.Item;
import auction.model.state.AuctionState;
import auction.model.users.Member;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MemberLogicTest {

    private Member user;

    @BeforeEach
    void setUp() {
        user = new Member("U01", "testUser", "pass123");
    }

    @Test
    @DisplayName("Tạo Item mới -> Trạng thái phải là PENDING")
    void testCreateItem_ShouldBePending() {
        user.createItem("ART", "A01", "Mona Lisa", 5000.0);
        Item item = user.findById("A01");

        assertNotNull(item, "Item phải được thêm vào danh sách ownedItems");
        assertEquals("U01", item.getOwnerId(), "Owner ID phải khớp với ID người tạo");
        assertEquals(AuctionState.PENDING, item.getState(), "Item mới tạo phải ở trạng thái PENDING");
    }

    @Test
    @DisplayName("Đăng Item (postItem) -> Trạng thái phải chuyển sang OPEN")
    void testPostItem_ShouldChangeStateToOpen() {
        user.createItem("VEHICLE", "V01", "Honda Wave", 1000.0);
        Item item = user.findById("V01");

        user.postItem(item); // Đăng món hàng lên sàn

        assertEquals(AuctionState.OPEN, item.getState(), "Sau khi post, trạng thái phải là OPEN");
    }
}