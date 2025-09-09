package com.github.rob269.helloMessengerClient.gui;

import com.github.rob269.helloMessengerClient.Client;
import com.github.rob269.helloMessengerClient.Main;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;

import java.io.IOException;
import java.util.Objects;

public class LoginSceneController {

    @FXML
    private PasswordField passwordTextField;

    @FXML
    private TextField usernameTextField;

    @FXML
    private Text errorMessage;

    @FXML
    void onLogInButton(ActionEvent event) throws IOException {
        Client.login(usernameTextField.getText(), passwordTextField.getText());
        String message = Main.serverConnect();
        if (message.equals("OK")) {
            FXMLLoader loader = new FXMLLoader(MainSceneController.class.getResource("main.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(Objects.requireNonNull(LoginSceneController.class.getResource("style.css")).toExternalForm());
            Main.stage.setScene(scene);
            Main.initChats();
        }
        else {
            errorMessage.setText(message);
            errorMessage.setVisible(true);
        }
    }
}
