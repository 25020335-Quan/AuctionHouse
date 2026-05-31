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
        if (type == null) { return null; }
        String normalizedType = type.trim().toUpperCase();
        return factoryRegistry.get(normalizedType);
    }

    public static Item createItemByType(String type, String id, String ownerId, String name, double startingPrice) {
        ItemFactory factory = getFactory(type);
        if (factory == null) { return null; }
        return factory.createItem(id, ownerId, name, startingPrice);
    }

    public static Item createNewItemByType(String type, String ownerId, String name, double startingPrice) {
        ItemFactory factory = getFactory(type);
        if (factory == null) { return null; }
        return factory.createNewItem(ownerId, name, startingPrice);
    }
}