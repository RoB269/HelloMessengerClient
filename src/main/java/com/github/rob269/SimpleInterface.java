package com.github.rob269;

import com.github.rob269.io.ServerIO;
import com.github.rob269.io.ServerResponseException;
import com.github.rob269.rsa.RSAClientKeys;

import java.util.Scanner;
import java.util.logging.Logger;

public class SimpleInterface {
    private static final Logger LOGGER = Logger.getLogger(SimpleInterface.class.getName());
    private static volatile boolean keepAlive = false;
    private KeepAliveThread keepAliveThread;
    private final ServerIO serverIO;

    public SimpleInterface(ServerIO serverIO) {
        this.serverIO = serverIO;
    }

    public void uiPanel() {
        while (!serverIO.isClosed()){
            System.out.println("1. Get messages\n2. Send message\n3. Exit");
            Scanner scanner = new Scanner(System.in);
            int input = scanner.nextInt();
            switch (input) {
                case 1 -> {

                }
                case 2 -> {
                    String recipient = scanner.next();
                    String message = scanner.next();
                    sendMessage(new Message(recipient, RSAClientKeys.getUserId(), message));
                }
                case 3 -> serverIO.close();
            }
        }
    }

    public void sendMessage(Message message) {
        serverIO.write("SEND MESSAGE");
        String[] strMessage = new String[]{message.getRecipient(), message.getMessage()};
        serverIO.write(strMessage);
        try {
            String response = serverIO.readFirst();
            if (!response.equals("MESSAGE OK"))
                serverIO.close();
        } catch (ServerResponseException e) {
            LOGGER.warning("Server response exception");
            e.printStackTrace();
            serverIO.close();
        }
    }

    public void updateTimer() {
        keepAliveThread.updateTimer();
    }

    public void keepAlive() {
        if (!keepAlive){
            keepAlive = true;
            keepAliveThread = new KeepAliveThread(serverIO);
            keepAliveThread.start();
        }
    }

    public static boolean isKeepAlive() {
        return keepAlive;
    }

    public static void disableKeepAlive() {
        keepAlive = false;
    }
}