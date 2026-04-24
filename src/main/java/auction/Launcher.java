package auction;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Launcher extends Application {
    private Stage primaryStage;

    public static void main(String[] args) {
        Application.launch(args);
    }

    @Override

    public void start(Stage stage) {
        try{
            //Load file FMXL
        Parent root = FXMLLoader.load(Launcher.class.getResource("/fxml/Login.fxml"));
        primaryStage = stage;
        stage.setResizable(false);
        stage.setTitle("UET Auction House");
        //Đưa root vào scene và đưa scene vào root
        stage.setScene(new Scene(root));
        //Hiển thị stage
        stage.show();
    }
        catch(Exception e){
            e.printStackTrace();
        }
}
}
