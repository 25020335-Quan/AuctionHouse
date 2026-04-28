package auction.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class MainScreenController {
    @FXML
    Label welcomeText;
    @FXML
    public void initialize() {
    }
    public void displayName(String name) {
        welcomeText.setText("Hello, " + name);
    }
}