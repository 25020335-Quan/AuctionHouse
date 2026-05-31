package auction.model;

import auction.model.item.Electronics;
import auction.model.item.Item;
import auction.model.transaction.BidTransaction;
import auction.util.LoginRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm tra các object truyền qua Socket không mất dữ liệu.
 * Hệ thống dùng ObjectOutputStream/ObjectInputStream nên tất cả object
 * gửi đi phải implements Serializable và giữ nguyên giá trị sau deserialize.
 */
public class SerializationTest {

    // Hàm tiện ích: serialize → byte array → deserialize lại
    // Mô phỏng đúng luồng gửi/nhận qua Socket mà không cần kết nối mạng thật
    @SuppressWarnings("unchecked")
    private static <T> T roundTrip(T obj) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new ObjectOutputStream(baos).writeObject(obj);
        return (T) new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray())).readObject();
    }

    @Test
    @DisplayName("LoginRequest serialize/deserialize → username và password không đổi")
    void testLoginRequest_Serialization() throws IOException, ClassNotFoundException {
        LoginRequest original = new LoginRequest("buyer_quan", "password123");
        LoginRequest result   = roundTrip(original);

        assertNotNull(result);
        assertEquals(original.getUsername(), result.getUsername());
        assertEquals(original.getPassword(), result.getPassword());
    }

    @Test
    @DisplayName("Item (Electronics) serialize/deserialize → id, type, giá không đổi")
    void testItem_Serialization() throws IOException, ClassNotFoundException {
        Item original = new Electronics("E01", "U99", "Laptop", 1000.0);
        Item result   = roundTrip(original);

        assertEquals(original.getId(),           result.getId());
        assertEquals(original.getItemType(),     result.getItemType());
        assertEquals(original.getCurrentPrice(), result.getCurrentPrice());
    }

    @Test
    @DisplayName("BidTransaction serialize/deserialize → bidderId và bidAmount không đổi")
    void testBidTransaction_Serialization() throws IOException, ClassNotFoundException {
        // BidTransaction được Server gửi về Client khi broadcast lịch sử đấu giá
        BidTransaction original = new BidTransaction("TX-001", "U01", "E01", 1500.0);
        BidTransaction result   = roundTrip(original);

        assertEquals(original.getBidderId(),  result.getBidderId());
        assertEquals(original.getItemId(),    result.getItemId());
        assertEquals(original.getBidAmount(), result.getBidAmount());
    }

    @Test
    @DisplayName("BidTransaction implements Serializable → không crash khi gửi qua Socket")
    void testBidTransaction_IsSerializable() {
        // Nếu thiếu implements Serializable → ObjectOutputStream.writeObject() ném NotSerializableException
        BidTransaction tx = new BidTransaction("TX-002", "U01", "E01", 2000.0);
        assertTrue(tx instanceof Serializable);
    }
}