package com.github.rob269;

import java.util.Calendar;
import java.util.Date;

public class Message {
    private String recipient;
    private String sender;
    private String message;
    private Calendar date;

    public Message(String recipient, String sender, String message, Calendar date) {
        this.recipient = recipient;
        this.sender = sender;
        this.message = message;
        this.date = date;
    }

    public Message(String recipient, String sender, String message) {
        this.recipient = recipient;
        this.sender = sender;
        this.message = message;
    }

    @Override
    public String toString() {
        return "(" + date.getTime() + ")" + sender + "-->" + recipient + ":" + message;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Calendar getDate() {
        return date;
    }

    public void setDate(Calendar date) {
        this.date = date;
    }
}
