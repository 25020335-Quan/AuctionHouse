package auction.controller;

import auction.model.AuctionManager;
import auction.model.item.Item;
import auction.model.transaction.BidTransaction;
import auction.model.users.Member;
import auction.model.users.User;
import auction.util.AutoBidRequest;
import auction.util.GetItemHistoryRequest;
import auction.util.SceneSwitcher;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;


public class AuctionRoomController {
    @FXML
    private Label timeLeftLabel;       // Hiển thị đồng hồ đếm ngược
    @FXML
    private Label leadingBidderLabel;  // Hiển thị người đang trả giá cao nhất
    private Timeline countdownTimeline;
    @FXML
    private Label titleLabel;
    @FXML
    private Label currentPriceLabel;
    private Item currentItem;
    private User currentBidder;
    @FXML
    private ImageView productAuctionImage;
    @FXML
    private TextField bidAmountField;
    @FXML
    private Button btnPlaceBid;
    @FXML
    private ListView<String> historyListView;
    @FXML
    private TextField maxBidField;
    @FXML
    private TextField incrementField;
    private ObservableList<String> bidHistoryItems = FXCollections.observableArrayList();

    // Trục X dùng String để chứa giờ:phút:giây, Trục Y dùng Number để chứa số tiền
    @FXML
    private javafx.scene.chart.LineChart<String, Number> priceChart;

    // Đường nét đứt nối các điểm giá trị trên biểu đồ
    private javafx.scene.chart.XYChart.Series<String, Number> priceSeries;


    /**
     * Hàm này KHÔNG gắn vào nút bấm nào cả.
     * Nó được gọi ở bên ngoài để lưu dữ liệu
     */
    public void initRoom(Item item, User user) {
        // Cất dữ liệu vào túi riêng để lát dùng
        this.currentItem = item;
        this.currentBidder = user;
        auction.client.AuctionClient.getInstance().setCurrentRoom(this);
        //Đưa thông tin lên giao diện
        if (item != null) {
            titleLabel.setText(item.getName());
            // Format số tiền
            currentPriceLabel.setText(String.format("%,.0f VND", item.getCurrentPrice()));
            if (item.getHighestBidderId() != null && !item.getHighestBidderId().trim().isEmpty()) {
                if (item.getHighestBidderId().equals(user.getId())) {
                    leadingBidderLabel.setText("You");
                    leadingBidderLabel.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;"); // Xanh lá chỉ mình
                } else {
                    leadingBidderLabel.setText("@" + item.getHighestBidderId());
                    leadingBidderLabel.setStyle("-fx-text-fill: #3b82f6; -fx-font-weight: bold;"); // Xanh dương chỉ người khác
                }
            } else {
                leadingBidderLabel.setText("No bids yet");
                leadingBidderLabel.setStyle("-fx-text-fill: #6b7280; -fx-font-weight: normal;");
            }
        }
        loadSingleProductImage(item.getId());
        historyListView.setItems(bidHistoryItems);
        bidHistoryItems.clear(); // Xóa lịch sử cũ nếu có

        // Dòng thông báo đầu tiên khi bước vào phòng
        bidHistoryItems.add("System: The system has opened the auction room.");
        // Kích hoạt hàm đồng hồ đếm ngược khi vào phòng
        if (item.getStartTime() != null && item.getEndTime() != null) {
            startCountdownTimer(item);
        } else {
            if (timeLeftLabel != null) timeLeftLabel.setText("Not Scheduled");
            btnPlaceBid.setDisable(true); // Khóa nút luôn nếu không có thời gian
        }

        //Set up biểu đồ giá của sản phẩm
        if (priceChart != null) {
            // Dọn sạch dữ liệu cũ
            priceChart.getData().clear();
            // Tắt animation
            priceChart.setAnimated(false);
            priceSeries = new javafx.scene.chart.XYChart.Series<>();
            priceSeries.setName("Price Fluctuations");
            List<BidTransaction> history = new java.util.ArrayList<>();
            try {
                auction.util.GetItemHistoryRequest req = new auction.util.GetItemHistoryRequest(item.getId());
                Object response = auction.client.AuctionClient.getInstance().sendRequest(req);
                if (response instanceof List) {
                    history = (List<BidTransaction>) response;
                }
            } catch (Exception e) {
                System.err.println("Không thể tải lịch sử biểu đồ từ Server: " + e.getMessage());
            };

            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss");
            String startTimeStr = item.getStartTime().format(formatter);
            double startPrice = item.getStartingPrice();
            System.out.println(startPrice);
            priceSeries.getData().add(new javafx.scene.chart.XYChart.Data<>(startTimeStr, startPrice));

            history.sort(Comparator.comparing(BidTransaction::getBidTime));

            for (BidTransaction tx : history) {
                String txTime = tx.getBidTime().format(formatter);
                // Đưa tọa độ (Thời gian, Giá tiền) vào đường vẽ biểu đồ
                priceSeries.getData().add(new javafx.scene.chart.XYChart.Data<>(txTime, tx.getBidAmount()));
            }

//                String currentTime = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
//                if (priceSeries.getData().isEmpty()) {
//                    java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
//                    priceSeries.getData().add(new javafx.scene.chart.XYChart.Data<>(currentTime, item.getCurrentPrice()));
//                }
//                //Tạo điểm đầu tiên
//                priceSeries.getData().add(new javafx.scene.chart.XYChart.Data<>(currentTime, item.getCurrentPrice()));
//                // Gắn đường vẽ vào biểu đồ
            priceChart.getData().add(priceSeries);

        }
    }
    public Item getCurrentItem() {
        return currentItem;
    }

    @FXML
    private void handleBack(ActionEvent event) {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }
        auction.client.AuctionClient.getInstance().setCurrentRoom(null);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/mainscreen.fxml"));
            Parent root = loader.load();
            MainScreenController mainController = loader.getController();
            mainController.setLoggedInUser(this.currentBidder);
            mainController.displayName(this.currentBidder.getUsername());
            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Marketplace");
            stage.show();

        } catch (IOException e) {
            System.err.println("Lỗi: Không thể quay lại màn hình Marketplace!");
            e.printStackTrace();
        }
    }

    private void loadSingleProductImage(String itemId) {
        if (currentItem != null && currentItem.getImageUrls() != null && !currentItem.getImageUrls().isEmpty()) {
            try {
                Image img = new Image(currentItem.getImageUrls().get(0), true);
                productAuctionImage.setImage(img);
            } catch (Exception e) {}
        } else {
            try {
                File defaultFile = new File("src/main/resources/images/no-image.jpg");
                if (defaultFile.exists()) productAuctionImage.setImage(new Image(defaultFile.toURI().toString()));
            } catch (Exception e) {}
        }
        Rectangle clip = new Rectangle(productAuctionImage.getFitWidth(), productAuctionImage.getFitHeight());
        clip.setArcHeight(10);
        clip.setArcWidth(10);
        productAuctionImage.setClip(clip);
    }

    @FXML
    public void handlePlaceBid(ActionEvent event) {
        if (currentBidder == null || currentItem == null) {
            System.err.println("Lỗi: Chưa có thông tin người dùng hoặc sản phẩm!");
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(currentItem.getStartTime())) {
            showErrorAlert("Bid Denied", "This auction room has not started yet!");
            return;
        }
        if (now.isAfter(currentItem.getEndTime())) {
            showErrorAlert("Bid Denied", "This auction room has already closed!");
            return;
        }

        // Chặn chủ phòng tự đấu giá đồ của mình
        if (currentItem.getOwnerId().equals(currentBidder.getId())) {
            showErrorAlert("Bid Rejected", "You cannot place a bid on your own product!");
            return;
        }
        String input = bidAmountField.getText().trim();
        if (input.isEmpty()) {
            showErrorAlert("Invalid Input", "Please enter a bid amount.");
            return;
        }

        try {
            double oldPrice = currentItem.getCurrentPrice();
            double amount = Double.parseDouble(input);
            auction.util.BidRequest req = new auction.util.BidRequest(currentItem.getId(), currentBidder.getId(), amount);

            // Gửi đi và chờ Server phán xét
            Object response = auction.client.AuctionClient.getInstance().sendRequest(req);

            // 2. NHẬN KẾT QUẢ TỪ SERVER
            if (response instanceof String msg) {
                if (msg.equals("SUCCESS")) {
                    // Nếu thành công, chỉ cần xóa trắng ô nhập.
                    // Luồng ngầm (AuctionClient) sẽ tự nghe loa BidNotification và chọc vào hàm refreshPriceFromNetwork sau!
                    bidAmountField.clear();
                } else if (msg.startsWith("LỖI:")) {
                    // Nếu Server bắt lỗi (giá thấp, hết giờ, tự mua đồ mình...)
                    showErrorAlert("Bid Rejected", msg.replace("LỖI:", ""));
                } else {
                    showErrorAlert("Lỗi Hệ Thống", msg);
                }
            }

        } catch (NumberFormatException e) {
            showErrorAlert("Invalid Format", "Please enter a valid numeric value.");
        } catch (Exception e) {
            showErrorAlert("Lỗi Mạng", "Mất kết nối tới Server!");
            e.printStackTrace();
        }
    }

    public void refreshPriceFromNetwork(String bidderId, double newPrice) {
        javafx.application.Platform.runLater(() -> {
            currentPriceLabel.setText(String.format("%,.0f VND", newPrice));
            // Kiểm tra xem ID người vừa đặt có phải là tài khoản mình đang đăng nhập không
            if (bidderId.equals(this.currentBidder.getId())) {
                leadingBidderLabel.setText("You");
                leadingBidderLabel.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
            } else {
                leadingBidderLabel.setText("@" + bidderId);
                leadingBidderLabel.setStyle("-fx-text-fill: #3b82f6; -fx-font-weight: bold;");
            }
            String logMessage;
            if (bidderId.equals(this.currentBidder.getId())) {
                logMessage = String.format(" You just bid: %,.0f VND", newPrice); // Tự mình đặt
            } else {
                logMessage = String.format(" User ID @%s just bid: %,.0f VND", bidderId, newPrice); // Người khác đặt
            }
            bidHistoryItems.add(0, logMessage);

            // Cập nhật biểu đồ gía
            if (priceSeries != null) {
                // Lấy khoảnh khắc vừa có người đặt giá thành công
                String timeNow = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
                //  Thêm một điểm mới để nối từ đường biểu đồ có sẵn
                priceSeries.getData().add(new javafx.scene.chart.XYChart.Data<>(timeNow, newPrice));
            }


        });
    }

    private void startCountdownTimer(Item item) {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }
        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            LocalDateTime now = LocalDateTime.now();

            // Chưa đến giờ mở phiên
            if (now.isBefore(item.getStartTime())) {
                btnPlaceBid.setDisable(true); // Khóa nút đặt giá
                java.time.Duration duration = java.time.Duration.between(now, item.getStartTime());
                long days = duration.toDays();
                long hours = duration.toHours() % 24;
                long minutes = duration.toMinutes() % 60;
                long seconds = duration.getSeconds() % 60;

                if (timeLeftLabel != null) {
                    timeLeftLabel.setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold;"); // Chữ màu vàng
                    if (days > 0) {
                        timeLeftLabel.setText(String.format("Starts in: %d d %02d:%02d:%02d", days, hours, minutes, seconds));
                    } else {
                        timeLeftLabel.setText(String.format("Starts in: %02d:%02d:%02d", hours, minutes, seconds));
                    }
                }
            }
            // Phiên đã kết thúc
            else if (now.isAfter(item.getEndTime()) || now.isEqual(item.getEndTime())) {
                if (timeLeftLabel != null) {
                    timeLeftLabel.setText("Ended");
                    timeLeftLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;"); // Chữ màu đỏ
                }
                btnPlaceBid.setDisable(true); // Khóa nút
                countdownTimeline.stop();
            }
            // Phiên đang diễn ra
            else {
                btnPlaceBid.setDisable(false); // Mở khóa nút bấm công khai

                java.time.Duration duration = java.time.Duration.between(now, item.getEndTime());
                long days = duration.toDays();
                long hours = duration.toHours() % 24;
                long minutes = duration.toMinutes() % 60;
                long seconds = duration.getSeconds() % 60;

                if (timeLeftLabel != null) {
                    timeLeftLabel.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;"); // Chữ xanh lá sôi động
                    if (days > 0) {
                        timeLeftLabel.setText(String.format("%d:%02d:%02d:%02d", days, hours, minutes, seconds));
                    } else {
                        timeLeftLabel.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
                    }
                }
            }
        }));
        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();
    }
    private void showErrorAlert(String title, String content) {
        javafx.application.Platform.runLater(() -> {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }


    // Tính năng nâng cao Auto-Bid
    @FXML
    public void handleSetAutoBid(ActionEvent event) {
        try {
            // Kiểm tra người dùng có để trống ô nhập không
            if (maxBidField.getText().trim().isEmpty() || incrementField.getText().trim().isEmpty()) {
                showErrorAlert("Invalid Input", "Please enter both Max Bid and Increment values!");
                return;
            }

            double maxBid = Double.parseDouble(maxBidField.getText().trim());
            double increment = Double.parseDouble(incrementField.getText().trim());

            // Lọc dữ liệu input
            if (increment <= 0) {
                showErrorAlert("Invalid Increment", "Increment amount must be greater than 0!");
                return;
            }
            if (maxBid <= currentItem.getCurrentPrice()) {
                showErrorAlert("Invalid Max Bid", "Max Auto-Bid must be strictly higher than the current price!");
                return;
            }

            // Tạo gói tin gửi lên cho Server
            AutoBidRequest request = new AutoBidRequest(currentItem.getId(), currentBidder.getId(), maxBid, increment);

            // Đưa lệnh gọi mạng vào luồng chạy ngầm (Task)
            // Chạy ngầm để không gây ảnh hưởng đến UI
            javafx.concurrent.Task<Object> sendTask = new javafx.concurrent.Task<>() {
                @Override
                protected Object call() throws Exception {
                    return auction.client.AuctionClient.getInstance().sendRequest(request);
                }
            };

            // 4. Xử lý khi luồng ngầm chạy XONG
            sendTask.setOnSucceeded(e -> {
                Object response = sendTask.getValue();

                if (response instanceof String && (response.equals("SUCCESS") || response.equals("AUTOBID_SUCCESS"))) {
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                    alert.setTitle("Success");
                    alert.setHeaderText(null);
                    alert.setContentText("Auto-Bid activated successfully! The system will automatically place bids on your behalf.");
                    alert.showAndWait();

                    maxBidField.clear();
                    incrementField.clear();
                } else {
                    showErrorAlert("Request Denied", "Server rejected the request: " + response);
                }
            });

            // Xử lý khi luồng ngầm BỊ LỖI
            sendTask.setOnFailed(e -> {
                Throwable ex = sendTask.getException();
                if (ex instanceof java.io.IOException) {
                    showErrorAlert("Connection Error", "Unable to connect to the server. Please check your network!");
                    System.err.println("Network error: " + ex.getMessage());
                } else if (ex instanceof ClassNotFoundException) {
                    showErrorAlert("System Error", "Invalid response packet received!");
                    System.err.println("Data error: " + ex.getMessage());
                } else {
                    showErrorAlert("Error", "An unknown error occurred!");
                }
            });

            // Khởi động luồng
            new Thread(sendTask).start();

        } catch (NumberFormatException e) {
            showErrorAlert("Invalid Format", "Please enter valid numeric values only.");
        }
    }
}