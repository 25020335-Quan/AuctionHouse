package auction.controller;
import auction.client.AuctionClient;
import auction.model.item.*;
import auction.model.*;
import auction.model.users.User;
import auction.util.GetItemListRequest;
import auction.util.SceneSwitcher;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainScreenController {
    @FXML
    private VBox marketplaceContainer;
    @FXML
    private VBox mymarketplaceContainer;
    private User currentUser;
    @FXML
    private MenuButton menuButton;
    @FXML
    private Button btnMarketplace;
    @FXML
    private Button btnMyItems;
    @FXML
    private Button btnMyBids;
    @FXML
    private Button btnProfile;
    @FXML
    private AnchorPane marketplacePane;
    @FXML
    private AnchorPane mymarketplacePane;
    @FXML
    private TextField searchField;

    @FXML
    public void initialize() {
        AuctionManager manager = AuctionManager.getInstance();
        if (manager.getAllItems().isEmpty()) {
            Item art1 = new Art("I01", "U01", "A", 50000);
            Item art2 = new Art("I02", "U02", "B", 60000);
            manager.addItem(art1);
            manager.addItem(art2);
        }
            loadProducts(manager.getAllItems());
            setupRealTimeSearch();
        }
    @FXML
    public void displayName(String name) {
        menuButton.setText("Hello," + name);
    }

    public void loadProducts(List<Item> itemList) {
        marketplaceContainer.getChildren().clear();// Clear hết các sản phẩm cũ trước cũ load danh sách mới nếu không sẽ bị đè lên nhau
        mymarketplaceContainer.getChildren().clear();
        for (Item item : itemList) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/productRow.fxml"));
                HBox row = loader.load();
                ProductRowController controller = loader.getController();
                controller.setData(item , currentUser, this );
                marketplaceContainer.getChildren().add(row);
                if (currentUser != null && item.getOwnerId().equals(currentUser.getId())) {
                    // Phải tạo một bản load mới (loader) vì một Object giao diện (row)
                    // không thể xuất hiện ở 2 nơi cùng lúc trên màn hình được
                    FXMLLoader myLoader = new FXMLLoader(getClass().getResource("/fxml/productRow.fxml"));
                    HBox myRow = myLoader.load();
                    ProductRowController controllerMine = myLoader.getController();
                    controllerMine.setData(item , currentUser , this);

                    mymarketplaceContainer.getChildren().add(myRow);
                }
            } catch (IOException e) {
                System.out.println("load product failed");
                e.printStackTrace();
            }
        }
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
    public void refreshUI() {
        Task<List<Item>> refreshTask = new Task<>() {
            @Override
            protected List<Item> call() throws Exception {
                // Gửi yêu cầu lấy đồ
                Object response = AuctionClient.getInstance().sendRequest(new GetItemListRequest());

                // Ép kiểu về List nếu Server trả về đúng
                if (response instanceof List) {
                    return (List<Item>) response;
                }
                return null;
            }
        };

        refreshTask.setOnSucceeded(e -> {
            List<Item> newList = (List<Item>) refreshTask.getValue();
            if (newList != null) {
                AuctionManager.getInstance().updateList(newList);
                // Cập nhật lên UI sử dụng hàm applySearchFilter có loadProducts trong đó
                applySearchFilter(searchField.getText());
            }
        });

        new Thread(refreshTask).start();
    }


   public void setLoggedInUser(User user) {
        this.currentUser = user;
        List<Item> items = AuctionManager.getInstance().getAllItems();
        loadProducts(items);
    }

    public User getCurrentUser() {
        return this.currentUser;
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
        System.err.println("Lỗi load file FXML");
        e.printStackTrace();
    } catch (Exception e) {
        System.err.println("Lỗi khởi tạo màn hình!");
        e.printStackTrace();
    }
}
    @FXML
    public void switchMarketplace(ActionEvent event) {
        marketplacePane.toFront();
        setActiveButton(btnMarketplace);
    }
    public void switchMyItems(ActionEvent event) {
        mymarketplacePane.toFront();
        setActiveButton(btnMyItems);
    }
    public void switchProfile(ActionEvent event) {
        setActiveButton(btnProfile);
    }
    public void switchMyBids(ActionEvent event) {
        setActiveButton(btnMyBids);
    }
    private void setActiveButton(Button clickedButton) {
        // Gom tất cả các nút trên Sidebar vào một mảng
        Button[] sidebarButtons = {btnMarketplace, btnMyItems, btnMyBids, btnProfile};

        // Dùng vòng lặp đi qua từng nút và gỡ "active" của chúng nó
        for (Button btn : sidebarButtons) {
            if (btn != null) {
                btn.getStyleClass().remove("sidebar-btn-active");
            }
        }

        if (clickedButton != null && !clickedButton.getStyleClass().contains("sidebar-btn-active")) {
            clickedButton.getStyleClass().add("sidebar-btn-active");
        }
    }
    private void setupRealTimeSearch() {
        // Hàm này sẽ tự động chạy mỗi khi gõ thêm/xóa bớt 1 ký tự vào ô search
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            applySearchFilter(newValue);
        });
    }
    private void applySearchFilter(String keyWord) {
        List<Item> allItems = AuctionManager.getInstance().getAllItems();
        // Nếu người dùng xóa trắng thanh search thì hiện lại toàn bộ danh sách gốc
        if (keyWord == null || keyWord.trim().isEmpty()) {
            loadProducts(allItems);
            return;
        }
    // Chuyển từ khóa về chữ thường để tìm kiếm không phân biệt Hoa/thường
    String lowerCaseKeyword = keyWord.toLowerCase().trim();
    List<Item> filteredList = new ArrayList<>();

    // Quét danh sách gốc, xem item nào có tên hoặc ID chứa từ khóa thì nhặt ra
        for (Item item : allItems) {
        if (item.getName().toLowerCase().contains(lowerCaseKeyword) ||
                item.getId().toLowerCase().contains(lowerCaseKeyword)) {
            filteredList.add(item);
        }
    }

    // Gọi hàm vẽ lại UI với danh sách đã lọc
    loadProducts(filteredList);
}
    // Thêm hàm để nhận hàng mới từ AddItemController
    public void addNewItemLocally(Item newItem) {
        //Cất vào kho Local của AuctionManager
        AuctionManager.getInstance().addItem(newItem);
        //Ép bộ lọc chạy lại.
        applySearchFilter(searchField.getText());
    }
    public void refreshLocalUI() {
        if (searchField != null) {
            applySearchFilter(searchField.getText());
        } else {
            loadProducts(AuctionManager.getInstance().getAllItems());
        }
    }
}