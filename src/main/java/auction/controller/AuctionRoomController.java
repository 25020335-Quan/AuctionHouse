package auction.controller;

import auction.model.item.Item;
import auction.model.users.Member;
import auction.model.users.User;
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

public class AuctionRoomController {
    @FXML private Label timeLeftLabel;       // Hiển thị đồng hồ đếm ngược
    @FXML private Label leadingBidderLabel;  // Hiển thị người đang trả giá cao nhất
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


        //Đưa thông tin lên giao diện
        if (item != null) {
            titleLabel.setText(item.getName());
            // Format số tiền
            currentPriceLabel.setText(String.format("%,.0f VND", item.getCurrentPrice()));
            if (item.getHighestBidderName() != null && !item.getHighestBidderName().isEmpty()) {
                if (item.getHighestBidderName().equals(user.getUsername())) {
                    leadingBidderLabel.setText("You");
                    leadingBidderLabel.setStyle("-fx-text-fill: #3b82f6; -fx-font-weight: bold;");
                }
            } else {
                leadingBidderLabel.setText("@" + item.getHighestBidderName());
                leadingBidderLabel.setStyle("-fx-text-fill: #000000; -fx-font-weight: normal;");
            }
        }else {
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
                if(timeLeftLabel != null) timeLeftLabel.setText("Not Scheduled");
                btnPlaceBid.setDisable(true); // Khóa nút luôn nếu không có thời gian
            }

            //Set up biểu đồ giá của sản phẩm
            if (priceChart != null) {
                // Dọn sạch dữ liệu cũ
                priceChart.getData().clear();
                priceSeries = new javafx.scene.chart.XYChart.Series<>();
                priceSeries.setName("Price Fluctuations");
                //Lấy thời điểm hiện tại làm mốc
                String currentTime = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
                //Tạo điểm đầu tiên
                priceSeries.getData().add(new javafx.scene.chart.XYChart.Data<>(currentTime, item.getCurrentPrice()));
                // Gắn đường vẽ vào biểu đồ
                priceChart.getData().add(priceSeries);
                // Tắt animation
                priceChart.setAnimated(false);

            }

        }
    @FXML
    private void handleBack(ActionEvent event) {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }
        try{
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

        try{
            double oldPrice = currentItem.getCurrentPrice();
            double amount = Double.parseDouble(input);
            if (amount <= 0) {
                showErrorAlert("Invalid Amount", "The bid amount must be greater than 0.");
                return;
            }
            if (amount <= oldPrice) {
                showErrorAlert("Bid Too Low", String.format("Your bid must be higher than the current price (%,.0f VND).", oldPrice));
                return;
            }
            currentBidder.bid(currentItem, amount);
            if (currentItem.getCurrentPrice() > oldPrice) {
                // Tính năng nâng cao : Anti-Sniping
                java.time.Duration timeRemaining = java.time.Duration.between(LocalDateTime.now(), currentItem.getEndTime());
                // Nếu thời gian còn lại nhỏ hơn hoặc bằng 30 giây
                if (timeRemaining.getSeconds() <= 30 && timeRemaining.getSeconds() > 0) {
                    // Tự động cộng thêm 60 giây vào giờ kết thúc
                    currentItem.setEndTime(currentItem.getEndTime().plusSeconds(60));
                    bidHistoryItems.add(0, "System: Anti-sniping activated! +60 seconds added.");
                }
                refreshPriceFromNetwork(currentBidder.getUsername(), amount);
                bidAmountField.clear();
            }
            else{
                showErrorAlert("Bid Rejected", "Your bid failed. You might be bidding on your own item or violating a bidding rule.");
            }
        }
        catch (NumberFormatException e){
            showErrorAlert("Invalid Format", "Please enter a valid numeric value. Letters and special characters are not allowed.");
        }

    }
    public void refreshPriceFromNetwork(String bidderName, double newPrice){
        if (currentItem != null) {
            currentItem.setPrice(newPrice);
            currentItem.setHighestBidderName(bidderName);
        }
        javafx.application.Platform.runLater(() -> {
            currentPriceLabel.setText(String.format("%,.0f VND", newPrice));
            // Kiểm tra xem ID người vừa đặt có phải là tài khoản mình đang đăng nhập không
            String logMessage;
            if (bidderName.equals(this.currentBidder.getUsername())) {
                logMessage = String.format(" You just bid: %,.0f VND", newPrice); // Tự mình đặt
            } else {
                logMessage = String.format(" @%s just bid: %,.0f VND", bidderName, newPrice); // Người khác đặt
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
    private void startCountdownTimer(Item item){
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }
        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            LocalDateTime now = LocalDateTime.now();
            // Nếu đã kết thúc
            if (now.isAfter(item.getEndTime()) || now.isEqual(item.getEndTime())) {
                if(timeLeftLabel != null) {
                    timeLeftLabel.setText("Ended");
                }
                btnPlaceBid.setDisable(true); // Khóa nút Place Bid ngay
                countdownTimeline.stop();
            }
            else {
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
}