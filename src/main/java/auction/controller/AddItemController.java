package auction.controller;

import auction.client.AuctionClient;
import auction.model.AuctionManager;
import auction.model.item.Art;
import auction.model.item.Electronics;
import auction.model.item.Item;
import auction.model.item.Vehicle;
import auction.model.users.User;
import auction.util.AddItemRequest;
import auction.util.LoginRequest;
import auction.util.NotificationRequest;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
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

        Task<Item> addItemTask = new Task<Item>() {
            @Override
            protected Item call() throws Exception {
                while (true) {
                    Object response = AuctionClient.getInstance().sendRequest(new AddItemRequest(newItem));

                    if (response instanceof Item) {
                        return (Item) response; // Chỉ thoát vòng lặp khi nhận đúng Item
                    } else if (response instanceof NotificationRequest note) {
                        // Tiện thể xử lý luôn thông báo nếu nhận nhầm
                        Platform.runLater(() -> showAlert(note.getMsg()));
                        // Sau đó tiếp tục vòng lặp để đợi gói tin tiếp theo (Hy vọng là Item)
                    }
                }
            }
        };

        addItemTask.setOnSucceeded(e -> {
            AuctionManager.getInstance().addItem(newItem);
            parentController.loadProducts(AuctionManager.getInstance().getAllItems());
            parentController.refreshUI();
            ((Stage)nameField.getScene().getWindow()).close();
            System.out.println("Đã thêm sản phẩm thành công!");
        });
        Thread thread = new Thread(addItemTask);
        thread.start();
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION); // Loại thông báo có icon chữ 'i'
        alert.setTitle("Thông báo hệ thống");
        alert.setHeaderText(null); // Không dùng tiêu đề phụ
        alert.setContentText(message);
        alert.showAndWait(); // Hiển thị và bắt người dùng nhấn OK mới được làm tiếp
    }
}
