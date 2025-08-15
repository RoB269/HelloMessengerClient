package com.github.rob269;

import com.github.rob269.io.ResourcesIO;
import com.github.rob269.io.ServerIO;
import com.github.rob269.io.ServerResponseException;
import com.github.rob269.rsa.RSAClientKeys;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

public class SimpleInterface {
    public static String lang = "EN";
    private static final Logger LOGGER = Logger.getLogger(SimpleInterface.class.getName());
    private static volatile boolean keepAlive = false;
    private KeepAliveThread keepAliveThread;
    private final ServerIO serverIO;
    private static Map<String, List<Message>> messages = new HashMap<>();
    private static Map<String, List<Message>> sentMessages = new HashMap<>();
    private final Messenger messenger;

    public SimpleInterface(ServerIO serverIO) {
        this.serverIO = serverIO;
        messenger = new Messenger(serverIO);
    }

    public void updateMessages() {
        messages = messenger.getMessagesFromCache();
        System.out.println(1);
        printMap(messages);
    }

    public void spamer() {
        for (int i = 0; i < 100; i++) {
            messenger.sendMessage(new Message("SPAM_TEST_USER", RSAClientKeys.getUserId(), String.valueOf(i)));
        }
        serverIO.close();
    }

    public void checking() {
        messenger.checkingMessages(!Messenger.getChecking());
    }

    public boolean isChecking() {
        return Messenger.getChecking();
    }

    public void keepAlive() {
        if (!keepAlive){
            keepAlive = true;
            keepAliveThread = new KeepAliveThread(serverIO, 5000);
            keepAliveThread.start();
        }
    }

    public void uiPanel() {
        while (!serverIO.isClosed()){
            System.out.println((lang.equals("RU") ? "\nПользователь:" : "\nUser:") + RSAClientKeys.getUserId());
            if (!Main.isMini()){
                System.out.println(lang.equals("RU") ? """
                        1. Получить новые сообщения
                        2. Получить все сообщения
                        3. Отправить сообщение
                        4. Выйти
                        5. Пинг
                        6. Получить отправленные сообщения
                        7. Отправить файл на сервер""" :
                        """
                                1. Get new messages
                                2. Get all messages
                                3. Send message
                                4. Exit
                                5. Ping
                                6. Get sent messages
                                7. Send file to server""");
                Scanner scanner = new Scanner(System.in);
                String input = scanner.nextLine();
                switch (input) {
                    case "1" -> {
                        Map<String, List<Message>> messages = messenger.getNewMessages();
                        printMap(messages);
                    }
                    case "2" -> {
                        Map<String, List<Message>> messages = messenger.getMessagesFromCache();
                        printMap(messages);
                    }
                    case "3" -> {
                        String recipient = scanner.nextLine();
                        String message = scanner.nextLine();
                        System.out.println(recipient);
                        System.out.println(message);
                        messenger.sendMessage(new Message(recipient, RSAClientKeys.getUserId(), message));
                    }
                    case "4" -> serverIO.close();
                    case "5" -> System.out.println(ping() + "ms");
                    case "6" -> {
                        long start = System.currentTimeMillis();
                        Map<String, List<Message>> messages = messenger.getSentMessages();
                        printMap(messages);
                        System.out.println((System.currentTimeMillis() - start) + "ms");
                    }
                    case "7" -> {
                        messenger.sendTxtFile(new File("file.txt"));
                    }
                }
            }
            else {
                System.out.println(lang.equals("RU") ? """
                        1. Отправить сообщение
                        2. Получить сообщения
                        3. Выйти""" :
                        """
                                1. Send message
                                2. Get messages
                                3. Exit""");
                Scanner scanner = new Scanner(System.in);
                String input = scanner.nextLine();
                switch (input) {
                    case "1"  -> {
                        String recipient = scanner.nextLine();
                        String message = scanner.nextLine();
                        System.out.println(recipient);
                        System.out.println(message);
                        messenger.sendMessage(new Message(recipient, RSAClientKeys.getUserId(), message));
                    }
                    case "2" -> {
                        Map<String, List<Message>> messages = messenger.getMessagesFromCache();
                        printMap(messages);
                    }
                    case "3"  -> serverIO.close();
                }
            }
        }
    }

    private void printMap(Map<String, List<Message>> map) {
        List<Message> messageList = messenger.sortByDate(map);
        if (messageList.isEmpty()) {
            System.out.println(lang.equals("RU") ? "Сообщений нет" : "No messages");
        }
        else {
            for (Message message : messageList) {
                System.out.println(message);
            }
        }
    }

    public int ping() {
        long start = System.currentTimeMillis();
        serverIO.write("PING");
        try {
            String response = serverIO.readFirst();
            if (response.equals("PING")) return (int) ((int) System.currentTimeMillis()-start);
        } catch (ServerResponseException e) {
            LOGGER.warning("Ping exception");
        }
        return -1;
    }


    public void updateTimer() {
        if (keepAlive) keepAliveThread.updateTimer();
        if (isChecking()) messenger.updateCheckingTimer();
    }

    public static boolean isKeepAlive() {
        return keepAlive;
    }

    public static void disableKeepAlive() {
        keepAlive = false;
    }
}