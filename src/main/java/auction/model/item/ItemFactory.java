package auction.model.item;

import auction.model.item.Art;
import auction.model.item.Electronics;
import auction.model.item.Vehicle;

public class ItemFactory {
    public static Item factoryItem(String type, String ownerId, String id, String name, double startingPrice) {
        if (type == null) {
            return null;
        }

        // Trả về đối tượng tương ứng dựa trên chuỗi nhập vào
        if (type.equalsIgnoreCase("ELECTRONICS")) {
            return new Electronics(id, ownerId, name, startingPrice);
        } else if (type.equalsIgnoreCase("ART")) {
            return new Art(id, ownerId, name, startingPrice);
        } else if (type.equalsIgnoreCase("VEHICLE")) {
            return new Vehicle(id, ownerId, name, startingPrice);
        }

        return null;
    }
}