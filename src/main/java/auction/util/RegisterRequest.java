package auction.util;

import auction.model.users.User;
import java.io.Serializable;

    public class RegisterRequest implements Serializable {
        private User user;

        public RegisterRequest(User user) {
            this.user = user;
        }

        public User getUser() {
            return user;
        }
    }
