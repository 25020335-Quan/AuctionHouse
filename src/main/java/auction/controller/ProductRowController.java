package auction.controller;
import auction.model.AuctionManager;
import auction.model.item.Item;
import auction.model.service.DatabaseService;
import auction.model.state.AuctionState;
import auction.model.users.Admin;
import auction.model.users.Member;
import auction.model.users.User;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;

import static java.lang.String.format;

public class ProductRowController {
    private Item currentItem;
    private User currentUser;
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
    @FXML
    ImageView productImage;
    @FXML
    Button btnView;
    @FXML
    Button btnBid;
    @FXML
    Label timeLeft;
    @FXML
    Button btnEdit;
    private Timeline countdownTimeline;
    private MainScreenController parentController;

    public void setData(Item item, User user, MainScreenController mainController) {
        System.out.println("Kiểm tra thời gian món " + item.getName() + ": " + item.getStartTime());
        this.currentItem = item;
        this.parentController = mainController;
        productName.setText(item.getName());
        productPrice.setText(format("%,.0f VNĐ", item.getCurrentPrice()));
        productOwnerId.setText(item.getOwnerId());
        productId.setText(item.getId());
        productState.setText(item.getState().name());
        if (!productState.getStyleClass().contains("badge")) {
            productState.getStyleClass().add("badge");
        }
        productState.getStyleClass().removeAll("badge-pending", "badge-open", "badge-closed");
        switch (item.getState()) {
            case PENDING:
                productState.getStyleClass().add("badge-pending");
                break;
            case OPEN:
            case RUNNING:
                productState.getStyleClass().add("badge-open");
                break;
            case CLOSED:
            case SOLD:
                productState.getStyleClass().add("badge-closed");
                break;
            case CANCELED:
                productState.getStyleClass().add("badge-cancelled");
                break;
        }
        loadProductImage(item.getId());
        if (user instanceof Member) {
            this.currentUser = (Member) user;
        }
        if (user instanceof Admin) {
            this.currentUser = (Admin) user;
        }
        if (btnBid != null && currentUser != null) {
            // Xóa style
            btnBid.setStyle("");
            if (item.getOwnerId().equals(currentUser.getId()) || currentUser.getRole().equals("ADMIN")) {
                // Nếu là đồ của mình -> Biến thành nút xóa
                btnBid.setDisable(false);
                btnBid.setText("Delete");
                if (!btnBid.getStyleClass().contains("btn-delete")) {
                    btnBid.getStyleClass().add("btn-delete");
                }
                btnBid.setOnAction(event -> {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Confirm To Delete Product");
                    alert.setHeaderText(null);
                    alert.setContentText("Are you sure you want to delete \"" + item.getName() + "\" ?");
                    alert.showAndWait().ifPresent(response -> {
                        if (response == ButtonType.OK) {
                            AuctionManager.getInstance().removeItemByID(item.getId());
                            auction.model.service.DatabaseService dbService = new DatabaseService();
                            dbService.deleteItemById(item.getId());
                            // Gọi màn hình cha vẽ lại danh sách ngay lập tức để mất hình món đồ vừa xóa
                            if (mainController != null) {
                                mainController.refreshLocalUI();
                            }
                        }
                    });
                });
                if (btnEdit != null) {
                    btnEdit.setVisible(true);
                    btnEdit.setManaged(true);
                    btnEdit.setOnAction(this::openEditScreen);
                }
            } else {
                // Nếu là đồ của người khác: Giữ nguyên là nút Đặt giá (Bid) bình thường
                btnBid.setDisable(false);
                btnBid.setText("Bid");
                btnBid.setStyle("btn-bid"); // Reset lại style mặc định của nút đấu giá

                // Trỏ lại sự kiện mở phòng đấu giá
                btnBid.setOnAction(event -> openAuctionRoom(event));
                // ĐỒ CỦA NGƯỜI KHÁC: Giấu nút Sửa đi
                if (btnEdit != null) {
                    btnEdit.setVisible(false);
                    btnEdit.setManaged(false);
                    // Cởi áo CSS ra
                    btnEdit.getStyleClass().remove("btn-edit");

                }
            }
        }
        // Gọi hàm để bắt đầu chạy đồng hồ đếm ngược (Gọi sau khi đã setup xong nút Bid/Delete)
        if (item.getStartTime() != null && item.getEndTime() != null) {
            startCountdownTimer(item);
        } else {
            timeLeft.setText("Not Scheduled");
        }
    }

    // Hàm đếm ngược thời gian
    private void startCountdownTimer(Item item) {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }
        checkAndUpdateState(item);
        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            checkAndUpdateState(item);
        }));
        //Indefinite -> lặp lại hành động vô hạn lần
        //Chỉ dừng khi gọi stop()
        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();
    }
    public void checkAndUpdateState(Item item) {
        LocalDateTime now = LocalDateTime.now();
        boolean isOwner = (currentUser != null && (item.getOwnerId().equals(currentUser.getId()) || currentUser.getRole().equals("ADMIN")));

        // 1. TRẠNG THÁI: CHƯA BẮT ĐẦU (PENDING)
        if (item.getStartTime() != null && now.isBefore(item.getStartTime())) {
            // Tính thời gian đếm ngược đến lúc mở cửa
            java.time.Duration duration = java.time.Duration.between(now, item.getStartTime());
            long days = duration.toDays();
            long hours = duration.toHours() % 24;
            long minutes = duration.toMinutes() % 60;
            long seconds = duration.getSeconds() % 60;

            timeLeft.setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold; -fx-font-size: 11px;"); // Chữ vàng
            if (days > 0) {
                timeLeft.setText(String.format("Starts in: %d d %02d:%02d:%02d", days, hours, minutes, seconds));
            } else {
                timeLeft.setText(String.format("Starts in: %02d:%02d:%02d", hours, minutes, seconds));
            }

            // Ép UI hiển thị huy hiệu PENDING
            updateBadge(AuctionState.PENDING);

            // Người ngoài nhìn thấy thì khóa nút Bid lại
            if (btnBid != null && !isOwner) {
                btnBid.setDisable(true);
                btnBid.setText("Wait");
            }
        }
        // 2. TRẠNG THÁI: ĐÃ KẾT THÚC
        else if (item.getEndTime() != null && (now.isAfter(item.getEndTime()) || now.isEqual(item.getEndTime()))) {
            timeLeft.setText("Ended");
            timeLeft.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;"); // Chữ đỏ

            if (item.getState() == AuctionState.OPEN || item.getState() == AuctionState.RUNNING) {
                item.setState(AuctionState.CLOSED);
            }
            updateBadge(item.getState());

            // Xử lý nút thanh toán cho người thắng
            if (btnBid != null && !isOwner) {
                boolean isWinner = item.getHighestBidderId() != null && item.getHighestBidderId().equals(currentUser.getId());
                if (isWinner && item.getState() == AuctionState.CLOSED) {
                    btnBid.setDisable(false);
                    btnBid.setText("Pay Now");
                    btnBid.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold;");
                    btnBid.setOnAction(e -> parentController.handlePayment(item));
                } else if (isWinner && item.getState() == AuctionState.SOLD) {
                    btnBid.setDisable(true);
                    btnBid.setText("Paid");
                    btnBid.setStyle("-fx-background-color: #d1d5db; -fx-text-fill: #4b5563;");
                } else if (item.getState() == AuctionState.CANCELED) {
                    btnBid.setDisable(true);
                    btnBid.setText("Canceled");
                    btnBid.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-opacity: 0.7;");
                } else {
                    btnBid.setDisable(true);
                    btnBid.setText("Ended");
                }
            }
            if (countdownTimeline != null) {
                countdownTimeline.stop(); // Dừng đồng hồ
            }
        }
        // 3. TRẠNG THÁI: ĐANG DIỄN RA (OPEN/RUNNING)
        else if (item.getEndTime() != null) {
            // Tính thời gian đếm ngược đến lúc ĐÓNG CỬA
            java.time.Duration duration = java.time.Duration.between(now, item.getEndTime());
            long days = duration.toDays();
            long hours = duration.toHours() % 24;
            long minutes = duration.toMinutes() % 60;
            long seconds = duration.getSeconds() % 60;

            timeLeft.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;"); // Chữ xanh lá
            if (days > 0) {
                timeLeft.setText(String.format("%d:%02d:%02d:%02d", days, hours, minutes, seconds));
            } else {
                timeLeft.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
            }

            // Ép UI hiển thị huy hiệu OPEN
            if (item.getState() == AuctionState.PENDING) {
                item.setState(AuctionState.OPEN);
            }
            updateBadge(item.getState());

            // Mở nút Bid cho người ngoài vào chiến
            if (btnBid != null && !isOwner) {
                btnBid.setDisable(false);
                btnBid.setText("Bid");
                btnBid.setStyle(""); // Reset về style CSS mặc định
                btnBid.getStyleClass().add("btn-bid"); // Đảm bảo có class CSS
                btnBid.setOnAction(this::openAuctionRoom); // Trỏ lại sự kiện vào phòng
            }
        }
    }

    // Hàm phụ trợ để đổi màu huy hiệu trạng thái gọn gàng hơn
    private void updateBadge(AuctionState state) {
        if (productState != null) {
            productState.setText(state.name());
            productState.getStyleClass().removeAll("badge-pending", "badge-open", "badge-closed", "badge-cancelled");
            productState.setStyle("");

            switch (state) {
                case PENDING:
                    productState.getStyleClass().add("badge-pending");
                    break;
                case OPEN:
                case RUNNING:
                    productState.getStyleClass().add("badge-open");
                    break;
                case CLOSED:
                case SOLD:
                    productState.getStyleClass().add("badge-closed");
                    break;
                case CANCELED:
                    productState.getStyleClass().add("badge-cancelled");
                    break;
            }
        }
    }
    private void loadProductImage(String itemId) {
        if (currentItem != null && currentItem.getImageUrls() != null && !currentItem.getImageUrls().isEmpty()) {
            try {
                Image img = new Image(currentItem.getImageUrls().get(0), true);
                productImage.setImage(img);
            } catch (Exception e) {}
        } else {
            try {
                File defaultFile = new File("src/main/resources/images/no-image.jpg");
                if (defaultFile.exists()) productImage.setImage(new Image(defaultFile.toURI().toString()));
            } catch (Exception e) {}
        }
        //Bo tròn góc ảnh
        Rectangle clip = new Rectangle(77, 81);
        clip.setArcWidth(10);
        clip.setArcHeight(10);
        productImage.setClip(clip);
    }

    @FXML
    public void openAuctionRoom(ActionEvent event) {
        try {
            // 1. Tải giao diện phòng đấu giá
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/auctionRoom.fxml"));
            Parent root = loader.load();
            // 2. Chộp lấy Controller của phòng đấu giá
            AuctionRoomController roomController = loader.getController();
            roomController.initRoom(this.currentItem, this.currentUser);
            // 4. Lấy cửa sổ hiện tại (Stage)
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Live Auction: " + this.currentItem.getName());
            stage.show();

        } catch (IOException e) {
            System.err.println("Lỗi không thể mở phòng đấu giá!");
            e.printStackTrace();
        }
    }

    @FXML
    public void openViewItem(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/viewItem.fxml"));
            Parent root = loader.load();
            // Đưa Item sang cho màn hình View
            ViewItemController controller = loader.getController();
            controller.setItemData(this.currentItem);
            Stage stage = new Stage();
            stage.setTitle("Chi tiết sản phẩm: " + this.currentItem.getName());
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @FXML
    public void openEditScreen(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/editItem.fxml"));
            Parent root = loader.load();
            EditItemController controller = loader.getController();
            if (this.currentItem != null && this.parentController != null) {
                controller.setEditData(this.currentItem, this.parentController);
            }
            Stage stage = new Stage();
            stage.setTitle("Edit Item: " + this.currentItem.getName());
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}