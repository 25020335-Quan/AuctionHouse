package auction.controller;

import auction.model.item.*;
import auction.model.*;
import auction.model.service.DatabaseService;
import auction.model.users.User;
import auction.util.SceneSwitcher;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

public class MainScreenController {
    @FXML
    Label welcomeText;
    @FXML
    Label welcomeText2;
    @FXML
    private VBox marketplaceContainer;
    @FXML
    private VBox mymarketplaceContainer;
    private User currentUser;

    @FXML
    public void initialize() {
        startAutoRefresh();
    }


    public void startAutoRefresh() {
        // Tạo một Timeline chạy mỗi 5 giây (Duration.seconds(5))
        Timeline autoRefresh = new Timeline(new KeyFrame(Duration.seconds(5), event -> {
            refreshUI(); // Gọi phương thức refresh của bạn
            System.out.println("Giao diện đã được tự động cập nhật!");
        }));

        // Thiết lập lặp lại vô hạn
        autoRefresh.setCycleCount(Timeline.INDEFINITE);
        autoRefresh.play();
    }

    @FXML
    public void displayName(String name) {
        welcomeText.setText("Hello, " + name);
        welcomeText2.setText("Hello, " + name);
    }

    public void loadProducts(List<Item> itemList) {
        marketplaceContainer.getChildren().clear();// Clear hết các sản phẩm cũ trước cũ load danh sách mới nếu không sẽ bị đè lên nhau
        mymarketplaceContainer.getChildren().clear();
        for (Item item : itemList) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/productRow.fxml"));
                HBox row = loader.load();
                ProductRowController controller = loader.getController();
                controller.setData(item);
                marketplaceContainer.getChildren().add(row);
                if (currentUser != null && item.getOwnerId().equals(currentUser.getId())) {
                    // Phải tạo một bản load mới (loader) vì một Object giao diện (row)
                    // không thể xuất hiện ở 2 nơi cùng lúc trên màn hình được
                    FXMLLoader myLoader = new FXMLLoader(getClass().getResource("/fxml/productRow.fxml"));
                    HBox myRow = myLoader.load();
                    ProductRowController controllerMine = myLoader.getController();
                    controllerMine.setData(item);

                    mymarketplaceContainer.getChildren().add(myRow);
                }
            } catch (IOException e) {
                System.out.println("load product failed");
            }
        }
    }

    public void setLoggedInUser(User user) {
        this.currentUser = user;
        List<Item> items = AuctionManager.getInstance().getAllItems();
        loadProducts(items);
    }

    public User getCurrentUser() {
        return this.currentUser;
    }

    void refreshUI() {
        // Lấy danh sách mới nhất từ Manager
        DatabaseService dbService = new DatabaseService();
        dbService.loadAllItemsToManager();
        // Gọi hàm load chung mà bạn đã viết ở bước trước
        loadProducts(AuctionManager.getInstance().getAllItems());
    }
    @FXML
    public void openAddItemPopup(ActionEvent event) {
        try{
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/addItem.fxml"));
        Parent root = loader.load();
        AddItemController controller = loader.getController();
        controller.setParentController(this);
            Stage stage = new Stage();
            stage.setTitle("Post New Item");
            stage.setScene(new Scene(root));
            stage.show();
    }
     catch (IOException e) {
        System.err.println("Lỗi load file FXML: Kiểm tra lại đường dẫn /fxml/addItem.fxml");
        e.printStackTrace(); // Dòng này sẽ hiện chữ đỏ ở Console cho bạn biết lỗi gì
    } catch (Exception e) {
        System.err.println("Lỗi khởi tạo màn hình!");
        e.printStackTrace();
    }
}
}