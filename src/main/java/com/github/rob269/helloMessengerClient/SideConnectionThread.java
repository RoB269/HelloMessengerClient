package com.github.rob269.helloMessengerClient;

import javafx.application.Platform;

import java.io.IOException;

public class SideConnectionThread extends Thread {
    private final Messenger messenger;

    public SideConnectionThread(Messenger messenger) {
        this.messenger = messenger;
    }

    @Override
    public void run() {
        try {
            while (!messenger.isClosed()) {
                byte command = messenger.readCommand();
                switch (command) {
                    case -10 -> {
                        long chatId = messenger.getNewMessage();
                        Platform.runLater(() -> {
                            if (Main.selectedChatId == chatId) Main.controller.addReceivedMessage();
                        });
                    }
                    case -11 -> {
                        Chat chat = messenger.getNewChat();
                        Platform.runLater(() -> {Main.controller.addChat(chat);});
                    }
                }
            }
        } catch (IOException _) {
        }
    }
}
