package com.github.rob269.helloMessengerClient.gui;

import com.github.rob269.helloMessengerClient.Main;
import com.github.rob269.helloMessengerClient.io.ResourcesIO;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ServerIpInputSceneController {

    @FXML
    private TextField textField;

    @FXML
    void onConfirmButton(ActionEvent event) throws IOException {
        String input = textField.getText();
        if (!input.isEmpty()) {
            Main.setServerIp(input);
            FXMLLoader fxmlLoader = new FXMLLoader(LoginSceneController.class.getResource("login.fxml"));
            Parent root = fxmlLoader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(Objects.requireNonNull(MainSceneController.class.getResource("style.css")).toExternalForm());
            Main.stage.setScene(scene);
            ResourcesIO.write(ResourcesIO.appdataPath + "config", List.of("\nserver_ip = " + input + ";"), true);
        }
    }
}
