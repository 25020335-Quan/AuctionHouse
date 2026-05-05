package auction.model;

import auction.model.item.Electronics;
import auction.model.item.Item;
import auction.util.LoginRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.*;

import static org.junit.jupiter.api.Assertions.*;

public class SerializationTest {

    @Test
    @DisplayName("Kiểm tra Serialization của LoginRequest (Tuần 9)")
    void testLoginRequestSerialization() throws IOException, ClassNotFoundException {
        LoginRequest originalRequest = new LoginRequest("U01", "password123");

        // Ghi Object ra byte array (Mô phỏng gửi qua Socket)
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(originalRequest);
        oos.close();

        // Đọc Object từ byte array (Mô phỏng nhận từ Socket)
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);
        LoginRequest deserializedRequest = (LoginRequest) ois.readObject();
        ois.close();

        assertNotNull(deserializedRequest);
        assertEquals(originalRequest.getUsername(), deserializedRequest.getUsername());
        assertEquals(originalRequest.getPassword(), deserializedRequest.getPassword());
    }

    @Test
    @DisplayName("Kiểm tra Serialization của Item (Đảm bảo truyền được qua mạng)")
    void testItemSerialization() throws IOException, ClassNotFoundException {
        Item originalItem = new Electronics("E01", "U99", "Laptop", 1000.0);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(originalItem);
        oos.close();

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);
        Item deserializedItem = (Item) ois.readObject();
        ois.close();

        assertEquals(originalItem.getId(), deserializedItem.getId());
        assertEquals(originalItem.getItemType(), deserializedItem.getItemType());
    }
}