package auction;

import auction.controller.MainScreenController;
import auction.model.users.Member;
import auction.model.users.User;
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
            //Tạm thời bỏ qua phần login
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();
//            MainScreenController mainScreenController = loader.getController() ;
//            Member testUser = new Member("U99" , "test" , "123456");
//            mainScreenController.setLoggedInUser(testUser);
//            mainScreenController.displayName(testUser.getUsername());
            primaryStage = stage;
            stage.setResizable(true);
            stage.setFullScreen(false);
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