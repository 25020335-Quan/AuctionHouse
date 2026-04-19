package auction.model;

public class ItemFactory {
    public static Item createItem(String type, String id, String name, double startingPrice) {
        if (type == null) {
            return null;
        }

        // Trả về đối tượng tương ứng dựa trên chuỗi nhập vào
        if (type.equalsIgnoreCase("ELECTRONICS")) {
            return new Electronics(id, name, startingPrice);
        } else if (type.equalsIgnoreCase("ART")) {
            return new Art(id, name, startingPrice);
        } else if (type.equalsIgnoreCase("VEHICLE")) {
            return new Vehicle(id, name, startingPrice);
        }

        return null;
    }
}