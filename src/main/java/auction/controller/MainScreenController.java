package auction.controller;
import auction.client.AuctionClient;
import auction.model.item.*;
import auction.model.*;
import auction.model.state.AuctionState;
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
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;


import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MainScreenController {
    @FXML
    private VBox marketplaceContainer;
    @FXML
    private VBox mymarketplaceContainer;
    @FXML
    private VBox wonAuctionsContainer;
    private User currentUser;
    @FXML
    private MenuButton menuButton;
    @FXML
    private Button btnMarketplace;
    @FXML
    private Button btnMyItems;
    @FXML
    private Button btnWonAuctions;
    @FXML
    private Button btnProfile;
    @FXML
    private AnchorPane marketplacePane;
    @FXML
    private AnchorPane mymarketplacePane;
    @FXML
    private AnchorPane wonAuctionsPane;
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
        setupRealTimeSearch();
    }

    @FXML
    public void displayName(String name) {
        menuButton.setText("Hello," + name);
    }

    public void loadProducts(List<Item> itemList) {
        marketplaceContainer.getChildren().clear(); // Clear hết các sản phẩm cũ
        mymarketplaceContainer.getChildren().clear();

        for (Item item : itemList) {
            try {
                // lọc Marketplace : Chỉ những món đang mở hoặc đang chạy mới được ra chợ
                boolean isActiveForMarket = (item.getState() == AuctionState.OPEN ||
                        item.getState() == AuctionState.PENDING ||
                        item.getState() == AuctionState.RUNNING);

                // lọc My Items : Kiểm tra xem món đồ này có phải của mình không
                boolean isMyItem = (currentUser != null && item.getOwnerId().equals(currentUser.getId()));

                // Nếu đồ đã CANCELED/SOLD và không phải đồ của mình
                if (!isActiveForMarket && !isMyItem) {
                    continue;
                }

                // Nếu đủ điều kiện ra chợ -> Đưa vào marketplaceContainer
                if (isActiveForMarket) {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/productRow.fxml"));
                    HBox row = loader.load();
                    ProductRowController controller = loader.getController();
                    controller.setData(item, currentUser, this);
                    marketplaceContainer.getChildren().add(row);
                }

                // Nếu là đồ của mình -> Đưa vào mymarketplaceContainer
                if (isMyItem) {
                    FXMLLoader myLoader = new FXMLLoader(getClass().getResource("/fxml/productRow.fxml"));
                    HBox myRow = myLoader.load();
                    ProductRowController controllerMine = myLoader.getController();
                    controllerMine.setData(item, currentUser, this);
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
        // Code test
        boolean hasTestItem = false;
        for (Item i : AuctionManager.getInstance().getAllItems()) {
            if (i.getId().equals("TEST-999")) hasTestItem = true;
        }
        if (!hasTestItem) {
            // 1. Tạo món đồ do người khác bán
            Item mockItem = new Art("TEST-999", "KhachLa", "Đồng hồ Rolex (Test)", 100000);

            // 2. Ép thời gian đếm ngược chỉ còn đúng 10 giây
            mockItem.setStartTime(LocalDateTime.now());
            mockItem.setEndTime(LocalDateTime.now().plusSeconds(60));

            // 3. Ép mình đang là người trả giá cao nhất (Winner)
            mockItem.setHighestBidderName(user.getUsername());
            mockItem.setState(AuctionState.RUNNING);

            // Bơm vào kho
            AuctionManager.getInstance().addItem(mockItem);
        }
        // =====================================================================

        List<Item> items = AuctionManager.getInstance().getAllItems();
        loadProducts(items);
    }

    public User getCurrentUser() {
        return this.currentUser;
    }

    @FXML
    public void openAddItemPopup(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/addItem.fxml"));
            Parent root = loader.load();
            AddItemController controller = loader.getController();
            controller.setParentController(this);
            Stage stage = new Stage();
            stage.setTitle("Post New Item");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
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

    @FXML
    public void switchMyItems(ActionEvent event) {
        mymarketplacePane.toFront();
        setActiveButton(btnMyItems);
    }

    public void switchProfile(ActionEvent event) {
        setActiveButton(btnProfile);
    }

    @FXML
    public void handleShowWonAuctions(ActionEvent event) {
        wonAuctionsPane.toFront();
        setActiveButton(btnWonAuctions);

        if (wonAuctionsContainer != null) {
            wonAuctionsContainer.getChildren().clear();
        }
        List<Item> allItems = AuctionManager.getInstance().getAllItems();
        for (Item item : allItems) {
            // Kiểm tra phiên đấu giá đã kết thúc (CLOSED, SOLD, CANCELED)
            boolean isEnded = (item.getState() == AuctionState.CLOSED ||
                    item.getState() == AuctionState.SOLD ||
                    item.getState() == AuctionState.CANCELED);

            // Kiểm tra người dùng hiện tại có phải là người trả giá cao nhất không
            boolean isWinner = item.getHighestBidderName() != null && currentUser != null &&
                    item.getHighestBidderName().equals(currentUser.getUsername());

            if (isEnded && isWinner) {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/productRow.fxml"));
                    HBox row = loader.load();

                    ProductRowController rowController = loader.getController();
                    // Truyền currentUser và màn hình chính vào cho controller
                    rowController.setData(item, currentUser, this);

                    if (wonAuctionsContainer != null) {
                        wonAuctionsContainer.getChildren().add(row);
                    }
                } catch (IOException e) {
                    System.err.println("Lỗi tải giao diện cho mục Won Auctions");
                    e.printStackTrace();
                }
            }
        }
        if (wonAuctionsContainer != null && wonAuctionsContainer.getChildren().isEmpty()) {
            Label emptyLabel = new Label("You haven't won any auctions yet");
            emptyLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #6b7280; -fx-padding: 20px;");
            wonAuctionsContainer.getChildren().add(emptyLabel);
        }
    }

    private void setActiveButton(Button clickedButton) {
        // Gom tất cả các nút trên Sidebar vào một mảng
        Button[] sidebarButtons = {btnMarketplace, btnMyItems, btnWonAuctions, btnProfile};

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

    public void handlePayment(Item wonItem) {
        // Confirm thanh toán
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Payment Confirmation");
        alert.setHeaderText("Checkout for: " + wonItem.getName());
        alert.setContentText(String.format("Grand total: %,.0f VND.\n\nDo you want to complete your payment now?", wonItem.getCurrentPrice()));
        // Tạo 3 nút bấm
        ButtonType btnPay = new ButtonType("Pay Now", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancelOrder = new ButtonType("Cancel Order", ButtonBar.ButtonData.LEFT);
        ButtonType btnLater = new ButtonType("Later", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(btnPay, btnCancelOrder, btnLater);
        // Chờ người dùng bấm nút OK hoặc Cancel
        alert.showAndWait().ifPresent(response -> {
            if (response == btnPay) {
                Task<Boolean> paymentTask = new Task<>() {
                    @Override
                    protected Boolean call() throws Exception {

                        Thread.sleep(1000);
                        return true;
                    }
                };
                paymentTask.setOnSucceeded(e -> {
                    boolean isSuccess = paymentTask.getValue();

                    if (isSuccess) {
                        // Cập nhật trạng thái món đồ thành SOLD
                        wonItem.setState(AuctionState.SOLD);
                        Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                        successAlert.setTitle("Payment successful");
                        successAlert.setHeaderText(null);
                        successAlert.setContentText("Payment successful! Your order ID is #" + wonItem.getId());
                        successAlert.showAndWait();
                        // Load lại màn hình cập nhật lại nút
                        handleShowWonAuctions(null);
                        List<Item> currentItems = AuctionManager.getInstance().getAllItems();
                        loadProducts(currentItems);
                    }
                });
                paymentTask.setOnFailed(e -> {
                    Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                    errorAlert.setTitle("Payment Failed");
                    errorAlert.setHeaderText(null);
                    errorAlert.setContentText("Failed to connect to the payment server. Please try again later.");
                    errorAlert.show();
                });
                new Thread(paymentTask).start();
            } else if (response == btnCancelOrder) {
                // Hủy giao dịch
                Alert confirmCancel = new Alert(Alert.AlertType.WARNING, "Are you sure you want to cancel this winning bid? This action cannot be undone and may affect your account reputation.", ButtonType.YES, ButtonType.NO);
                confirmCancel.showAndWait().ifPresent(confirm -> {
                    if (confirm == ButtonType.YES) {
                        wonItem.setState(AuctionState.CANCELED);
                        handleShowWonAuctions(null); // Load lại màn hình
                        List<Item> currentItems = AuctionManager.getInstance().getAllItems();
                        loadProducts(currentItems);
                    }
                });
            }
        });
    }
}
