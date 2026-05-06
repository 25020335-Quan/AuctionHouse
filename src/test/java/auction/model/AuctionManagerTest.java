package auction.model;

import auction.model.factory.FactoryProvider;
import auction.model.item.Item;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

public class AuctionManagerTest {
    private AuctionManager manager;

    @BeforeEach
    void setUp() {
        manager = AuctionManager.getInstance();
    }

    @Test
    void testSingletonInstance() {
        AuctionManager instance2 = AuctionManager.getInstance();
        assertSame(manager, instance2, "AuctionManager phải là Singleton duy nhất.");
    }

    @Test
    void testAddItem() {
        Item item = FactoryProvider.createItemByType("ELECTRONICS", "U01", "T1", "Test Item", 100.0);
        int initialSize = manager.getAllItems().size();
        manager.addItem(item);
        assertEquals(initialSize + 1, manager.getAllItems().size(), "Danh sách sản phẩm phải tăng thêm 1.");
    }
}