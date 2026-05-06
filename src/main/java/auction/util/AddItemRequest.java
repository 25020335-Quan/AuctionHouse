package auction.util;

import java.io.Serializable;
import auction.model.item.Item; // Lớp Item của bạn

public class AddItemRequest implements Serializable {
    private Item item;

    public AddItemRequest(Item item) {
        this.item = item;
    }

    public Item getItem() { return item; }
}