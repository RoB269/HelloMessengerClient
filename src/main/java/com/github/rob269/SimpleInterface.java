package com.github.rob269;

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

    public SimpleInterface(ServerIO serverIO) {
        this.serverIO = serverIO;
    }

    public void uiPanel() {
        while (!serverIO.isClosed()){
            System.out.println("\nUser:" + RSAClientKeys.getUserId());
            System.out.println("1. Get messages\n2. Send message\n3. Exit\n4. Ping");
            Scanner scanner = new Scanner(System.in);
            String input = scanner.nextLine();
            switch (input) {
                case "1" -> {
                    Map<String, List<Message>> messages = getMessages();
                    String[] senders = messages.keySet().toArray(new String[0]);
                    for (int i = 0; i < senders.length; i++) {
                        List<Message> m = messages.get(senders[i]);
                        for (int j = 0; j < m.size(); j++) {
                            System.out.println(m.get(j));
                        }
                    }
                }
                case "2" -> {
                    String recipient = scanner.nextLine();
                    String message = scanner.nextLine();
                    System.out.println(recipient);
                    System.out.println(message);
                    sendMessage(new Message(recipient, RSAClientKeys.getUserId(), message));
                }
                case "3" -> serverIO.close();
                case "4" -> {
                    System.out.println(ping()+"ms");
                }
            }
        }
    }

    public void sendTxtFile(File file) {
        if (file.exists()) {
            try {
                serverIO.write("SEND FILE");
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
                String line;
                while ((line=bufferedReader.readLine()) != null) {
                    serverIO.write(line);
                }
                serverIO.write("END");
            } catch (IOException e) {
                LOGGER.warning("File writing exception");
                e.printStackTrace();
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

    public Map<String, List<Message>> getMessages() {
        Map<String, List<Message>> messages = new HashMap<>();
        serverIO.write("GET MESSAGES");
        String user = "";
        List<Message> userMessageList = new ArrayList<>();
        while (true) {
            try {
                List<String> line = serverIO.read();
                if (line.getFirst().equals("#USER")) {
                    if (!user.isEmpty()) {
                        messages.put(user, userMessageList);
                        userMessageList = new ArrayList<>();
                    }
                    user = serverIO.readFirst();
                    user = user.substring(1, user.length()-1);
                }
                else if (line.getFirst().equals("#END")) {
                    if (!user.isEmpty()) messages.put(user, userMessageList);
                    break;
                }
                else {
                    String[] date = line.get(1).split(" ")[0].split("-");
                    String[] time = line.get(1).split(" ")[1].split(":");
                    userMessageList.add(new Message(RSAClientKeys.getUserId(), user, line.getFirst().substring(1, line.getFirst().length()-1), new GregorianCalendar(
                            Integer.parseInt(date[0].substring(1)), Integer.parseInt(date[1])-1, Integer.parseInt(date[2]), Integer.parseInt(time[0]), Integer.parseInt(time[1]),
                            Integer.parseInt(time[2].substring(0, time[2].length()-1))
                    )));
                }
            } catch (ServerResponseException e) {
                LOGGER.warning("Server response exception");
            }
        }
        return messages;
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