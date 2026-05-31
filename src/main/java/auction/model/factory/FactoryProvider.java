package auction.model.factory;

import auction.model.item.Item;
import java.util.HashMap;
import java.util.Map;

public class FactoryProvider {
    private static final Map<String, ItemFactory> factoryRegistry = new HashMap<>();

    static {
        factoryRegistry.put("ELECTRONICS", new ElectronicsFactory());
        factoryRegistry.put("ART", new ArtFactory());
        factoryRegistry.put("VEHICLE", new VehicleFactory());
    }

    private static ItemFactory getFactory(String type) {
        String normalizedType = type.trim().toUpperCase();
        ItemFactory factory = factoryRegistry.get(normalizedType);

        if (factory != null) {
            return factory;
        }
        throw new IllegalArgumentException("Lỗi: Không tìm thấy Factory cho loại '" + type + "'");
    }
    public static Item createItemByType(String type, String id, String ownerId, String name, double startingPrice) {
        ItemFactory factory = getFactory(type);

        return factory.createItem(id, ownerId, name, startingPrice);
    }

    public static Item createNewItemByType(String type, String ownerId, String name, double startingPrice) {
        ItemFactory factory = getFactory(type);

        return factory.createNewItem(ownerId, name, startingPrice);
    }
}
