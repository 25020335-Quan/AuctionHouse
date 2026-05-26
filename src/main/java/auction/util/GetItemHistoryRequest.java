package auction.util;
import java.io.Serializable;
    public class GetItemHistoryRequest implements Serializable {
        private String itemId;
        public GetItemHistoryRequest(String itemId) { this.itemId = itemId; }
        public String getItemId() { return itemId; }
    }
