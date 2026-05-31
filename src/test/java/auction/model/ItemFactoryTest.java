package auction.model;

import auction.model.factory.FactoryProvider;
import auction.model.item.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ItemFactoryTest {

    @Test
    void testCreateElectronics() {
        Item item = FactoryProvider.createItemByType("ELECTRONICS", "E1", "U01", "Sony TV", 500.0);
        assertNotNull(item);
        assertTrue(item instanceof Electronics, "Đối tượng phải thuộc lớp Electronics.");
        assertEquals("Electronics", item.getItemType());
    }

    @Test
    void testCreateArt() {
        Item item = FactoryProvider.createItemByType("ART", "A1", "U01", "Painting", 2000.0);
        assertNotNull(item);
        assertTrue(item instanceof Art, "Đối tượng phải thuộc lớp Art.");
        assertEquals("Art", item.getItemType());
    }

    @Test
    void testCreateVehicle() {
        Item item = FactoryProvider.createItemByType("VEHICLE", "V1", "U01", "Honda Wave", 20000.0);
        assertNotNull(item);
        assertTrue(item instanceof Vehicle, "Đối tượng phải thuộc lớp Vehicle.");
        assertEquals("Vehicle", item.getItemType());
    }

    @Test
    void testCreateUnknownType() {
        Item item = FactoryProvider.createItemByType("UNKNOWN", "U1", "U01", "None", 0.0);
        assertNull(item, "Với loại không xác định, Factory phải trả về null.");
    }
}