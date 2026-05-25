package auction.controller;

import auction.client.AuctionClient;
import auction.model.AuctionManager;
import auction.model.item.Art;
import auction.model.item.Electronics;
import auction.model.item.Item;
import auction.model.item.Vehicle;
import auction.model.users.User;
import auction.util.AddItemRequest;
import auction.util.LoginRequest;
import auction.util.NotificationRequest;
import javafx.application.Platform;
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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class AddItemController {
    @FXML
    TextField nameField;
    @FXML
    TextField priceField;
    @FXML
    ComboBox<String> typeField;
    @FXML
    TextArea descriptionField;
    @FXML
    Button saveItem;
    @FXML
    Button selectMoreFiles;
    @FXML
    VBox dragDropBox;
    @FXML
    private MainScreenController parentController;
    @FXML
    private FlowPane imagePreviewPane;
    private Item newItem;
    // Danh sách lưu trữ các file ảnh người dùng đã thả vào
    private List<File> selectedFiles = new ArrayList<>();

    @FXML
    public void initialize() {
        // Khởi tạo dữ liệu cho ComboBox
        typeField.getItems().addAll("Art", "Electronics", "Vehicle");

        // Kích hoạt tính năng Kéo Thả Ảnh
        setupDragAndDrop();
    }

    //Xử lý ảnh sản phẩm
    private void setupDragAndDrop() {
        //Khi cầm file lướt qua khung
        dragDropBox.setOnDragOver(event -> {
            if (event.getGestureSource() != dragDropBox && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            event.consume();
        });
        //Khi nhả chuột thả file xuống box
        dragDropBox.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;

            if (db.hasFiles()) {
                for (File file : db.getFiles()) {
                    String name = file.getName().toLowerCase();
                    // Chỉ cho phép đuôi ảnh
                    if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg")) {
                        if (selectedFiles.size() < 5) {
                            selectedFiles.add(file);
                        } else {
                            showAlert("You are only allowed to upload 5 pictures");
                            break;
                        }
                    }
                }
                success = true;
                updateImagePreviews(); // Cập nhật lại giao diện
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    public void updateImagePreviews() {
        imagePreviewPane.getChildren().clear(); // Xóa UI cũ để cập nhật lại
        for (File file : selectedFiles) {
            // Tạo ảnh
            Image img = new Image(file.toURI().toString());
            ImageView imageView = new ImageView(img);
            imageView.setFitWidth(77);
            imageView.setFitHeight(78);

            // Bo góc ảnh
            Rectangle clip = new Rectangle(77, 78);
            clip.setArcWidth(15);
            clip.setArcHeight(15);
            imageView.setClip(clip);

            // Tạo nút Xóa đỏ
            Button btnRemove = new Button("X");
            btnRemove.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 15; -fx-cursor: hand;");
            btnRemove.setOnAction(e -> {
                selectedFiles.remove(file); // Xóa khỏi danh sách
                updateImagePreviews(); // Vẽ lại UI
            });

            // Xếp chồng ảnh và nút X
            StackPane container = new StackPane();
            container.getChildren().addAll(imageView, btnRemove);
            StackPane.setAlignment(btnRemove, Pos.TOP_RIGHT); // Ép nút X lên góc phải

            imagePreviewPane.getChildren().add(container);
        }
    }

    // Copy ảnh người dùng đã chọn vào thư mục dự án
    private void saveImagesToLocalProject(String itemId) {
        if (selectedFiles.isEmpty()) {
            return;
        }
        ;
        File dir = new File("src/main/resources/images");
        if (!dir.exists()) dir.mkdirs();
        for (int i = 0; i < selectedFiles.size(); i++) {
            File sourceFile = selectedFiles.get(i);
            // Đổi tên ảnh theo ID sản phẩm (VD: I-ABC123_0.jpg, I-ABC123_1.jpg)
            String extension = sourceFile.getName().substring(sourceFile.getName().lastIndexOf("."));
            String newFileName = itemId + "_" + i + extension;

            Path sourcePath = Paths.get(sourceFile.getAbsolutePath());
            Path targetPath = Paths.get(dir.getAbsolutePath() + "/" + newFileName);

            try {
                // Copy file từ máy tính vào project
                Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Đã lưu ảnh: " + newFileName);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void setParentController(MainScreenController parent) {
        this.parentController = parent;
    }

    @FXML
    public void handleSave(ActionEvent event) {
        // Lấy thông tin từ các ô nhập liệu
        String name = nameField.getText();
        double price = Double.parseDouble(priceField.getText());

        // Tạo sản phẩm mới gắn với ID của người đang đăng nhập
        String itemId = "I" + System.currentTimeMillis();
        String ownerId = parentController.getCurrentUser().getId();
        String typeInput = typeField.getValue().trim().toLowerCase();
        if (typeInput.equals("art")) {
            newItem = new Art(itemId, ownerId, name, price);
        } else if (typeInput.equals("electronics")) {
            newItem = new Electronics(itemId, ownerId, name, price);
        } else if (typeInput.equals("vehicle")) {
            newItem = new Vehicle(itemId, ownerId, name, price);
        } else {
            // Mặc định hoặc báo lỗi nếu nhập sai loại
            System.out.println("Loại sản phẩm không hợp lệ");
        }
        String userDesc = descriptionField.getText();
        newItem.setStartingPrice(price);
        saveImagesToLocalProject(itemId);
        // Lấy đúng khoảnh khắc người dùng bấm nút "Save" làm giờ bắt đầu
        LocalDateTime startTime = LocalDateTime.now();

        // Tự động cộng thêm 3 ngày làm giờ kết thúc
        LocalDateTime endTime = startTime.plusDays(3);

        // Đưa vào sản phẩm
        newItem.setStartTime(startTime);
        newItem.setEndTime(endTime);

        //Định dạng lại ngày giờ để hiển thị lên màn hình
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        StringBuilder desc = new StringBuilder();
        desc.append("----------------------------------------\n");
        desc.append("AUCTION DETAILS\n");
        desc.append("----------------------------------------\n");
        desc.append("▪ Product Name: ").append(name).append("\n");
        desc.append(String.format("▪ Starting Price: %,.0f VND\n", price));
        desc.append("▪ Start Time: ").append(startTime.format(formatter)).append("\n");
        desc.append("▪ End Time: ").append(endTime.format(formatter)).append("\n");
        desc.append("▪ Description:\n ");
        desc.append(userDesc);
        desc.append("---------------------------------------- \n");
        newItem.setDescription(desc.toString());

        Task<Item> addItemTask = new Task<Item>() {
            @Override
            protected Item call() throws Exception {
                while (true) {
                    Object response = AuctionClient.getInstance().sendRequest(new AddItemRequest(newItem));

                    if (response instanceof Item) {
                        return (Item) response; // Chỉ thoát vòng lặp khi nhận đúng Item
                    } else if (response instanceof NotificationRequest note) {
                        // Tiện thể xử lý luôn thông báo nếu nhận nhầm
                        Platform.runLater(() -> showAlert(note.getMsg()));
                        // Sau đó tiếp tục vòng lặp để đợi gói tin tiếp theo (Hy vọng là Item)
                    }
                }
            }
        };

        addItemTask.setOnSucceeded(e -> {
            parentController.addNewItemLocally(newItem);
            ((Stage)nameField.getScene().getWindow()).close();
            System.out.println("Đã thêm sản phẩm thành công!");
        });
        Thread thread = new Thread(addItemTask);
        thread.start();
    }
        private void showAlert (String message){
            Alert alert = new Alert(Alert.AlertType.INFORMATION); // Loại thông báo có icon chữ 'i'
            alert.setTitle("Thông báo hệ thống");
            alert.setHeaderText(null); // Không dùng tiêu đề phụ
            alert.setContentText(message);
            alert.showAndWait(); // Hiển thị và bắt người dùng nhấn OK mới được làm tiếp
        }

        
    }
