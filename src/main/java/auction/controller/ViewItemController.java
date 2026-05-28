package auction.controller;

import auction.model.item.Item;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

import java.io.File;

public class ViewItemController {
    @FXML
    private Label titleLabel;
    @FXML
    private StackPane mainImagePane;
    @FXML
    private HBox thumbnailBox;
    @FXML
    private Label descLabel;
    private Item currentItem;

    public void setItemData(Item item) {
        this.currentItem = item;
        titleLabel.setText(item.getName());
        descLabel.setText(item.getDescription() != null && !item.getDescription().isEmpty()
                ? item.getDescription()
                : "There is no description available for this product");
        loadImages(item.getId());
    }

    public void loadImages(String itemID) {
        mainImagePane.getChildren().clear();
        thumbnailBox.getChildren().clear();
        thumbnailBox.setSpacing(10);
        boolean hasAnyImage = false;

        if (currentItem != null && currentItem.getImageUrls() != null) {
            for (String cloudLink : currentItem.getImageUrls()) {
                try {
                    Image img = new Image(cloudLink, true);
                    if (!hasAnyImage) {
                        setMainImage(img);
                        hasAnyImage = true;
                    }
                    createThumbnail(img);
                } catch (Exception e) {}
            }
        }
        if (!hasAnyImage) {
            File defaultFile = new File("src/main/resources/images/no-image.jpg");
            if (defaultFile.exists()) {
                setMainImage(new Image(defaultFile.toURI().toString()));
            }
        }
    }
    // Đưa ảnh lên khung to
    public void setMainImage(Image img){
        ImageView mainView = new ImageView(img);
        mainView.setFitWidth(300);
        mainView.setFitHeight(300);
        mainView.setPreserveRatio(true);

        mainImagePane.getChildren().clear();
        mainImagePane.getChildren().add(mainView);
    }

    //Tạo ảnh nhỏ và cài sự kiện bấm chuột
    public void createThumbnail(Image img){
        ImageView thumbView = new ImageView(img);
        thumbView.setFitWidth(60); // Kích thước ảnh nhỏ
        thumbView.setFitHeight(60);

        // Bo góc cho ảnh nhỏ nhìn pro hơn
        Rectangle clip = new Rectangle(60, 60);
        clip.setArcWidth(10);
        clip.setArcHeight(10);
        thumbView.setClip(clip);

        // Hiệu ứng chuột và sự kiện click đổi ảnh chính
        StackPane thumbWrapper = new StackPane(thumbView); // Bỏ ảnh vào khay
        thumbWrapper.getStyleClass().add("thumbnail-item"); // Gắn class CSS

        thumbWrapper.setOnMouseClicked(event -> {
            setMainImage(img);
        });

        // Ném cái khay vào HBox thay vì ném ảnh trần
        thumbnailBox.getChildren().add(thumbWrapper);
    }
    @FXML
    public void backToMarketplace(javafx.event.ActionEvent event) {
        //Lấy cái cửa sổ (Stage) hiện tại đang chứa cái nút vừa bị bấm
        javafx.stage.Stage stage = (javafx.stage.Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();

        // Đóng cửa sổ
        stage.close();
    }
}