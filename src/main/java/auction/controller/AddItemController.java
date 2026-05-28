package auction.controller;

import auction.client.AuctionClient;
import auction.model.AuctionManager;
import auction.model.factory.FactoryProvider;
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

    // Chọn thời gian bắt đầu
    @FXML
    private DatePicker startDatePicker;
    @FXML
    private ComboBox<String> startHourCombo;
    @FXML
    private ComboBox<String> startMinuteCombo;

    // Chọn thời gian kết thúc
    @FXML
    private DatePicker endDatePicker;
    @FXML
    private ComboBox<String> endHourCombo;
    @FXML
    private ComboBox<String> endMinuteCombo;

    @FXML
    public void initialize() {
        // Khởi tạo dữ liệu cho ComboBox
        typeField.getItems().addAll("Art", "Electronics", "Vehicle");

        // Kích hoạt tính năng Kéo Thả Ảnh
        setupDragAndDrop();

        //  Nạp số giờ và số phu cho cả hai comboBox
        for (int i = 0; i < 24; i++) {
            String hourStr = String.format("%02d", i);
            startHourCombo.getItems().add(hourStr);
            endHourCombo.getItems().add(hourStr);
        }
        for (int i = 0; i < 60; i++) {
            String minuteStr = String.format("%02d", i);
            startMinuteCombo.getItems().add(minuteStr);
            endMinuteCombo.getItems().add(minuteStr);
        }

        // Đặt giá trị gợi ý mặc định hiển thị sẵn cho người dùng
        LocalDateTime now = LocalDateTime.now();
        startDatePicker.setValue(now.toLocalDate());
        startHourCombo.setValue(String.format("%02d", now.getHour()));
        startMinuteCombo.setValue(String.format("%02d", now.getMinute()));

        endDatePicker.setValue(now.toLocalDate().plusDays(3)); // Kết thúc mặc định sau 3 ngày
        endHourCombo.setValue("23");
        endMinuteCombo.setValue("59");
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


    public void setParentController(MainScreenController parent) {
        this.parentController = parent;
    }

    @FXML
    public void handleSave(ActionEvent event) {
        // Lấy thông tin từ các ô nhập liệu
        String name = nameField.getText();
        double price = Double.parseDouble(priceField.getText());

        // Tạo sản phẩm mới gắn với ID của người đang đăng nhập
        String ownerId = parentController.getCurrentUser().getId();
        String typeInput = typeField.getValue().trim().toLowerCase();

        newItem = FactoryProvider.createNewItemByType(typeInput, ownerId, name, price);

        String userDesc = descriptionField.getText();


        // Đọc dữ liệu từ bộ chọn Start Time
        java.time.LocalDate sDate = startDatePicker.getValue();
        String sHour = startHourCombo.getValue();
        String sMinute = startMinuteCombo.getValue();

        // Đọc dữ liệu từ bộ chọn End Time
        java.time.LocalDate eDate = endDatePicker.getValue();
        String eHour = endHourCombo.getValue();
        String eMinute = endMinuteCombo.getValue();

        // Kiểm tra xem người dùng có bỏ trống ô nào không
        if (sDate == null || sHour == null || sMinute == null ||
                eDate == null || eHour == null || eMinute == null) {
            showAlert("Please select both Start Time and End Time fully!");
            return;
        }

        // Ráp LocalDate và LocalTime thành LocalDateTime hoàn chỉnh
        LocalDateTime startTime = LocalDateTime.of(sDate, java.time.LocalTime.of(Integer.parseInt(sHour), Integer.parseInt(sMinute)));
        LocalDateTime endTime = LocalDateTime.of(eDate, java.time.LocalTime.of(Integer.parseInt(eHour), Integer.parseInt(eMinute)));

        LocalDateTime currentTime = LocalDateTime.now();

        if (startTime.isBefore(currentTime.minusMinutes(1))) {
            showAlert("Start time cannot be in the past!");
            return;
        }
        if (endTime.isBefore(startTime) || endTime.isEqual(startTime)) {
            showAlert("End time must be strictly after the start time!");
            return;
        }
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
        desc.append(userDesc != null && !userDesc.trim().isEmpty() ? userDesc : "No description").append("\n");
        desc.append("---------------------------------------- \n");
        newItem.setDescription(desc.toString());
        Task<Item> addItemTask = new Task<Item>() {
            @Override
            protected Item call() throws Exception {
                // Up toàn bộ ảnh lên Cloud trước khi gửi gói tin
                for (File imgFile : selectedFiles) {
                    String cloudLink = auction.util.ImageUploadUtil.uploadImage(imgFile);
                    if (cloudLink != null) {
                        newItem.addImageUrl(cloudLink);
                    }
                }

                while (true) {
                    Object response = AuctionClient.getInstance().sendRequest(new AddItemRequest(newItem));

                    if (response instanceof Item) {
                        return (Item) response; // Chỉ thoát vòng lặp khi nhận đúng Item
                    } else if (response instanceof NotificationRequest note) {
                        // Tiện thể xử lý luôn thông báo nếu nhận nhầm
                        Platform.runLater(() -> showAlert(note.getMsg()));
                        // Sau đó tiếp tục vòng lặp để đợi gói tin tiếp theo (Hy vọng là Item)
                    }
                    else {
                        // Nếu Server trả về String báo lỗi hoặc một Object lạ
                        System.err.println("Lỗi Server trả về: " + response);
                        throw new Exception("Thêm sản phẩm thất bại: " + response);
                        // throw Exception sẽ lập tức phá vỡ while(true) và đẩy Task vào trạng thái Failed
                    }
                }
            }
        };

        addItemTask.setOnSucceeded(e -> {
            showAlert("Added item successfully!");
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
