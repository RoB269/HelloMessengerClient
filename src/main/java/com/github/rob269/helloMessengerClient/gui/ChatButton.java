package com.github.rob269.helloMessengerClient.gui;

import com.github.rob269.helloMessengerClient.Chat;
import javafx.scene.control.Button;

public class ChatButton extends Button {
    private final long chatId;

    public ChatButton(long chatId, String name) {
        super(name);
        this.chatId = chatId;
    }

    public long getChatId() {
        return chatId;
    }
}
