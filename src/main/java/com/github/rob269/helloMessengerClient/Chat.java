package com.github.rob269.helloMessengerClient;

import com.github.rob269.helloMessengerClient.util.LinkedList;
import java.util.List;

public class Chat {
    private final String name;
    private final long chatId;
    private final Status status;
    private final boolean isPrivate;
    private final LinkedList<Message> messages = new LinkedList<>();

    public enum Status {
        NEW,
        OK,
        BLOCK
    }

    public Chat(long chatId, String name, Status status, Message lastMessage, boolean isPrivate) {
        this.name = name;
        this.chatId = chatId;
        this.status = status;
        this.isPrivate = isPrivate;
        if (lastMessage != null) messages.add(lastMessage);
    }

    public void loadMessages(List<Message> messageList) {
        for (Message message : messageList) {
            messages.addFirst(message);
        }
    }

    public void addMessage(Message message) {
        messages.addLast(message);
    }

    public LinkedList<Message> getMessages() {
        return messages;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public Status getStatus() {
        return status;
    }

    public String getName() {
        return name;
    }

    public long getChatId() {
        return chatId;
    }

    @Override
    public String toString() {
        if (!messages.isEmpty() && messages.getLast().getMessageId() != 0)
            return (isPrivate ? "Contact:" : "Chat:") + "[" + chatId + "]" + name + "\tLast message:" + messages.getLast();
        return (isPrivate ? "Contact:" : "Chat:") +  "[" + chatId + "]" + name;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Chat chat)) return false;
        return this.chatId == chat.chatId;
    }
}
