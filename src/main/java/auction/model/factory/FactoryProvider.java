package auction.model.factory;

import auction.model.item.Item;

public class FactoryProvider {
    public static Item createItemByType(String type, String id, String ownerId, String name, double startingPrice) {
        ItemFactory factory = null;

        if (type.equalsIgnoreCase("electronics")) {
            factory = new ElectronicsFactory();
        } else if (type.equalsIgnoreCase("art")) {
            factory = new ArtFactory();
        } else if (type.equalsIgnoreCase("vehicle")) {
            factory = new VehicleFactory();
        } else {
            System.out.println("Loại sản phẩm không hợp lệ");
        }
        if (factory != null) {
            return factory.createItem(id , ownerId, name, startingPrice);
        }
        return null;
    }

    public static Item createNewItemByType(String type, String ownerId, String name, double startingPrice) {
        ItemFactory factory = null;
        if (type.equalsIgnoreCase("electronics")) factory = new ElectronicsFactory();
        else if (type.equalsIgnoreCase("art")) factory = new ArtFactory();
        else if (type.equalsIgnoreCase("vehicle")) factory = new VehicleFactory();

        if (factory != null) return factory.createNewItem(ownerId, name, startingPrice);
        return null;
    }
}
