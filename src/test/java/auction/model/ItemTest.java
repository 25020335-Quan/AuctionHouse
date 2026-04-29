package auction.model;

import auction.model.item.Item;
import auction.model.item.ItemFactory;
import auction.model.users.Member;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;

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