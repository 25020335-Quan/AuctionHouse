package auction.controller;

import auction.model.item.Item;
import auction.util.ImageHelper;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;

public class EditItemController {
    @FXML
    private TextField nameField;
    @FXML
    private TextField priceField;
    @FXML
    private ComboBox<String> typeField;
    @FXML
    private TextArea descriptionField;
    @FXML
    private VBox dropZoneBox;
    @FXML
    private FlowPane imageContainer;
    @FXML
    private Label dropHintLabel;


    private Item itemToEdit;
    private MainScreenController mainController;

    public void setEditData(Item item, MainScreenController mainController) {
        this.itemToEdit = item;
        this.mainController = mainController;
        // Điền sẵn dữ liệu cũ lên form
        nameField.setText(item.getName());
        priceField.setText(String.valueOf(item.getCurrentPrice()));
        descriptionField.setText(item.getDescription());
        typeField.setValue(item.getItemType());

        // Gọi hàm load ảnh
        loadExistingImages();
        setupDragAndDrop();

    }

    @FXML
    public void handleSaveItem(ActionEvent event) {
        try {
            itemToEdit.setName(nameField.getText());
            itemToEdit.setPrice(Double.parseDouble(priceField.getText()));
            itemToEdit.setDescription(descriptionField.getText());
            if (mainController != null) {
                mainController.refreshLocalUI();
            }
            ((Stage) nameField.getScene().getWindow()).close();
        } catch (NumberFormatException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Price must be a number!");
            alert.showAndWait();
        }
    }

    // Hàm vẽ ảnh vào khung và xử lý xóa ảnh
    private void loadExistingImages() {
        imageContainer.getChildren().clear();
        List<File> imageFiles = ImageHelper.getImagesOfItem(itemToEdit.getId());
        // Ẩn/Hiện dòng chữ
        if (dropHintLabel != null) {
            dropHintLabel.setVisible(imageFiles.isEmpty());
            dropHintLabel.setManaged(imageFiles.isEmpty());
        }
        for (File file : imageFiles) {
            StackPane imageWrapper = new StackPane();

            // --- TẠO ẢNH NHỎ ---
            Image img = ImageHelper.loadImageSafe(file);
            ImageView thumbView = new ImageView(img);
            thumbView.setFitWidth(80);  // Kích thước chuẩn
            thumbView.setFitHeight(80);

            // Bo tròn góc ảnh cho mượt (Tùy chọn)
            Rectangle clip = new Rectangle(80, 80);
            clip.setArcWidth(10);
            clip.setArcHeight(10);
            thumbView.setClip(clip);

            // ---  NÚT XÓA [X]---
            Button btnDelete = new Button("X");
            btnDelete.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 0 4; -fx-cursor: hand;");

            // Ép nút xóa lên góc trên bên phải của tấm ảnh
            StackPane.setAlignment(btnDelete, Pos.TOP_RIGHT);
            //Hàm xử lý xóa ảnh và dồn ảnh lại
            btnDelete.setOnAction(e -> {
                // Xóa file vật lý trong ổ cứng
                if (file.delete()) {
                    System.out.println("Đã xóa ảnh: " + file.getName());

                    // Gọi ImageHelper để re-index các file ảnh
                    ImageHelper.reindexImages(itemToEdit.getId());

                    // Vẽ lại toàn bộ dải ảnh nhỏ để thấy ảnh bị dồn lại
                    loadExistingImages();
                } else {
                    System.out.println("Lỗi: Không thể xóa file.");
                }
            });
            imageWrapper.getChildren().addAll(thumbView, btnDelete);
            imageContainer.getChildren().add(imageWrapper);
        }
    }

    // Hàm cài đặt Drag and Drop cho màn hình
    private void setupDragAndDrop() {
        //Khi người dùng lướt qua vùng nét đứt
        dropZoneBox.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
                if (!dropZoneBox.getStyleClass().contains("drag-over")) {
                    dropZoneBox.getStyleClass().add("drag-over");
                }
            }
            event.consume();
        });
        //Khi người dùng lướt ra ngoaài vùng nét đứt
        dropZoneBox.setOnDragExited(event -> {
            // Xóa hiệu ứng đổi màu viền
            dropZoneBox.getStyleClass().remove("drag-over");
            event.consume();
        });
        dropZoneBox.setOnDragDropped(event -> {
            //Khi người dùng thả ảnh vào vùng nét đứt
            Dragboard db = event.getDragboard();
            boolean isSuccess = false;
            if (db.hasFiles()) {
                // Quét từng file được ném vào
                for (File file : db.getFiles()) {
                    String fileName = file.getName().toLowerCase();
                    // Chỉ chấp nhận file ảnh
                    if (fileName.endsWith(".png") || fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
                        boolean isAddSuccess = ImageHelper.addImage(itemToEdit.getId(), file);

                        if (isAddSuccess) {
                            isSuccess = true;
                        } else {
                            // Nếu báo fail (do quá 5 ảnh) -> Hiện cảnh báo và ngắt luôn
                            Alert alert = new Alert(Alert.AlertType.WARNING, "You are only allowed to upload 5 pictures");
                            alert.showAndWait();
                            break;
                        }
                    }
                }
            }
            dropZoneBox.getStyleClass().remove("drag-over");

            // Nếu có ít nhất 1 ảnh nạp thành công -> Vẽ lại dải ảnh
            if (isSuccess) {
                loadExistingImages();
            }

            event.setDropCompleted(isSuccess);
            event.consume();
        });
    }

}