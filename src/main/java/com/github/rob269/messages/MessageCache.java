package com.github.rob269.messages;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class MessageCache implements Serializable {
    private Map<String, List<Message>> messages;
    private Map<String, List<Message>> sentMessages;
    public MessageCache(Map<String, List<Message>> messages, Map<String, List<Message>> sentMessages) {
        this.messages = messages;
        this.sentMessages = sentMessages;
    }

    public Map<String, List<Message>> messages() {
        return messages;
    }

    public Map<String, List<Message>> sentMessages() {
        return sentMessages;
    }

    public MessageCache setMessages(Map<String, List<Message>> messages) {
        this.messages = messages;
        return this;
    }

    public MessageCache setSentMessages(Map<String, List<Message>> messages) {
        this.sentMessages = messages;
        return this;
    }

    @Override
    public String toString() {
        return messages.toString() + "\n" + sentMessages.toString() + "\n";
    }
}
