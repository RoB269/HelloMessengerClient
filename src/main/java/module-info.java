module com.github.rob269.helloMessengerClient {
    requires javafx.controls;
    requires javafx.fxml;
    requires jdk.compiler;
    requires java.logging;
    requires com.google.gson;


    exports com.github.rob269.helloMessengerClient;
    opens com.github.rob269.helloMessengerClient to javafx.fxml;
    opens com.github.rob269.helloMessengerClient.rsa to com.google.gson;
    exports com.github.rob269.helloMessengerClient.gui;
    opens com.github.rob269.helloMessengerClient.gui to javafx.fxml;
    exports com.github.rob269.helloMessengerClient.io;
    exports com.github.rob269.helloMessengerClient.rsa;
    opens com.github.rob269.helloMessengerClient.io to com.google.gson;
}