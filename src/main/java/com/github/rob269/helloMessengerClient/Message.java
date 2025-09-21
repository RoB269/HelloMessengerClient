package com.github.rob269.helloMessengerClient;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Message implements Serializable {
    private final long messageId;
    private final String sender;
    private final LocalDateTime date;
    private final String message;

    public Message(long messageId, String sender, LocalDateTime date, String message) {
        this.messageId = messageId;
        this.sender = sender;
        this.date = date;
        this.message = message;
    }

    @Override
    public String toString() {
        return "(" + date.format(Main.dateTimeFormatter) + ")" + sender + ": " + message;
    }

    public long getMessageId() {
        return messageId;
    }

    public String getSender() {
        return sender;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getDate() {
        return date;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Message msg)) return false;
        return messageId == msg.messageId;
    }
}
