/*
This file will be used in future
 */


//package com.github.rob269.messages;
//
//import com.github.rob269.LogFormatter;
//import com.github.rob269.Main;
//import com.github.rob269.SimpleUserInterface;
//import com.github.rob269.io.ResourcesIO;
//import com.github.rob269.io.ServerIO;
//import com.github.rob269.io.ServerResponseException;
//import com.github.rob269.rsa.RSAClientKeys;
//
//import java.io.*;
//import java.util.*;
//import java.util.logging.Logger;
//
//public class Messenger {
//    private static final Logger LOGGER = Logger.getLogger(Messenger.class.getName());
//    private final ServerIO serverIO;
//    public Messenger(ServerIO serverIO) {
//        this.serverIO = serverIO;
//    }
//
//    public List<Message> sortByDate(Map<String, List<Message>> messages) {
//        List<Message> messageList = new ArrayList<>();
//        String[] keys = messages.keySet().toArray(new String[0]);
//        for (String key : keys) {
//            messageList.addAll(messages.get(key));
//        }
//        return quickSort(messageList);
//    }
//

//
//    private int getMessagesCount(Map<String, List<Message>> map) {
//        String[] users = map.keySet().toArray(new String[0]);
//        int count = 0;
//        for (String user : users) count += map.get(user).size();
//        return count;
//    }
//
//    private Map<String, List<Message>> sumMessages(Map<String, List<Message>> a, Map<String, List<Message>> b) {
//        String[] users = a.keySet().toArray(new String[0]);
//        for (String user : users) {
//            if (b.containsKey(user)) {
//                List<Message> messageList = b.get(user);
//                messageList.addAll(a.get(user));
//                b.put(user, messageList);
//            }
//            else {
//                b.put(user, a.get(user));
//            }
//        }
//        return b;
//    }
//
//    public void sendMessage(Message message) {
//        serverIO.write("SEND MESSAGE");
//        String[] strMessage = new String[]{message.getRecipient(), message.getMessage()};
//        serverIO.write(strMessage);
//        try {
//            String response = serverIO.readFirst();
//            if (!response.equals("MESSAGE OK"))
//                serverIO.close();
//        } catch (ServerResponseException e) {
//            LOGGER.warning("Server response exception\n" + LogFormatter.formatStackTrace(e));
//            serverIO.close();
//        }
//    }
//
//    public Map<String, List<Message>> getMessagesFromCache() {
//        Map<String, List<Message>> messages = new HashMap<>();
//        if (ResourcesIO.isExist("Messages/" + RSAClientKeys.getUserId())) {
//            MessageCache messagesCache = (MessageCache) Objects.requireNonNull(ResourcesIO.readObject("Messages/" + RSAClientKeys.getUserId()));
//            Map<String, List<Message>> cache = messagesCache.messages();
//            Map<String, List<Message>> sum = sumMessages(cache, getNewMessages());
//            int count = getMessagesCount(sum);
//            serverIO.write("GET MESSAGES COUNT");
//            int a = -1;
//            try {
//                a = Integer.parseInt(serverIO.readFirst());
//            } catch (ServerResponseException e) {
//                LOGGER.warning(LogFormatter.formatStackTrace(e));
//            }
//            if (count != a) {
//                Map<String, List<Message>> allMessages = getAllMessages();
//                MessageCache messageCache = messagesCache.setMessages(allMessages);
//                ResourcesIO.writeObject(messageCache, "Messages/" + RSAClientKeys.getUserId());
//                return allMessages;
//            }
//            else {
//                if (!sum.equals(cache)) {
//                    ResourcesIO.writeObject(messagesCache.setMessages(sum), "Messages/" + RSAClientKeys.getUserId());
//                }
//                return sum;
//            }
//        }
//        else {
//            Map<String, List<Message>> newMessages = getAllMessages();
//            ResourcesIO.writeObject(new MessageCache(newMessages, new HashMap<>()), "Messages/" + RSAClientKeys.getUserId());
//            return newMessages;
//        }
//    }
//
//    private void writeReceivedMessagesCache(Map<String, List<Message>> messages) {
//       if (ResourcesIO.isExist("Messages/" + RSAClientKeys.getUserId())) {
//           MessageCache messageCache = (MessageCache) ResourcesIO.readObject("Messages/" + RSAClientKeys.getUserId());
//           Map<String, List<Message>> cache = messageCache.messages();
//           cache = sumMessages(cache, messages);
//           ResourcesIO.writeObject(new MessageCache(cache, messageCache.sentMessages()), "Messages/" + RSAClientKeys.getUserId());
//       }
//       else {
//           ResourcesIO.writeObject(new MessageCache(messages, new HashMap<>()), "Messages/" + RSAClientKeys.getUserId());
//       }
//    }
//
//    public Map<String, List<Message>> getAllMessages() {
//        serverIO.write("GET MESSAGES");
//        return getMessages(false);
//    }
//
//    public Map<String, List<Message>> getNewMessages() {
//        serverIO.write("GET NEW MESSAGES");
//        return getMessages(false);
//    }
//
//    public Map<String, List<Message>> getSentMessages() {
//        if (ResourcesIO.isExist("Messages/" + RSAClientKeys.getUserId())) {
//            MessageCache messageCache = (MessageCache) Objects.requireNonNull(ResourcesIO.readObject("Messages/" + RSAClientKeys.getUserId()));
//            Map<String, List<Message>> cache = messageCache.sentMessages();
//            int count = getMessagesCount(cache);
//            serverIO.write("GET SENT MESSAGES COUNT");
//            int a = -1;
//            try {
//                a = Integer.parseInt(serverIO.readFirst());
//            } catch (ServerResponseException e) {
//                LOGGER.warning("Server response exception\n" + LogFormatter.formatStackTrace(e));
//            }
//            if (count != a) {
//                serverIO.write("GET SENT MESSAGES");
//                Map<String, List<Message>> messages = getMessages(true);
//                ResourcesIO.writeObject(messageCache.setSentMessages(messages), "Messages/" + RSAClientKeys.getUserId());
//                return messages;
//            }
//            else {
//                return cache;
//            }
//        }
//        else {
//            serverIO.write("GET SENT MESSAGES");
//            Map<String, List<Message>> messages = getMessages(true);
//            ResourcesIO.writeObject(new MessageCache(new HashMap<>(), messages), "Messages/" + RSAClientKeys.getUserId());
//            return messages;
//        }
//    }
//
//    private Map<String, List<Message>> getMessages(boolean ifSent) {
//        Map<String, List<Message>> messages = new HashMap<>();
//        String user = "";
//        List<Message> userMessageList = new ArrayList<>();
//        while (true) {
//            try {
//                List<String> line = serverIO.read();
//                if (line.getFirst().equals("#USER")) {
//                    if (!user.isEmpty()) {
//                        messages.put(user, userMessageList);
//                        userMessageList = new ArrayList<>();
//                    }
//                    user = serverIO.readFirst();
//                    user = user.substring(1, user.length()-1);
//                }
//                else if (line.getFirst().equals("#END")) {
//                    if (!user.isEmpty()) messages.put(user, userMessageList);
//                    break;
//                }
//                else {
//                    String[] date = line.get(1).split(" ")[0].split("-");
//                    String[] time = line.get(1).split(" ")[1].split(":");
//                    userMessageList.add(new Message(!ifSent ? RSAClientKeys.getUserId() : user, !ifSent ? user : RSAClientKeys.getUserId(), line.getFirst().substring(1, line.getFirst().length()-1), new GregorianCalendar(
//                            Integer.parseInt(date[0].substring(1)), Integer.parseInt(date[1])-1, Integer.parseInt(date[2]), Integer.parseInt(time[0]), Integer.parseInt(time[1]),
//                            Integer.parseInt(time[2].substring(0, time[2].length()-1))
//                    )));
//                }
//            } catch (ServerResponseException e) {
//                LOGGER.warning("Server response exception\n" + LogFormatter.formatStackTrace(e));
//            }
//        }
//        return messages;
//    }
//
////    public void sendTxtFile(File file) {
////        if (file.exists()) {
////            long start = System.currentTimeMillis();
////            try {
////                serverIO.write("SEND FILE");
////                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
////                String line;
////                StringBuilder stringBuilder = new StringBuilder();
////                while ((line=bufferedReader.readLine()) != null) {
////                    stringBuilder.append(line).append("\n");
////                }
////                serverIO.write(stringBuilder.toString());
////                serverIO.write("#END");
////            } catch (IOException e) {
////                LOGGER.warning("File writing exception\n" + LogFormatter.formatStackTrace(e));
////            }
////            System.out.println((System.currentTimeMillis()-start) + "ms");
////        }
////    }
//}
///*
//
//
//
//
// */