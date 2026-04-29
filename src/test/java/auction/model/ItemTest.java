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


}