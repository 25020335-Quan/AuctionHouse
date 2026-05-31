package auction.controller;
import auction.client.AuctionClient;
import auction.model.item.*;
import auction.model.*;
import auction.model.state.AuctionState;
import auction.model.users.Member;
import auction.model.users.User;
import auction.util.GetItemListRequest;
import auction.util.SceneSwitcher;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
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
    private StackPane mainStackPane;
    @FXML
    private TextField searchField;
    @FXML
    private Button btnBell;
    @FXML
    private Label lblBadge;
    @FXML
    private ListView<String> notificationListView;
    // Danh sách động để lưu trữ các chuỗi thông báo trên RAM Client
    private static ObservableList<String> savedNotifications = FXCollections.observableArrayList();

    // Biến đếm số thông báo chưa đọc
    private static int unreadCount = 0;

    @FXML
    public void initialize() {
        if (marketplacePane != null) {
            marketplacePane.toFront();
        }
        setupRealTimeSearch();
        if (notificationListView != null) {
            //Đưa danh sách lưu trữ vào ListView giao diện
            notificationListView.setItems(savedNotifications);
            notificationListView.setVisible(false); // Mặc định ẩn hộp thư đi khi mới vào app
            notificationListView.setManaged(false);
        }
        if (lblBadge != null) {
            if (unreadCount > 0) {
                lblBadge.setText(String.valueOf(unreadCount));
                lblBadge.setVisible(true);
                lblBadge.toFront();
            } else {
                lblBadge.setVisible(false);
            }
        }
        javafx.application.Platform.runLater(() -> {
            auction.client.AuctionClient.getInstance().setMainScreen(this);
        });
    }

    @FXML
    public void displayName(String name) {
        menuButton.setText("Hello, " + name);
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
                boolean isMyItem = (currentUser != null && (item.getOwnerId().equals(currentUser.getId()) || currentUser.getRole().equals("ADMIN")));

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
        auction.client.AuctionClient.getInstance().setMainScreen(this);
        refreshUI();
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
        try {
            // Xóa cái giao diện Profile cũ đang nằm trong StackPane (nếu có)
            mainStackPane.getChildren().removeIf(node -> "profileNode".equals(node.getId()));

            // Tải file giao diện profile.fxml độc lập từ bên ngoài vào
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/profile.fxml"));
            javafx.scene.Parent profileRoot = loader.load();

            // Gắn thẻ tên cho nó là "profileNode" để lần sau gọi removeIf nó biết đường xóa
            profileRoot.setId("profileNode");

            // Lấy cái Controller của Profile ra và bơm dữ liệu User hiện tại vào
            auction.controller.ProfileController profileController = loader.getController();
            profileController.setUserData(this.currentUser);

            //Đưa giao diện vừa tải vào StackPane và ép nó nổi lên trên cùng
            mainStackPane.getChildren().add(profileRoot);
            profileRoot.toFront();

        } catch (java.io.IOException e) {
            System.err.println("Lỗi load file FXML của Profile!");
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Lỗi khi mở màn hình Profile!");
            e.printStackTrace();
        }
    }

    @FXML
    public void handleShowWonAuctions(ActionEvent event) {
        wonAuctionsPane.toFront();
        setActiveButton(btnWonAuctions);

        applySearchFilter(searchField.getText());
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
        auction.model.service.DatabaseService dbService = new auction.model.service.DatabaseService();
        dbService.loadAllItemsToManager();
        List<Item> allItems = AuctionManager.getInstance().getAllItems();
        // Nếu người dùng xóa trắng thanh search thì hiện lại toàn bộ danh sách gốc
        if (keyWord == null || keyWord.trim().isEmpty()) {
            loadProducts(allItems);
            loadWonAuctions(allItems);
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
        loadWonAuctions(filteredList);

    }

    // Thêm hàm để nhận hàng mới từ AddItemController
    public void addNewItemLocally(Item newItem) {
        //Cất vào kho Local của AuctionManager
        AuctionManager.getInstance().addItem(newItem);
        //Ép bộ lọc chạy lại.
        applySearchFilter(searchField.getText());
    }

    public void refreshLocalUI() {
        auction.model.service.DatabaseService dbService = new auction.model.service.DatabaseService();
        dbService.loadAllItemsToManager();
        if (searchField != null) {
            applySearchFilter(searchField.getText());
        } else {
            loadProducts(AuctionManager.getInstance().getAllItems());
        }
    }

    public void handlePayment(Item wonItem, User bidder) {
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
                        auction.model.service.DatabaseService dbService = new auction.model.service.DatabaseService();
                        boolean isDeducted = dbService.deductBalance(bidder.getId(), wonItem.getCurrentPrice());
                        if (isDeducted) {
                            dbService.deductBalance(bidder.getId(), wonItem.getCurrentPrice());
                            bidder.setBalance(bidder.getBalance() - wonItem.getCurrentPrice());
                            dbService.updateItemState(wonItem.getId(), "SOLD");
                            wonItem.setState(AuctionState.SOLD);
                            dbService.updateItemState(wonItem.getId(), "SOLD");
                            Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                            successAlert.setTitle("Payment successful");
                            successAlert.setHeaderText(null);
                            successAlert.setContentText("Payment successful! Your order ID is #" + wonItem.getId());
                            successAlert.showAndWait();
                            // Load lại màn hình cập nhật lại nút
                            handleShowWonAuctions(null);
                            dbService.loadAllItemsToManager();
                            List<Item> currentItems = AuctionManager.getInstance().getAllItems();
                            loadProducts(currentItems);
                        } else {
                            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                            errorAlert.setTitle("Payment Failed");
                            errorAlert.setHeaderText("Insufficient Balance!");
                            errorAlert.setContentText("Your wallet has insufficient funds to pay for this order. Please top up your account.");
                            errorAlert.showAndWait();
                        }
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
                auction.model.service.DatabaseService dbService = new auction.model.service.DatabaseService();
                Alert confirmCancel = new Alert(Alert.AlertType.WARNING, "Are you sure you want to cancel this winning bid? This action cannot be undone and may affect your account reputation.", ButtonType.YES, ButtonType.NO);
                confirmCancel.showAndWait().ifPresent(confirm -> {
                    if (confirm == ButtonType.YES) {
                        wonItem.setState(AuctionState.CANCELED);
                        dbService.updateItemState(wonItem.getId(), "CANCELED");
                        handleShowWonAuctions(null); // Load lại màn hình
                        dbService.loadAllItemsToManager();
                        List<Item> currentItems = AuctionManager.getInstance().getAllItems();
                        loadProducts(currentItems);
                    }
                });
            }
        });
    }

    public void loadWonAuctions(List<Item> itemList) {
        if (wonAuctionsContainer != null) {
            wonAuctionsContainer.getChildren().clear(); // Xóa sạch đồ cũ trước khi load đồ đã lọc
        }
        for (Item item : itemList) {
            // Kiểm tra phiên đấu giá đã kết thúc (CLOSED, SOLD, CANCELED)
            boolean isEnded = (item.getState() == AuctionState.CLOSED ||
                    item.getState() == AuctionState.SOLD ||
                    item.getState() == AuctionState.CANCELED);

            // Kiểm tra mình có phải người thắng cuộc không
            boolean isWinner = item.getHighestBidderId() != null && currentUser != null &&
                    item.getHighestBidderId().equals(currentUser.getId());

            if (isEnded && isWinner) {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/productRow.fxml"));
                    HBox row = loader.load();

                    ProductRowController rowController = loader.getController();
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
    }

    public void showToast(String message) {
        try {
            // 1. Tạo một Label chứa nội dung thông báo
            Label toastLabel = new Label(message);
            toastLabel.getStyleClass().add("toast-label");

            StackPane toastPane = new StackPane(toastLabel);
            toastPane.getStyleClass().add("toast-container");
            toastPane.setMouseTransparent(true);
            toastPane.setAlignment(Pos.BOTTOM_RIGHT);

            // 2. Tìm thẻ gốc an toàn nhất để dán lên
            javafx.scene.layout.Pane rootPane = (javafx.scene.layout.Pane) marketplacePane.getScene().getRoot();
            rootPane.getChildren().add(toastPane);

            // 3. Đặt thời gian hiện trong 3 giây rồi biến mất
            PauseTransition delay = new PauseTransition(Duration.seconds(3));
            delay.setOnFinished(e -> {
                rootPane.getChildren().remove(toastPane);
            });
            delay.play();

        } catch (Exception e) {
            System.out.println("LỖI HIỂN THỊ TOAST: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void addNotification(String message) {
        // Nhét thông báo mới lên đầu danh sách
        savedNotifications.add(0, message);
        // Tăng số lượng tin nhắn chưa đọc và cập nhật nhãn nút
        unreadCount++;
        if (lblBadge != null) {
            lblBadge.setText(String.valueOf(unreadCount)); // Hiện số
            lblBadge.setVisible(true); // Gỡ tàng hình cho cái chấm đỏ
        }

    }

    @FXML
    public void handleToggleNotificationBox(ActionEvent event) {
        if (notificationListView == null) return;

        // Kiểm tra xem đang đóng hay mở
        boolean isCurrentlyVisible = notificationListView.isVisible();

        // Đảo ngược trạng thái
        notificationListView.setVisible(!isCurrentlyVisible);

        notificationListView.setManaged(!isCurrentlyVisible);

        if (!isCurrentlyVisible) { // Nếu vừa ra lệnh MỞ hộp thư
            notificationListView.toFront(); // Ép nó nổi lên trên tất cả các khung khác

            // Xóa chấm đỏ
            unreadCount = 0;
            if (lblBadge != null) {
                lblBadge.setVisible(false);
            }
        }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        // Hiện hộp xác nhận
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText(null);
        alert.setContentText("Are you sure you want to log out?");

        // Nếu người dùng bấm OK
        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            // Xóa thông tin người dùng hiện tại ở màn hình này
            this.currentUser = null;
            // Xóa các con trỏ màn hình đang gắn
            // Tránh việc luồng ngầm vẫn cố cập nhật UI của màn hình cũ gây lỗi
            auction.client.AuctionClient.getInstance().setMainScreen(null);
            auction.client.AuctionClient.getInstance().setCurrentRoom(null);

            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml")); // Nhớ check chữ hoa/thường ở đây
                Parent root = loader.load();

                // Lấy cửa sổ (Stage) hiện tại dựa vào cái menuButton (vì chắc chắn nó không bị null)
                Stage stage = (Stage) menuButton.getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.setTitle("Login");
                stage.sizeToScene();     // Tự động co cửa sổ vừa khít với kích thước của file login.fxml
                stage.centerOnScreen();
                stage.show();

            } catch (Exception e) {
                System.err.println("Lỗi khi mở màn hình Login: " + e.getMessage());
                e.printStackTrace(); // In ra log đỏ để dễ soi lỗi nếu sai tên file
            }
        }
    }
    @FXML
    private void handleRefresh(ActionEvent event) {

    }
}
