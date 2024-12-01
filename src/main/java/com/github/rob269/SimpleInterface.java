package com.github.rob269;

import com.github.rob269.io.ResourcesIO;
import com.github.rob269.io.ServerIO;
import com.github.rob269.io.ServerResponseException;
import com.github.rob269.rsa.RSAClientKeys;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

public class SimpleInterface {
    private static final Logger LOGGER = Logger.getLogger(SimpleInterface.class.getName());
    private static volatile boolean keepAlive = false;
    private KeepAliveThread keepAliveThread;
    private final ServerIO serverIO;
    private static Map<String, List<Message>> messages = new HashMap<>();
    private final Messenger messenger;

    public SimpleInterface(ServerIO serverIO) {
        this.serverIO = serverIO;
        messenger = new Messenger(serverIO);
    }

    public void updateMessages() {
        messages = messenger.getMessagesFromCache();
        System.out.println("It's work!");
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

    public void keepAlive() {
        if (!keepAlive){
            keepAlive = true;
            keepAliveThread = new KeepAliveThread(serverIO, 5000);
            keepAliveThread.start();
        }
    }

    public void uiPanel() {
        while (!serverIO.isClosed()){
            System.out.println("\nUser:" + RSAClientKeys.getUserId());
            System.out.println("1. Get messages\n2. Send message\n3. Exit\n4. Ping\n5. Get sent messages");
            Scanner scanner = new Scanner(System.in);
            String input = scanner.nextLine();
            switch (input) {
                case "1" -> {
                    Map<String, List<Message>> messages = messenger.getNewMessages();
                    printMap(messages);
                }
                case "2" -> {
                    String recipient = scanner.nextLine();
                    String message = scanner.nextLine();
                    System.out.println(recipient);
                    System.out.println(message);
                    messenger.sendMessage(new Message(recipient, RSAClientKeys.getUserId(), message));
                }
                case "3" -> serverIO.close();
                case "4" -> System.out.println(ping()+"ms");
                case "5" -> {
                    long start = System.currentTimeMillis();
                    Map<String, List<Message>> messages = messenger.getSentMessages();
                    printMap(messages);
                    System.out.println((System.currentTimeMillis()-start) + "ms");
                }
            }
        }
    }

    private void printMap(Map<String, List<Message>> map) {
        String[] senders = map.keySet().toArray(new String[0]);
        for (String sender : senders) {
            List<Message> m = map.get(sender);
            for (Message message : m) {
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
        keepAliveThread.updateTimer();
    }

    public static boolean isKeepAlive() {
        return keepAlive;
    }

    public static void disableKeepAlive() {
        keepAlive = false;
    }
}