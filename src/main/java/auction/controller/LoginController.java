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
import javafx.scene.input.MouseEvent;
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
        Task<User> loginTask = new Task<User>() {
            @Override
            protected User call() throws Exception {
                // 2. Gửi yêu cầu và nhận Object chung
                Object response = AuctionClient.getInstance().sendRequest(new LoginRequest(logName, logPassword));

                // 3. Kiểm tra kiểu dữ liệu trước khi ép kiểu để tránh lỗi Incompatible types
                if (response instanceof User) {
                    return (User) response;
                } else {
                    throw new Exception("Server không trả về đối tượng Item hợp lệ!");
                }
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
                    mainScreen.setLoggedInUser(loggedInUser);
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
    @FXML
    void handleGoToSignUp(MouseEvent event){
        SceneSwitcher.switchScene(event, "/fxml/signup.fxml", "SignUp");
    }
}
