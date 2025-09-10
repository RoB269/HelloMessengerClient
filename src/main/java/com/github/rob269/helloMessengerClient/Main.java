package com.github.rob269.helloMessengerClient;

import com.github.rob269.helloMessengerClient.gui.LoginSceneController;
import com.github.rob269.helloMessengerClient.gui.MainSceneController;
import com.github.rob269.helloMessengerClient.io.ResourcesIO;
import com.github.rob269.helloMessengerClient.io.ServerIO;
import com.github.rob269.helloMessengerClient.rsa.Guarantor;
import com.github.rob269.helloMessengerClient.rsa.Key;
import com.github.rob269.helloMessengerClient.io.WrongKeyException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class Main extends Application {
    static {
        File logsDir = new File("log/");
        if (!logsDir.exists()) {
            logsDir.mkdir();
        }
        try {
            LogManager.getLogManager().readConfiguration(Objects.requireNonNull(Main.class.getResource("log.properties")).openStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    public static Stage stage;
    private static String serverIp = "127.0.0.1";
    public static Messenger messenger = null;
    public static MainSceneController controller;
    public static long selectedChatId = -1;

    public static void main(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-login")) {
                if (i+2<args.length) {
                    Client.login(args[i + 1], args[i + 2]);
                    i += 2;
                }
                else {
                    LOGGER.warning("Login arguments exception");
                }
            }
            else if (args[i].equals("-ip")) {
                if (i+1 < args.length) {
                    serverIp = args[++i];
                }
                else {
                    LOGGER.warning("Ip argument exception");
                }
            }
        }
        parseConfigFile();
        launch();
    }

    @Override
    public void start(Stage stage) throws Exception {
        stage.setTitle("Hello Messenger");
        stage.getIcons().add(new Image(Objects.requireNonNull(Main.class.getResource("icon.png")).openStream()));
        stage.setMinHeight(400);
        stage.setMinWidth(600);
        FXMLLoader fxmlLoader;
        Parent root;
        if (!Client.isLogin()) {
            fxmlLoader = new FXMLLoader(LoginSceneController.class.getResource("login.fxml"));
            root = fxmlLoader.load();
        }
        else {
            fxmlLoader = new FXMLLoader(MainSceneController.class.getResource("main.fxml"));
            root = fxmlLoader.load();
            controller = fxmlLoader.getController();
            String message = "Error";
            try {
                message = serverConnect();
            } catch (Exception _) {
            }
            if (!message.equals("OK")) {
                controller.printErrorMessage(message);
            }
            else initChats();
        }
        stage.setOnCloseRequest((WindowEvent event) -> {
            if (messenger != null) {
                try {
                    messenger.close();
                } catch (IOException _) {
                }
            }
        });

        Scene scene = new Scene(root);
        scene.getStylesheets().add(Objects.requireNonNull(MainSceneController.class.getResource("style.css")).toExternalForm());
        stage.setScene(scene);
        Main.stage = stage;
        stage.show();
    }

    public static void initChats() {
        Map<Long, Chat> map = Main.messenger.getChats();
        List<Long> ids = Main.messenger.getChatIds();
        controller.clearChats();
        for (Long id : ids) controller.addChat(map.get(id));
    }

    private static void parseConfigFile() {
        try {
            if (!ResourcesIO.isExist("config")) {
                ResourcesIO.write("config", new ArrayList<>());
                throw new RuntimeException();
            }
            BigInteger[] publicKey = null;
            StringBuilder builder = new StringBuilder();
            for (String line : ResourcesIO.read("config")) builder.append(line);
            String[] configs = builder.toString().replaceAll(" ", "").split(";");
            for (String config : configs) {
                if (config.startsWith("guarantor_public_key")) {
                    String[] key = config.split("=")[1].split(",");
                    publicKey = new BigInteger[]{new BigInteger(key[0]), new BigInteger(key[1])};
                } else if (config.startsWith("server_ip")) {
                    serverIp = config.split("=")[1];
                }
            }
            if (publicKey != null) {
                Guarantor.init(new Key(publicKey));
            } else {
                throw new RuntimeException();
            }
        } catch (RuntimeException e) {
            LOGGER.severe("The configuration file does not contain the necessary data");
            throw e;
        }
    }

    private static int connectTryCount = 0;
    public static String serverConnect() {
        String message = "";
        do {
            try {
                connectTryCount++;
                Client.initKeys();
                ServerIO serverIO = null;
                try {
                    Thread.currentThread().setName("MainConnectionThread");
                    Socket serverSocket = new Socket(serverIp, 5099);
                    serverIO = new ServerIO(serverSocket);
                    serverSocket.setSoTimeout(3_000);
                    serverIO.init();
                    if (!serverIO.isClosed() && serverIO.isInitialized()) {
                        Messenger messenger = new Messenger(serverIO);
                        SideConnectionThread thread = new SideConnectionThread(messenger);
                        thread.setName("SideConnectionThread");
                        thread.start();
                        Main.messenger = messenger;
                        messenger.requestChatsFromServer();
                        message = "OK";
                    }
                } catch (IOException e) {
                    LOGGER.warning("Can't connect to server\n" + LogFormatter.formatStackTrace(e));
                    close(serverIO);
                    message = "Can't connect to server";
                    throw e;
                } catch (WrongKeyException e) {
                    LOGGER.warning(LogFormatter.formatStackTrace(e));
                    close(serverIO);
                    message = "Server error";
                } catch (InitializationException e) {
                    LOGGER.warning("Fail initialization");
                    close(serverIO);
                    message = "Protocol error";
                } catch (AuthenticationException e) {
                    LOGGER.warning("Authentication error");
                    close(serverIO);
                    message = "Authentication error";
                }
                break;
            } catch (Exception _) {
            }
        } while (connectTryCount < 2);
        return message;
    }

    private static void close(ServerIO serverIO) {
        if (serverIO != null && !serverIO.isClosed()) serverIO.close();
    }
}