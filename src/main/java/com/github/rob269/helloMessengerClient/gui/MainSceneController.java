package com.github.rob269.helloMessengerClient.gui;

import com.github.rob269.helloMessengerClient.*;
import com.github.rob269.helloMessengerClient.util.Cursor;
import com.github.rob269.helloMessengerClient.util.LinkedList;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ResourceBundle;

public class MainSceneController implements Initializable {
    private static final double maxMessageWidth = 290;
    private static boolean needScrollDown = false;

    @FXML
    private VBox chats;

    @FXML
    private Button userButton;

    @FXML
    private StackPane errorMessagePane;

    @FXML
    private AnchorPane mainPane;

    @FXML
    private Rectangle rectangle;

    @FXML
    private Rectangle rectangle1;

    @FXML
    private Rectangle rectangle2;

    @FXML
    private Rectangle rectangle3;

    @FXML
    private Text contactName;

    @FXML
    private VBox messagesPane;

    @FXML
    private TextField enterTextField; //todo replace with TextArea

    @FXML
    private ScrollPane scrollPane;

    @FXML
    private Text errorMessageText;

    @FXML
    private Text menuText;

    @FXML
    private TextField menuTextField;

    @FXML
    private StackPane menuPane;

    @FXML
    private Text menuErrorMessage;

    private enum MenuType {
        ADD_CONTACT,
        ADD_CHAT,
        CREATE_NEW_CHAT
    }
    private MenuType type;
    @FXML
    void onCreateNewChatButton(ActionEvent event) {
        menuText.setText("Enter chat name");
        type = MenuType.CREATE_NEW_CHAT;
        showMenu();
    }

    @FXML
    void onAddChatButton(ActionEvent event) {
        menuText.setText("Enter chat id");
        type = MenuType.ADD_CHAT;
        showMenu();
    }

    @FXML
    void onAddContactButton(ActionEvent event) {
        menuText.setText("Enter contact name");
        type = MenuType.ADD_CONTACT;
        showMenu();
    }

    private void showMenu() {
        menuTextField.clear();
        menuPane.setVisible(true);
        menuErrorMessage.setVisible(false);
    }

    public void printMenuErrorMessage(String message) {
        menuErrorMessage.setText(message);
        menuErrorMessage.setVisible(true);
    }

    @FXML
    void onMenuEnterKey(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER && !menuTextField.getText().isEmpty()) menuHandle();
    }

    @FXML
    void onMenuConfirmButton() {
        menuHandle();
    }

    @FXML
    void onMenuCancelButton() {
        menuPane.setVisible(false);
    }

    private void menuHandle() {
        if (!menuTextField.getText().isEmpty()) {
            switch (type) {
                case MenuType.ADD_CONTACT -> {
                    Chat chat = Main.messenger.addContact(menuTextField.getText());
                    if (chat == null) return;
                    addChat(chat);
                }
                case MenuType.ADD_CHAT -> {
                    long chatId;
                    try {
                        chatId = Long.parseLong(menuTextField.getText());
                    } catch (NumberFormatException _) {
                        printMenuErrorMessage("Wrong enter");
                        return;
                    }
                    Chat chat = Main.messenger.connectToTheChat(chatId);
                    if (chat == null) return;
                    addChat(chat);
                }
                case MenuType.CREATE_NEW_CHAT -> {
                    Chat chat = Main.messenger.createChat(menuTextField.getText());
                    if (chat == null) return;
                    addChat(chat);
                }
            }
            menuTextField.clear();
            menuPane.setVisible(false);
        }
    }

    @FXML
    void onMessageEnterKey(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) sendMessage();
    }

    private volatile boolean isDone = true;
    @FXML
    void onReconnectButton(ActionEvent event) {
        synchronized (this) {
            if (isDone) {
                isDone = false;
                printErrorMessage("Trying to reconnect");
                new Thread(() -> {
                    String message = Main.serverConnect();
                    if (message.equals("OK")) {
                        Platform.runLater(() -> {
                            hideErrorMessage();
                            Main.initChats();
                        });
                        if (Main.selectedChatId != -1) Platform.runLater(() -> addMessagesToPane(Main.selectedChatId));
                    } else {
                        Platform.runLater(() -> printErrorMessage(message + "\nReconnection failed"));
                    }
                    isDone = true;
                }).start();
            }
        }
    }

    @FXML
    void onSendMessageButton(ActionEvent event) {
        sendMessage();
    }

    @FXML
    void onChatSettingsButton() {
        addMessage(new Message(42, "Hello", LocalDateTime.now(), "Test"), true);
    }

    private void sendMessage() {
        if (!enterTextField.getText().isEmpty() && Main.selectedChatId != -1) {//todo Сделать отправку сообщения ассинхронной
            Message message = Main.messenger.sendMessage(enterTextField.getText(), Main.selectedChatId);
            if (message != null) {
                needScrollDown = true;
                addMessage(message);
            }
            enterTextField.clear();
        }
    }

    void onChatButton(ActionEvent event) {
        ChatButton button = (ChatButton) event.getTarget();
        contactName.setText(button.getText() + (Main.messenger.getChats().get(button.getChatId()).isPrivate() ? "" : "[" + button.getChatId() + "]"));
        addMessagesToPane(button.getChatId());
        Main.selectedChatId = button.getChatId();
        needScrollDown = true;
        contactName.setVisible(true);
    }

    private void addMessagesToPane(long chatId) {
        LinkedList<Message> messages = Main.messenger.getChats().get(chatId).getMessages();
        if (messages.getFirst().getMessageId() != 0 && messages.size() < Messenger.MESSAGE_PACK_SIZE) {
            try {
                Main.messenger.loadMessages(chatId);
            } catch (IOException _) {
            }
        }

        messagesPane.getChildren().clear();
        Cursor<Message> cursor = messages.getLastCursor();
        while (cursor.hasPrevious()) {
            addMessage(cursor.previous(), true);
        }
    }

    public void addChat(Chat chat) {
        ChatButton chatButton = new ChatButton(chat.getChatId(), chat.getName());
        chatButton.getStyleClass().add("chat-button");
        chatButton.setOnAction(this::onChatButton);
        chats.getChildren().add(chatButton);
    }

    public void clearChats() {
        chats.getChildren().clear();
    }

    public void addReceivedMessage() {
        needScrollDown = scrollPane.getVvalue() == 1;
        addMessage(Main.messenger.getChats().get(Main.selectedChatId).getMessages().getLast());
    }

    public void addMessage(Message message) {
        addMessage(message, false);
    }

    public void addMessage(Message message, boolean addFirst) {
        if (message.getMessageId() != 0) {
            Text messageText = new Text(message.getMessage());
            messageText.setFont(new Font(20));
            StackPane.setMargin(messageText, new Insets(0, 0, 0, 7));

            double textWidth = messageText.getLayoutBounds().getWidth();
            if (textWidth > maxMessageWidth) textWidth = maxMessageWidth;
            messageText.setWrappingWidth(maxMessageWidth);
            Rectangle background = new Rectangle(textWidth + 16, messageText.getLayoutBounds().getHeight() + 16);
            background.getStyleClass().add("massage-background");
            if (message.getSender().equals(Client.getUsername())) background.setFill(Color.rgb(57, 213, 39));
            else background.setFill(Color.rgb(57, 213, 39, 0.3));

            StackPane stackPane = new StackPane(background, messageText);
            stackPane.setAlignment(Pos.CENTER_LEFT);
            VBox.setMargin(stackPane, new Insets(0, 0, 12, 7));

            Text additionalData = new Text(message.getSender() + " at " + message.getDate().format(Main.dateTimeFormatter));
            VBox.setMargin(additionalData, new Insets(0, 0, 0, 7));
            VBox vBox = new VBox(additionalData, stackPane);
            vBox.setPadding(new Insets(7, 0, 0, 0));


            if (addFirst) {//todo fix autoscroll
                messagesPane.getChildren().addFirst(vBox);
            } else messagesPane.getChildren().add(vBox);
        }
        else {
            Text text = new Text("Beginning of the chat");
            text.setFont(new Font(20));
            text.setFill(Color.rgb(97, 97, 97));

            Line line1 = new Line(0, 0, 200, 0);
            line1.setStroke(Color.rgb(205, 205, 205));
            line1.setStrokeWidth(2);
            Line line2 = new Line(0, 0, 200, 0);
            line2.setStroke(Color.rgb(205, 205, 205));
            line2.setStrokeWidth(2);
            BorderPane.setAlignment(line1, Pos.CENTER);
            BorderPane.setAlignment(line2, Pos.CENTER);
            BorderPane.setMargin(text, new Insets(0, 7, 0, 7));

            double textWidth = text.getLayoutBounds().getWidth()+14;
            BorderPane borderPane = new BorderPane();
            line1.endXProperty().bind(borderPane.widthProperty().subtract(textWidth).divide(2));
            line2.endXProperty().bind(borderPane.widthProperty().subtract(textWidth).divide(2));

            borderPane.setCenter(text);
            borderPane.setLeft(line1);
            borderPane.setRight(line2);
            borderPane.minWidthProperty().bind(mainPane.widthProperty().subtract(270));
            messagesPane.getChildren().addFirst(borderPane);
        }
    }

    public void printErrorMessage(String message) {
        errorMessageText.setText(message);
        errorMessagePane.setVisible(true);
    }

    public void hideErrorMessage() {
        errorMessagePane.setVisible(false);
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        userButton.setText(Client.getUsername());
        rectangle.widthProperty().bind(mainPane.widthProperty().subtract(270));
        rectangle1.widthProperty().bind(mainPane.widthProperty().subtract(270));
        rectangle2.heightProperty().bind(mainPane.heightProperty());
        rectangle2.widthProperty().bind(mainPane.widthProperty().subtract(270));
        rectangle3.widthProperty().bind(mainPane.widthProperty());
        rectangle3.heightProperty().bind(mainPane.heightProperty());
        messagesPane.prefHeightProperty().bind(mainPane.heightProperty().subtract(160));
        messagesPane.heightProperty().addListener((obs, oldVal, newVal) -> {
            if (needScrollDown) {
                scrollPane.setVvalue(1);
                needScrollDown = false;
            }
        });
        scrollPane.vvalueProperty().addListener((obs, oldVal, newVal) -> {
            if (Main.selectedChatId == -1) return;
            if (scrollPane.getVvalue() == 0 && Main.messenger.getChats().get(Main.selectedChatId)
                    .getMessages().getFirst().getMessageId() != 0) {
                Cursor<Message> cursor = Main.messenger.getChats().get(Main.selectedChatId).getMessages().getFirstCursor();
                cursor.previous();
                try {
                    Main.messenger.loadMessages(Main.selectedChatId);
                } catch (IOException _) {
                }
                while (cursor.hasPrevious()) {
                    addMessage(cursor.previous(), true);
                }
            }
        });
        Main.controller = this;
    }
}
