package com.github.rob269.helloMessengerClient;

import com.github.rob269.helloMessengerClient.io.HMPBatch;
import com.github.rob269.helloMessengerClient.io.ServerIO;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
            String[] privateChats = serverIO.readString(false).split("\\\\\\\\");
            String[] publicChats = serverIO.readString(false).split("\\\\\\\\");
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

    public static LocalDateTime getDate(String stringDate) {
        ZonedDateTime zonedDateTime = LocalDateTime.parse(stringDate, Main.dateTimeFormatter).atZone(ZoneId.of("UTC")).withZoneSameInstant(ZoneId.systemDefault());
        return zonedDateTime.toLocalDateTime();
    }

    public Chat addContact(String username) {
        try {
            LOGGER.fine("Adding new contact");
            if (username.contains("\\")) {
                LOGGER.fine("Username contains forbidden symbols");
                Main.controller.printMenuErrorMessage("Username contains forbidden symbols");
                return null;
            }
            if (username.equals(Client.getUsername())) {
                LOGGER.fine("This contact cannot be added");
                Main.controller.printMenuErrorMessage("This contact cannot be added");
                return null;
            }
            for (long chatId : chatIds) {
                Chat chat = chats.get(chatId);
                if (chat.isPrivate() && chat.getName().equals(username)) {
                    LOGGER.fine("Contact already exist");
                    Main.controller.printMenuErrorMessage("Contact already exist");
                    return null;
                }
            }
            HMPBatch batch = serverIO.writeBatch(81, 1, false);
            batch.write(username);
            byte response = serverIO.readCommand();
            switch (response) {
                case 54 -> {
                    String[] chatId = serverIO.readString().split("\\\\\\\\");
                    System.out.println(chatId[1]);
                    Chat chat = new Chat(Long.parseLong(chatId[0]), username, Chat.Status.OK,
                            new Message(0, "null", getDate(chatId[1]), ""), true);
                    chats.put(chat.getChatId(), chat);
                    return chat;
                }
                case 64 -> {
                    LOGGER.fine("User doesn't exist");
                    Main.controller.printMenuErrorMessage("User doesn't exist");
                }
            }
        } catch (IOException _) {
            Main.controller.printErrorMessage("Disconnected from the server");
        }
        return null;
    }

    public Chat createChat(String chatName) {
        try {
            LOGGER.fine("Adding new chat");
            if (chatName.contains("\\")) {
                LOGGER.fine("Chat name contains forbidden symbols");
                Main.controller.printMenuErrorMessage("Chat name contains forbidden symbols");
                return null;
            }
            HMPBatch batch = serverIO.writeBatch(82, 1, false);
            batch.write(chatName);
            byte response = serverIO.readCommand();
            if (response == 54) {
                String[] strChat = serverIO.readString().split("\\\\\\\\");
                Chat chat = new Chat(Long.parseLong(strChat[0]), chatName, Chat.Status.OK,
                        new Message(0, "null", getDate(strChat[1]), ""), false);
                chats.put(chat.getChatId(), chat);
                return chat;
            }
        } catch (IOException _) {
            Main.controller.printErrorMessage("Disconnected from the server");
        }
        return null;
    }

    public Chat connectToTheChat(long chatId) {
        try {
            for (long id : chatIds) {
                if (chats.get(id).getChatId() == chatId) {
                    LOGGER.fine("Chat already exist");
                    Main.controller.printMenuErrorMessage("Chat already exist");
                    return null;
                }
            }
            HMPBatch batch = serverIO.writeBatch(86, 1, false);
            batch.write(chatId);
            byte response = serverIO.readCommand();
            switch (response) {
                case 57 -> {
                    String[] strChat = serverIO.readString().split("\\\\\\\\");
                    Chat chat = new Chat(chatId, strChat[0], Chat.Status.OK, new Message(Long.parseLong(strChat[1]),
                            strChat[2], getDate(strChat[3]), strChat[4].replaceAll("\\\\&", "\\\\")), false);
                    chats.put(chatId, chat);
                    return chat;
                }
                case 67 -> {
                    LOGGER.fine("Chat does not exist");
                    Main.controller.printMenuErrorMessage("Chat does not exist");
                    return null;
                }
            }
        } catch (IOException _) {
            Main.controller.printErrorMessage("Disconnected from the server");
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
                    String[] meta = serverIO.readString(false).split("\\\\\\\\");
                    Message sentMessage = new Message(Long.parseLong(meta[0]), Client.getUsername(), getDate(meta[1]), message);
                    chats.get(chatId).getMessages().add(sentMessage);
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
        batch.write(chats.get(chatId).getMessages().getFirst().getMessageId() + "\\\\" + MESSAGE_PACK_SIZE + "\\\\;");
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
                getDate(strMessage[3].replace("T", " ")), strMessage[4].replaceAll("\\\\&", "\\\\"));
        long chatId = Long.parseLong(strMessage[0]);
        chats.get(chatId).addMessage(message);
        return chatId;
    }

    public Chat getNewChat() throws IOException {
        String[] strChat = serverIO.readString().split("\\\\\\\\");
        Chat chat = new Chat(Long.parseLong(strChat[0]), strChat[1], Chat.Status.NEW,
                new Message(Long.parseLong(strChat[2]), strChat[3], getDate(strChat[4].replace("T", " ")),
                        strChat[5].replaceAll("\\\\&", "\\\\")), strChat[6].equals("1"));
        chats.put(chat.getChatId(), chat);
        chatIds.add(chat.getChatId());
        return chat;
    }

    public void close() throws IOException {
        serverIO.close();
    }

    public boolean isClosed() {
        return serverIO.isClosed();
    }
}