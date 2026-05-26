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
    private Member currentBidder;
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
    public void initRoom(Item item, Member user) {
        // Cất dữ liệu vào túi riêng để lát dùng
        this.currentItem = item;
        this.currentBidder = user;
        auction.client.AuctionClient.getInstance().setCurrentRoom(this);
        //Đưa thông tin lên giao diện
        if (item != null) {
            titleLabel.setText(item.getName());
            // Format số tiền
            currentPriceLabel.setText(String.format("%,.0f VND", item.getCurrentPrice()));
            if (item.getHighestBidderId() != null && !item.getHighestBidderId().isEmpty()) {
                if (item.getHighestBidderId().equals(user.getId())) {
                    leadingBidderLabel.setText("You");
                    leadingBidderLabel.setStyle("-fx-text-fill: #3b82f6; -fx-font-weight: bold;");
                }
            } else {
                leadingBidderLabel.setText("@" + item.getHighestBidderId());
                leadingBidderLabel.setStyle("-fx-text-fill: #000000; -fx-font-weight: normal;");
            }
        } else {
            // Nếu là sản phẩm mới, chưa có người bid
            leadingBidderLabel.setText("No bids yet");
            leadingBidderLabel.setStyle("-fx-text-fill: #6b7280; -fx-font-weight: normal;");
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



            for (BidTransaction tx : history) {
                String txTime = tx.getBidTime().format(formatter);
                // Đưa tọa độ (Thời gian, Giá tiền) vào đường vẽ biểu đồ
                priceSeries.getData().add(new javafx.scene.chart.XYChart.Data<>(txTime, tx.getBidAmount()));
            }

                String currentTime = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
                if (priceSeries.getData().isEmpty()) {
                    java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
                    priceSeries.getData().add(new javafx.scene.chart.XYChart.Data<>(currentTime, item.getCurrentPrice()));
                }
                //Tạo điểm đầu tiên
                priceSeries.getData().add(new javafx.scene.chart.XYChart.Data<>(currentTime, item.getCurrentPrice()));
                // Gắn đường vẽ vào biểu đồ
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
        String[] extensions = {".jpg", ".png", ".jpeg"};
        boolean isImageFound = false;
        // Đi dò đúng ảnh với các đuôi file
        for (String ext : extensions) {
            String path = "src/main/resources/images/" + itemId + "_0" + ext;
            File imgFile = new File(path);

            if (imgFile.exists()) {
                Image img = new Image(imgFile.toURI().toString());
                productAuctionImage.setImage(img);
                isImageFound = true;
                break; // Tìm thấy ảnh rồi thì dừng vòng lặp luôn
            }
        }
        // Nếu sản phẩm không có ảnh, nạp ảnh mặc định
        if (!isImageFound) {
            File defaultFile = new File("src/main/resources/images/no-image.jpg");
            if (defaultFile.exists()) {
                productAuctionImage.setImage(new Image(defaultFile.toURI().toString()));
            }
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
            // Nếu đã kết thúc
            if (now.isAfter(item.getEndTime()) || now.isEqual(item.getEndTime())) {
                if (timeLeftLabel != null) {
                    timeLeftLabel.setText("Ended");
                }
                btnPlaceBid.setDisable(true); // Khóa nút Place Bid ngay
                countdownTimeline.stop();
            } else {
                btnPlaceBid.setDisable(false); // Mở nút

                java.time.Duration duration = java.time.Duration.between(now, item.getEndTime());
                long days = duration.toDays();
                long hours = duration.toHours() % 24;
                long minutes = duration.toMinutes() % 60;
                long seconds = duration.getSeconds() % 60;

                if (timeLeftLabel != null) {
                    if (days > 0) {
                        timeLeftLabel.setText(String.format("%d:%02d:%02d:%02d", days, hours, minutes, seconds));
                    } else {
                        timeLeftLabel.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
                    }
                }
            }
        }));
        // Chạy vô hạn lần
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
            double maxBid = Double.parseDouble(maxBidField.getText().trim());
            double increment = Double.parseDouble(incrementField.getText().trim());
            // Kiểm tra điều kiện đấu giá
            if (maxBid <= currentItem.getCurrentPrice()) {
                showErrorAlert("Lỗi", "Giá tối đa (Max Bid) phải lớn hơn giá hiện tại!");
                return;
            }
            // Tạo gói tin cho Server
            AutoBidRequest request = new AutoBidRequest(currentItem.getId(), currentBidder.getId(), maxBid, increment);
            //Send request cho Server
            auction.client.AuctionClient.getInstance().sendRequest(request);
            // Báo thành công và xóa trắng ô nhập
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
            alert.setContentText("Auto-Bid activated successfully! The system will automatically place bids on your behalf.");
            alert.showAndWait();

            maxBidField.clear();
            incrementField.clear();
        } catch (NumberFormatException e) {
            showErrorAlert("Lỗi nhập liệu", "Vui lòng nhập số hợp lệ.");
        }
        catch(IOException e){
            System.err.println("Network error while sending Auto-Bid: " + e.getMessage());
            showErrorAlert("Connection Error", "Unable to connect to the server. Please check your internet connection!");

        } catch (ClassNotFoundException e) {
            System.err.println("Data error: " + e.getMessage());
            showErrorAlert("System Error", "Invalid response packet received!");
        }
    }
}