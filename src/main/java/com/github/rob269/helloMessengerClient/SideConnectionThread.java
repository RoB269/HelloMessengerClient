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
                        if (Main.selectedChatId == chatId) {
                            Platform.runLater(() -> {Main.controller.addReceivedMessage();});
                        }
                    }
                }
            }
        } catch (IOException _) {
        }
    }
}
