package auction.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ItemFactoryTest {

    @Test
    void testCreateElectronics() {
        Item item = ItemFactory.createItem("ELECTRONICS", "E1", "Sony TV", 500.0);
        assertNotNull(item);
        assertTrue(item instanceof Electronics, "Đối tượng phải thuộc lớp Electronics.");
        assertEquals("Electronics", item.getItemType());
    }

    @Test
    void testCreateArt() {
        Item item = ItemFactory.createItem("ART", "A1", "Painting", 2000.0);
        assertNotNull(item);
        assertTrue(item instanceof Art, "Đối tượng phải thuộc lớp Art.");
        assertEquals("Art", item.getItemType());
    }

    @Test
    void testCreateVehicle() {
        Item item = ItemFactory.createItem("VEHICLE", "V1", "Honda Wave", 20000.0);
        assertNotNull(item);
        assertTrue(item instanceof Vehicle, "Đối tượng phải thuộc lớp Vehicle.");
        assertEquals("Vehicle", item.getItemType());
    }

    @Test
    void testCreateUnknownType() {
        Item item = ItemFactory.createItem("UNKNOWN", "U1", "None", 0.0);
        assertNull(item, "Với loại không xác định, Factory phải trả về null.");
    }
}