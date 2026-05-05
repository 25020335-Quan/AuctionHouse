package auction.controller;

import auction.model.AuctionManager;
import auction.model.item.Art;
import auction.model.item.Electronics;
import auction.model.item.Item;
import auction.model.item.Vehicle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class AddItemController {
    @FXML
    TextField nameField;
    @FXML
    TextField priceField;
    @FXML
    TextField typeField;
    @FXML
    Button saveItem;
    @FXML
    private MainScreenController parentController;
    private Item newItem;
    public void setParentController(MainScreenController parent) {
        this.parentController = parent;
    }
    @FXML
    public void handleSave(ActionEvent event) {
        // Lấy thông tin từ các ô nhập liệu
        String name = nameField.getText();
        double price = Double.parseDouble(priceField.getText());

        // Tạo sản phẩm mới gắn với ID của người đang đăng nhập
        String itemId = "I" + System.currentTimeMillis();
        String ownerId = parentController.getCurrentUser().getId();
        String typeInput  = typeField.getText().trim().toLowerCase();
        if (typeInput.contains("art")) {
            newItem = new Art(itemId, ownerId, name, price);
        } else if (typeInput.contains("electronics")) {
            newItem = new Electronics(itemId, ownerId, name, price);
        } else if (typeInput.contains("vehicle")) {
            newItem = new Vehicle(itemId, ownerId, name, price);
        } else {
            // Mặc định hoặc báo lỗi nếu nhập sai loại
            System.out.println("Loại sản phẩm không hợp lệ");
        }
        AuctionManager.getInstance().addItem(newItem);
        parentController.refreshUI();
        ((Stage)nameField.getScene().getWindow()).close();
    }
}
