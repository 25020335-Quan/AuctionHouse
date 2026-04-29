package auction.model.item;

// Lớp cho đồ điện tử
public class Electronics extends Item {
    public Electronics(String id, String ownerId, String name, double startingPrice) {
        super(id, ownerId, name, startingPrice);
    }

    @Override
    public String getItemType() {
        return "Electronics";
    }
}