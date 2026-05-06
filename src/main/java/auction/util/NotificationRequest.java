package auction.util;

import java.io.Serializable;

public class NotificationRequest implements Serializable {
    private String msg;

    public NotificationRequest(String msg) {
        this.msg = msg;
    }

    public String getMsg() {
        return msg;
    }
}
