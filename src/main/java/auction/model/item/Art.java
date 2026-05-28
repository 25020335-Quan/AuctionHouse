package auction.model.item;

// Lớp cho tác phẩm nghệ thuật
public class Art extends Item {

    public Art(String ownerId, String name, double startingPrice) {
        super(ownerId, name, startingPrice);
    }

    public Art(String id, String ownerId, String name, double startingPrice) {
        super(id, ownerId, name, startingPrice);
    }

    @Override
    public String getItemType() {
        return "Art";
    }
}