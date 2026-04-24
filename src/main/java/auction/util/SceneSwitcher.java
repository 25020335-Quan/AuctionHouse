package auction.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class SceneSwitcher {
    public static void switchScene(javafx.event.Event event, String fxmlFile, String title) {
        try {
            //Load file FXML được đưa vào phương thức
            Parent root = FXMLLoader.load(SceneSwitcher.class.getResource(fxmlFile));
            // Lấy stage tự sự kiện(Event) mà được người dùng kích hoạt
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setTitle(title);
            stage.setScene(new Scene(root));
            stage.show();
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
}