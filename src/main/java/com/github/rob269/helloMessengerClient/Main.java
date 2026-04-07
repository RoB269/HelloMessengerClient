package com.github.rob269.helloMessengerClient;

import com.github.rob269.helloMessengerClient.gui.LoginSceneController;
import com.github.rob269.helloMessengerClient.gui.MainSceneController;
import com.github.rob269.helloMessengerClient.gui.ServerIpInputSceneController;
import com.github.rob269.helloMessengerClient.io.ResourcesIO;
import com.github.rob269.helloMessengerClient.io.HMPServerIO;
import com.github.rob269.helloMessengerClient.io.ServerIO;
import com.github.rob269.helloMessengerClient.io.WrongKeyException;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.*;

public class Main extends Application {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    public static Stage stage;
    private static String serverIp = "";
    private static int port = -1;
    public volatile static Messenger messenger = null;
    public static MainSceneController controller;
    public static long selectedChatId = -1;
    public static Config configParser = new HMPConfig();
    public static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void setServerIp(String ip) {
        if (serverIp.isEmpty()) {
            if (ip.contains(":")) {
                serverIp = ip.split(":")[0];
                port = Integer.parseInt(ip.split(":")[1]);
            }
            else serverIp = ip;
        }
    }

    public static void setPort(int val) {
        if (port == -1) port = val;
    }

    public static void main(String[] args) {
        String resourcesPath = "";
        String configPath = "";
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-login" -> {
                    if (i + 2 < args.length) {
                        Client.login(args[i + 1], args[i + 2]);
                        i += 2;
                    } else {
                        LOGGER.warning("Login arguments exception");
                    }
                }
                case "-ip" -> {
                    if (i + 1 < args.length) {
                        setServerIp(args[++i]);
                    } else {
                        LOGGER.warning("Ip argument exception");
                    }
                }
                case "-port" -> {
                    if (i + 1 < args.length) {
                        setPort(Integer.parseInt(args[++i]));
                    } else {
                        LOGGER.warning("Port argument exception");
                    }
                }
                case "-configs" -> {
                    if (i + 1 < args.length) {
                        configPath = args[++i];
                    } else {
                        LOGGER.warning("Configs path argument exception");
                    }
                }
                case "-resources" -> {
                    if (i + 1 < args.length) {
                        resourcesPath = args[++i];
                    } else {
                        LOGGER.warning("Resources path argument exception");
                    }
                }
            }
        }
        boolean isResourcesFolderOk = true;
        try {
            ResourcesIO.setResourcesPath(resourcesPath);
            File logsDir = new File(ResourcesIO.getResourcesPath() + "logs\\");
            if (!logsDir.exists()) {
                logsDir.mkdirs();
            }
        } catch (IOException e) {
            LOGGER.warning("Can't create resources folder");
            isResourcesFolderOk = false;
        }
        try {
            LogManager.getLogManager().readConfiguration(Objects.requireNonNull(Main.class.getResource("log.properties")).openStream());
            if (isResourcesFolderOk) {
                Logger rootLogger = Logger.getLogger("");
                FileHandler fileHandler = getFileHandler(resourcesPath.isEmpty() ? "" : resourcesPath + "logs/log%g-%u.txt");
                rootLogger.addHandler(fileHandler);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        configParser.parseConfigFile(configPath.isEmpty() ? ResourcesIO.getResourcesPath() + "config" : configPath);
        setPort(5099);
        launch();
    }

    private static FileHandler getFileHandler(String pattern) throws IOException {
        LogManager logManager = LogManager.getLogManager();
        String limit = logManager.getProperty("fileHandler.limit");
        String count = logManager.getProperty("fileHandler.count");
        String level = logManager.getProperty("fileHandler.level");
        if (pattern.isEmpty()) {
            pattern = logManager.getProperty("fileHandler.pattern");
        }
        FileHandler fileHandler = new FileHandler(pattern,
                limit == null ? 10485760 : Integer.parseInt(limit),
                count == null ? 5 : Integer.parseInt(count), true);
        fileHandler.setLevel(Level.parse(level));
        fileHandler.setFormatter(new LogFormatter());
        return fileHandler;
    }

    @Override
    public void start(Stage stage) throws Exception {
        Thread.currentThread().setName("MainConnectionThread");
        stage.setTitle("Hello Messenger");
        stage.getIcons().add(new Image(Objects.requireNonNull(Main.class.getResource("icon.png")).openStream()));
        stage.setMinHeight(400);
        stage.setMinWidth(600);
        FXMLLoader fxmlLoader;
        Parent root;
        if (serverIp.isEmpty()) {
            fxmlLoader = new FXMLLoader(ServerIpInputSceneController.class.getResource("ipInput.fxml"));
            root = fxmlLoader.load();
        }
        else if (!Client.isLogin()) {
            fxmlLoader = new FXMLLoader(LoginSceneController.class.getResource("login.fxml"));
            root = fxmlLoader.load();
        }
        else {
            fxmlLoader = new FXMLLoader(MainSceneController.class.getResource("main.fxml"));
            root = fxmlLoader.load();
            controller = fxmlLoader.getController();
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
        new Thread(() -> {
            if (Client.isLogin()) {
                String message = serverConnect();
                if (!message.equals("OK")) {
                    Platform.runLater(() -> controller.printErrorMessage(message));
                }
                else {
                    Platform.runLater(() -> {
                        controller.hideErrorMessage();
                        initChats();
                    });
                }
            }
        }).start();
    }

    public static void initChats() {
        Map<Long, Chat> map = Main.messenger.getChats();
        List<Long> ids = Main.messenger.getChatIds();
        controller.clearChats();
        for (Long id : ids) controller.addChat(map.get(id));
    }



    public synchronized static String serverConnect() {
        String message = "";
        int connectTryCount = 0;
        do {
            try {
                Thread.currentThread().setName("MainConnectionThread");
                if (connectTryCount == 0) LOGGER.warning("Attempt to connect to the server");
                else LOGGER.warning("Repeated attempt to connect to the server");
                connectTryCount++;
                HMPClient.initKeys();
                ServerIO serverIO = null;
                try {
                    Socket serverSocket = new Socket(serverIp, port);
                    serverIO = new HMPServerIO(serverSocket);
                    serverSocket.setSoTimeout(3_000);
                    serverIO.init();
                    if (!serverIO.isClosed()) {
                        Messenger messenger = new Messenger(serverIO);
                        SideConnectionThread thread = new SideConnectionThread(messenger);
                        thread.setName("SideConnectionThread");
                        thread.start();
                        Main.messenger = messenger;
                        messenger.requestChatsFromServer();
                        message = "OK";
                    }
                } catch (IOException e) {
                    LOGGER.warning("Can't connect to server");
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
            } catch (Exception e) {
                LOGGER.warning("Exception when connecting to the server\n" + LogFormatter.formatStackTrace(e));
            }
        } while (connectTryCount < 2);
        return message;
    }

    private static void close(ServerIO serverIO) {
        if (serverIO != null && !serverIO.isClosed()) serverIO.close();
    }
}