package auction.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ItemTest {
    private Item testItem;
    private Member testMember;

    @BeforeEach
    void setUp() {
        testMember = new Member("U01", "Quan25020335", "password");
        testItem = ItemFactory.factoryItem("ELECTRONICS", "U02 ", "I01", "Laptop", 1000.0);
        testMember.postItem(testItem);
    }

    @Test
    void testPlaceBidHigherThanCurrent() {
        boolean result = testMember.bid(testItem, 1200.0);
        assertTrue(result, "Đặt giá cao hơn phải thành công.");
        assertEquals(1200.0, testItem.getCurrentPrice(), "Giá hiện tại phải cập nhật lên 1200.");
    }

    @Test
    void testPlaceBidLowerThanCurrent() {
        boolean result = testMember.bid(testItem, 900.0);
        assertFalse(result, "Đặt giá thấp hơn phải thất bại.");
        assertEquals(1000.0, testItem.getCurrentPrice(), "Giá hiện tại phải giữ nguyên 1000.");
    }
}