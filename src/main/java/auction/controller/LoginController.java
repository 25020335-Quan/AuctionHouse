package auction.controller;
import auction.client.AuctionClient;
import auction.model.users.User;
import auction.util.LoginRequest;
import auction.util.SceneSwitcher;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import java.io.IOException;
//Class điều khiển quá trình đăng nhập
public class LoginController{
    @FXML
    private Label wrongLogin;
    @FXML
    private TextField username;
    @FXML
    private PasswordField password;
    @FXML
    private Button loginButton;
    @FXML
    void userLogin(ActionEvent event) throws IOException {
        String logName = username.getText();
        String logPassword = password.getText();
        //Khi người dùng nhập đúng tên đăng nhập và mật khẩu thì chuyển qua scene tiếp theo
        Task<Object> loginTask = new Task<>() {
            @Override
            protected User call() throws Exception {
                // Gửi yêu cầu đăng nhập tới Server
                return (User) AuctionClient.getInstance().sendRequest(new LoginRequest(logName, logPassword));
            }
        };

        loginTask.setOnSucceeded(e -> {
            User loggedInUser = (User) loginTask.getValue();
            if (loggedInUser != null) {
                wrongLogin.setText("Success");
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/mainscreen.fxml"));
                try {
                    Parent root = loader.load();
                    MainScreenController mainScreen =  loader.getController();
                    mainScreen.displayName(logName);

                    Scene scene = new Scene(root);
                    Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
                    stage.setScene(scene);
                    stage.show();
                } catch (IOException exc) {
                    exc.printStackTrace();
                }
            } else {
                wrongLogin.setText("Wrong username or password");
                wrongLogin.setStyle("-fx-text-fill: red;");
            }
        });

        Thread thread = new Thread(loginTask);
        thread.start();

    }
}
