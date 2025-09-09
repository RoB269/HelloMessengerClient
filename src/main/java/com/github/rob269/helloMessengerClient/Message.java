package com.github.rob269.helloMessengerClient;

import java.io.Serializable;
import java.util.Calendar;

public class Message implements Serializable {
    private final long messageId;
    private final String sender;
    private final Calendar date;
    private final String message;

    public Message(long messageId, String sender, Calendar date, String message) {
        this.messageId = messageId;
        this.sender = sender;
        this.date = date;
        this.message = message;
    }

    @Override
    public String toString() {
        return "(" + date.getTime() + ")" + sender + ": " + message;
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

    public Calendar getDate() {
        return date;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Message msg)) return false;
        return messageId == msg.messageId;
    }
}
