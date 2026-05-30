package auction.controller;

import auction.model.users.Admin;
import auction.model.users.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;

public class ProfileController {
    @FXML
    private Label fullNameTitleLabel;
    @FXML
    private TextField idField;
    @FXML
    private TextField nameField;
    @FXML
    private TextField usernameField;
    @FXML
    private TextField roleField;
    @FXML
    private TextField emailField;
    private User currentUser;

    @FXML
    private Label balanceLabel;

    public void setUserData(User user) {
        this.currentUser = user;
        fullNameTitleLabel.setText(user.getFullName());
        idField.setText(user.getId());
        nameField.setText(user.getFullName());
        usernameField.setText(user.getUsername());
        emailField.setText(user.getEmail() != null ? user.getEmail() : "No email");

        if (user instanceof Admin) {
            roleField.setText("Admin");
        } else {
            roleField.setText("Member");
        }
        balanceLabel.setText(String.format("%,.0f VNĐ", user.getBalance()));
    }

    @FXML
    public void handleTopUp(ActionEvent event) {
        if (currentUser == null) return;

        TextInputDialog dialog = new TextInputDialog("1000000");
        dialog.setTitle("Deposit to Account");
        dialog.setHeaderText("Auction System Deposit Gateway");
        dialog.setContentText("Enter deposit amount (VND):");

        dialog.showAndWait().ifPresent(input -> {
            try {
                double amount = Double.parseDouble(input);
                if (amount <= 0) {
                    showAlert(Alert.AlertType.ERROR, "Error", "Deposit amount must be greater than 0!");
                    return;
                }

                // Gọi DatabaseService để cộng tiền dưới Database
                auction.model.service.DatabaseService dbService = new auction.model.service.DatabaseService();
                boolean success = dbService.addBalance(currentUser.getId(), amount);

                if (success) {
                    // Cộng tiền vào biến tạm
                    currentUser.setBalance(currentUser.getBalance() + amount);

                    // số tiền hiển thị trên màn hình ngay lập tức
                    balanceLabel.setText(String.format("%,.0f VND", currentUser.getBalance()));

                    showAlert(Alert.AlertType.INFORMATION, "Success", "Successfully deposited " + String.format("%,.0f VND", amount) + " into your wallet!");
                } else {
                    showAlert(Alert.AlertType.ERROR, "Failure", "Transaction error, unable to deposit funds!");
                }
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.ERROR, "Invalid Format", "Please enter numbers only!");
            }
        });
    }

    // Hàm hiện thông báo
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
