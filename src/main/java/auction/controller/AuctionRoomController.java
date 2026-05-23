package auction.controller;

import auction.model.item.Item;
import auction.model.users.Member;
import auction.model.users.User;
import auction.util.SceneSwitcher;
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

import java.io.File;
import java.io.IOException;

public class AuctionRoomController {
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
            loadSingleProductImage(item.getId());
            historyListView.setItems(bidHistoryItems);
            bidHistoryItems.clear(); // Xóa lịch sử cũ nếu có

            // Dòng thông báo đầu tiên khi bước vào phòng
            bidHistoryItems.add("System: The system has opened the auction room.");
        }
    }
    @FXML
    private void handleBack(ActionEvent event) {
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
        try{
            double oldPrice = currentItem.getCurrentPrice();
            double amount = Double.parseDouble(bidAmountField.getText().trim());
            currentBidder.bid(currentItem, amount);
            if (currentItem.getCurrentPrice() > oldPrice) {
                refreshPriceFromNetwork(currentBidder.getUsername(), amount);
            }
            else{
                System.out.println("Đặt giá thất bại (có thể do sai bước giá hoặc tự bid đồ của mình).");
            }
        }
        catch (NumberFormatException e){
            System.err.println("Số tiền nhập vào không hợp lệ!");
        }

    }
    public void refreshPriceFromNetwork(String bidderName, double newPrice){
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
        });
    }

}