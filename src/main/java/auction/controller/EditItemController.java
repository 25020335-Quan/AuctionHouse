package auction.controller;

import auction.model.item.Item;
import javafx.concurrent.Task;
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
import java.util.ArrayList;
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
        priceField.setText(String.format("%.0f", item.getStartingPrice()));
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

        // Lấy list Link ảnh của item
        List<String> cloudLinks = itemToEdit.getImageUrls();

        // Ẩn/Hiện dòng chữ hướng dẫn
        if (dropHintLabel != null) {
            boolean isEmpty = (cloudLinks == null || cloudLinks.isEmpty());
            dropHintLabel.setVisible(isEmpty);
            dropHintLabel.setManaged(isEmpty);
        }

        if (cloudLinks != null) {
        for (int i = 0; i < cloudLinks.size(); i++) {
            String link = cloudLinks.get(i);
            final int indexToDelete = i; // Lưu vị trí để xóa

            StackPane imageWrapper = new StackPane();

            // --- TẠO ẢNH NHỎ ---
            Image img = new Image(link, true); // Tải ngầm từ mạng
            ImageView thumbView = new ImageView(img);
            thumbView.setFitWidth(80);
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
            //Hàm xử lý xóa
            btnDelete.setOnAction(e -> {
                // Cắt Link khỏi RAM
                itemToEdit.getImageUrls().remove(indexToDelete);
                // Vẽ lại toàn bộ dải ảnh để cập nhật UI
                loadExistingImages();
            });

            imageWrapper.getChildren().addAll(thumbView, btnDelete);
            imageContainer.getChildren().add(imageWrapper);
        }
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
            Dragboard db = event.getDragboard();
            boolean isSuccess = false;
        if (db.hasFiles()) {
            List<File> filesToUpload = new ArrayList<>();
            int currentImageCount;
            if (itemToEdit.getImageUrls() != null) {
                // Nếu danh sách tồn tại, lấy kích thước thật
                currentImageCount = itemToEdit.getImageUrls().size();
            } else {
                // Nếu danh sách bị null, mặc định là 0 ảnh
                currentImageCount = 0;
            }

            // Quét từng file được ném vào
            for (File file : db.getFiles()) {
                String fileName = file.getName().toLowerCase();
                if (fileName.endsWith(".png") || fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
                    // Kiểm tra giới hạn 5 ảnh
                    if (currentImageCount + filesToUpload.size() < 5) {
                        filesToUpload.add(file);
                    } else {
                        Alert alert = new Alert(Alert.AlertType.WARNING, "You are only allowed to upload 5 pictures in total.");
                        alert.showAndWait();
                        break;
                    }
                }
            }

            // Nếu có ảnh hợp lệ -> Đưa vào luồng Task up Cloud
            if (!filesToUpload.isEmpty()) {
                if (dropHintLabel != null) {
                    dropHintLabel.setText("Đang tải ảnh lên Cloud, vui lòng chờ...");
                    dropHintLabel.setVisible(true);
                    dropHintLabel.setManaged(true);
                }

                Task<Void> uploadTask = new Task<Void>() {
                    @Override
                    protected Void call() throws Exception {
                        for (File file : filesToUpload) {
                            // đẩy lên API
                            String cloudLink = auction.util.ImageUploadUtil.uploadImage(file);
                            if (cloudLink != null) {
                                itemToEdit.addImageUrl(cloudLink); // Đút Link mới vào đối tượng Item
                            }
                        }
                        return null;
                    }
                };

                // Khi Task chạy xong, vẽ lại khung ảnh
                uploadTask.setOnSucceeded(e -> {
                    if (dropHintLabel != null) dropHintLabel.setText("Drop images here");
                    loadExistingImages();
                });

                new Thread(uploadTask).start();
                isSuccess = true;
            }
        }

        dropZoneBox.getStyleClass().remove("drag-over");
        event.setDropCompleted(isSuccess);
        event.consume();
    });
}
}