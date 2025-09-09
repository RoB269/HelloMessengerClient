package com.github.rob269.helloMessengerClient;

import com.github.rob269.helloMessengerClient.io.HMPBatch;
import com.github.rob269.helloMessengerClient.io.ServerIO;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

public class Messenger {
    public static final int MESSAGE_PACK_SIZE = 30;
    private static final Logger LOGGER = Logger.getLogger(Messenger.class.getName());
    private final Map<Long, Chat> chats = new HashMap<>();
    private final List<Long> chatIds = new ArrayList<>();
    private final ServerIO serverIO;
    public Messenger(ServerIO serverIO) {
        this.serverIO = serverIO;
    }

    public Map<Long, Chat> getChats() {
        return chats;
    }

    public List<Long> getChatIds() {
        return chatIds;
    }

    public void requestChatsFromServer() throws IOException {
        serverIO.writeCommand(80);
        byte response = serverIO.readCommand();
        if (response == 53) {
            String[] privateChats = serverIO.readString(true).split("\\\\\\\\");
            String[] publicChats = serverIO.readString(true).split("\\\\\\\\");
            for (Chat chat : formatChats(privateChats, true)) {
                chats.put(chat.getChatId(), chat);
                chatIds.add(chat.getChatId());
            }
            for (Chat chat : formatChats(publicChats, false)) {
                chats.put(chat.getChatId(), chat);
                chatIds.add(chat.getChatId());
            }
        }
    }

    private List<Chat> formatChats(String[] chats, boolean isPrivate) {
        List<Chat> formated = new ArrayList<>();
        for (int i = 0; i < chats.length/7; i++) {
            Message lastMessage = chats[i*7+3].isEmpty() ? null : new Message(Long.parseLong(chats[i*7+3]), chats[i*7+4], getDate(chats[i*7+5]),
                    chats[i*7+6].replaceAll("\\\\&", "\\\\"));
            Chat chat = new Chat(Long.parseLong(chats[i*7]), chats[i*7+1],
                    chats[i*7+2].equals("o") ? Chat.Status.OK :
                            chats[i*7+2].equals("n") ? Chat.Status.NEW :
                                    chats[i*7+2].equals("b") ? Chat.Status.BLOCK : Chat.Status.NEW, lastMessage, isPrivate);
            formated.add(chat);
        }
        return formated;
    }

    public static GregorianCalendar getDate(String stringDate) {
        String[] dateParts = stringDate.split(" ");
        String[] dateString = dateParts[0].split("-");
        String[] timeString = dateParts[1].split(":");
        return new GregorianCalendar(Integer.parseInt(dateString[0]),
                Integer.parseInt(dateString[1])-1, Integer.parseInt(dateString[2]),
                Integer.parseInt(timeString[0]), Integer.parseInt(timeString[1]), Integer.parseInt(timeString[2]));
    }

    public Chat addContact(String username) throws IOException {
        LOGGER.fine("Adding new contact");
        if (username.contains("\\")) {
            LOGGER.fine("Username contains forbidden symbols");
            return null;
        }
        if (username.equals(Client.getUsername()) || username.isEmpty()) {
            LOGGER.fine("This contact cannot be added");
            return null;
        }
        HMPBatch batch = serverIO.writeBatch(81, 1, false);
        batch.write(username);
        byte response = serverIO.readCommand();
        switch (response) {
            case 54 -> {
                long chatId = serverIO.readLong();
                Chat chat = new Chat(chatId, username, Chat.Status.OK, null, true);
                chats.put(chat.getChatId(), chat);
                return chat;
            }
            case 62 -> LOGGER.fine("Contact already exist");
            case 64 -> LOGGER.fine("User doesn't exist");
        }
        return null;
    }

    public Chat createChat(String chatName) throws IOException{
        LOGGER.fine("Adding new chat");
        if (chatName.contains("\\")) {
            LOGGER.fine("Chat name contains forbidden symbols");
            return null;
        }
        if (chatName.isEmpty()) {
            LOGGER.fine("You cannot create a chat with that name");
        }
        HMPBatch batch = serverIO.writeBatch(82, 1, false);
        batch.write(chatName);
        byte response = serverIO.readCommand();
        if (response == 54) {
            long chatId = serverIO.readLong();
            Chat chat = new Chat(chatId, chatName, Chat.Status.OK, null, false);
            chats.put(chat.getChatId(), chat);
            return chat;
        }
        return null;
    }

    public Message sendMessage(String message, long chatId) {
        try {
            HMPBatch batch = serverIO.writeBatch(83, 2, false);
            batch.write(chatId);
            batch.write(message);
            byte response = serverIO.readCommand();
            switch (response) {
                case 55 -> {
                    String[] meta = serverIO.readString().split("\\\\\\\\");
                    Message sentMessage = new Message(Long.parseLong(meta[0]), Client.getUsername(), getDate(meta[1].replace("T", " ")), message);
                    LOGGER.fine("Message is sent");
                    return sentMessage;
                }
                case 67 -> {
                    LOGGER.fine("Chat doesn't exist");
                }
                case 68 -> {
                    LOGGER.fine("User is not in the chat");
                }
                case 69 -> {
                    LOGGER.fine("User blocked this chat");
                }
            }
        } catch (IOException _) {
            Main.controller.printErrorMessage("Disconnected from the server");
        }
        return null;
    }

    public static Message formatMessage(String[] strMessage, int i) {
        return new Message(Long.parseLong(strMessage[i*4]), strMessage[i*4+1], getDate(strMessage[i*4+2]),
                strMessage[i*4+3].replaceAll("\\\\&", "\\\\"));
    }

    public void loadMessages(long chatId) throws IOException {
        if (chats.get(chatId).getMessages().isEmpty()) return;
        HMPBatch batch = serverIO.writeBatch(84, 2, false);
        batch.write(chatId);
        batch.write(chats.get(chatId).getMessages().getFirst().getMessageId() + "\\" + MESSAGE_PACK_SIZE);
        byte response = serverIO.readCommand();
        if (response == 56) {
            String[] messages = serverIO.readString().split("\\\\\\\\");
            List<Message> messageList = new ArrayList<>();
            for (int i = 0; i < messages.length/4; i++) {
                messageList.add(formatMessage(messages, i));
            }
            chats.get(chatId).loadMessages(messageList);
        }
        else if (response == 68) {
            LOGGER.fine("User not in the chat");
        }
    }

    public byte readCommand() throws IOException {
        return serverIO.readCommand();
    }

    public long getNewMessage() throws IOException {
        String[] strMessage = serverIO.readString().split("\\\\\\\\");
        Message message = new Message(Long.parseLong(strMessage[1]), strMessage[2],
                Messenger.getDate(strMessage[3].replace("T", " ")), strMessage[4]);
        long chatId = Long.parseLong(strMessage[0]);
        chats.get(chatId).addMessage(message);
        return chatId;
    }

    public void close() throws IOException {
        serverIO.close();
    }

    public boolean isClosed() {
        return serverIO.isClosed();
    }
}