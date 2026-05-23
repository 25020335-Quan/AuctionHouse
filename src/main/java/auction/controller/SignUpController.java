package auction.controller;

import auction.util.SceneSwitcher;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import java.io.IOException;

public class SignUpController {
    @FXML private TextField fullNameField;
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML
    private void handleRegister(ActionEvent event) throws IOException {
        String fullName = fullNameField.getText().trim();
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        //Không được để trống các thông tin
        if (fullName.isEmpty() || username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showAlert("Error", "Please fill in all fields!");
            return;
        }

        // Kiểm tra định dạng Email (Sơ bộ phải có @ và dấu chấm)
        if (!email.contains("@") || !email.contains(".")) {
            showAlert("Error", "Invalid email address!");
            return;
        }

        // Kiểm tra Mật khẩu có khớp nhau không
        if (!password.equals(confirmPassword)) {
            showAlert("Lỗi", "Passwords do not match!");
            return;
        }
        showAlert("Success", "Account registered successfully! Please log in.");
    }
    @FXML
    private void handleGoToLogin(MouseEvent event) {
        SceneSwitcher.switchScene(event, "/fxml/login.fxml", "Login");
    }
    /**
     * Hàm phụ trợ: Hiển thị hộp thoại thông báo (Popup)
     */
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        if (title.equals("Error")) {
            alert.setAlertType(Alert.AlertType.ERROR);
        }
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
