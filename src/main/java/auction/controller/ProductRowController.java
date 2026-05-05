package auction.controller;
import auction.model.item.Item;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import static java.lang.String.format;

public class ProductRowController {
    @FXML
    Label productName;
    @FXML
    Label productPrice;
    @FXML
    Label productOwnerId;
    @FXML
    Label productId;
    @FXML
    Label productState;
    public void setData(Item item){
        productName.setText(item.getName());
        productPrice.setText(format("%,.0f VNĐ", item.getCurrentPrice()));
        productOwnerId.setText(item.getOwnerId());
        productId.setText(item.getId());
        productState.setText("Pending");
    }
}
