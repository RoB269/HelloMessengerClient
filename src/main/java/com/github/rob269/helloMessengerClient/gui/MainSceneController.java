package com.github.rob269.helloMessengerClient.gui;

import com.github.rob269.helloMessengerClient.*;
import com.github.rob269.helloMessengerClient.util.Cursor;
import com.github.rob269.helloMessengerClient.util.LinkedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.io.IOException;
import java.net.URL;
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
        menuPane.setVisible(true);
    }

    @FXML
    void onAddChatButton(ActionEvent event) {
        menuText.setText("Enter chat id");
        type = MenuType.ADD_CHAT;
        menuPane.setVisible(true);
    }

    @FXML
    void onAddContactButton(ActionEvent event) {
        menuText.setText("Enter contact name");
        type = MenuType.ADD_CONTACT;
        menuPane.setVisible(true);
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
                    System.out.println(1);//todo HEREEEEEEEEEEEEEEEEEEEEE
                }
                case MenuType.ADD_CHAT -> {
                    System.out.println(2);
                }
                case MenuType.CREATE_NEW_CHAT -> {
                    System.out.println(3);
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

    @FXML
    void onReconnectButton(ActionEvent event) {
        String message = Main.serverConnect();
        if (message.equals("OK")) {
            hideErrorMessage();
            Main.initChats();
        }
        else {
            printErrorMessage(message + "\nReconnection failed");
        }
    }

    @FXML
    void onSendMessageButton(ActionEvent event) {
        sendMessage();
    }

    private void sendMessage() {
        if (!enterTextField.getText().isEmpty() && Main.selectedChatId != -1) {
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
        contactName.setText(button.getText());
        LinkedList<Message> messages = Main.messenger.getChats().get(button.getChatId()).getMessages();
        if (messages.getFirst().getMessageId() != 0 && messages.size() < Messenger.MESSAGE_PACK_SIZE) {
            try {
                Main.messenger.loadMessages(button.getChatId());
            } catch (IOException _) {
            }
        }

        messagesPane.getChildren().clear();
        Cursor<Message> cursor = messages.getLastCursor();
        while (cursor.hasPrevious()) {
            addMessage(cursor.previous(), true);
        }
        Main.selectedChatId = button.getChatId();
        needScrollDown = true;
        contactName.setVisible(true);
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

        Text additionalData = new Text(message.getSender() + " at " + message.getDate().getTime());
        VBox.setMargin(additionalData, new Insets(0, 0, 0, 7));
        VBox vBox = new VBox(additionalData, stackPane);
        vBox.setPadding(new Insets(7, 0, 0, 0));


        if (addFirst) {//todo fix autoscroll
            messagesPane.getChildren().addFirst(vBox);
        }
        else messagesPane.getChildren().add(vBox);
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
            if (Main.selectedChatId != -1 && scrollPane.getVvalue() == 0 && Main.messenger.getChats().get(Main.selectedChatId)
                    .getMessages().getFirst().getMessageId() != 0) {
                System.out.println(123);
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
