package com.github.rob269.io;

import com.github.rob269.*;
import com.github.rob269.rsa.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.Socket;
import java.util.*;
import java.util.logging.Logger;

public class ServerIO implements AutoCloseable {
    private DataOutputStream dos;
    private DataInputStream dis;
    private static UserKey serverKey;
    private boolean isClosed = false;
    private boolean initialized = false;

    private static final Logger LOGGER = Logger.getLogger(Thread.currentThread().getName() + ":" + ServerIO.class.getName());

    public ServerIO(Socket clientSocket) {
        try {
            dos = new DataOutputStream(clientSocket.getOutputStream());
            dis = new DataInputStream(clientSocket.getInputStream());
            LOGGER.fine("Output and Input streams is open");
        } catch (IOException e) {
            LOGGER.warning("Can't open streams\n" + LogFormatter.formatStackTrace(e));
        }
    }

    boolean isTry = false;
    private UserKey registerKey() throws ServerResponseException {
        write("REGISTER NEW KEY");
        UserKey publicKey = RSAClientKeys.getPublicKey();
        String[] message = new String[]{publicKey.getKey()[0].toString(), publicKey.getKey()[1].toString(), RSA.encodeString(publicKey.getUser().getId(), serverKey)};
        write(message);
        List<String> response = read();
        if (response.getFirst().startsWith("KEY IS REJECTED") && !isTry) {
            isTry = true;
            write("RESET KEY");
            String login = RSA.encodeString(publicKey.getUser().getId()+"\n"+RSAClientKeys.getPassword()+"\n", serverKey);
            write(login);
            String status = readFirst();
            if (status.equals("OK"))
                return registerKey();
            else if (status.equals("AUTHENTICATION ERROR")) {
                close();
            }
        }
        else if (response.getFirst().equals("META")) {
            response = read();
            if (response.size() == 2) {
                UserKey key = RSAClientKeys.getPublicKey();
                key.setMeta(new BigInteger[]{new BigInteger(response.getFirst()), new BigInteger(response.get(1))});
                return key;
            }
            else {
                LOGGER.warning("Wrong meta format");
            }
        }
        return null;
    }

    private void requestServerKey() throws WrongKeyException, ServerResponseException {
        write("GET RSA KEY");
        List<String> keyString = read();
        if (keyString.size() >= 5) {
            serverKey = new UserKey(new BigInteger[]{
                    new BigInteger(keyString.getFirst()),
                    new BigInteger(keyString.get(1))
            }, new User(keyString.get(4)));
            serverKey.setMeta(new BigInteger[]{
                    new BigInteger(keyString.get(2)),
                    new BigInteger(keyString.get(3))
            });
            if (!serverKey.getUser().getId().equals("#SERVER#") || !RSAKeys.isKey(serverKey)) {
                throw new WrongKeyException("Wrong server key");
            }
        } else {
            LOGGER.warning("Wrong key format");
            throw new WrongKeyException("Wrong key format");
        }
    }

    public void init() throws WrongKeyException, ServerResponseException {
        if (serverKey == null){
            requestServerKey();
        }
        if (RSAClientKeys.isNeedToRegister()) {
            RSAClientKeys.register(registerKey());
        }
        write("KEY");
        UserKey clientKey = RSAClientKeys.getPublicKey();
        String[] key = new String[]{clientKey.getKey()[0].toString(), clientKey.getKey()[1].toString(),
                clientKey.getMeta()[0].toString(), clientKey.getMeta()[1].toString(), RSA.encodeString(clientKey.getUser().getId(), serverKey)};
        write(key);
        if (checkInitialization()) {
            initialized = true;
            String login = RSAClientKeys.getUserId()+"\n"+RSAClientKeys.getPassword()+"\n";
            write(login);
            String status = readFirst();
            if (status.equals("AUTHENTICATION ERROR")) {
                LOGGER.warning("AUTHENTICATION ERROR");
                ResourcesIO.delete("RSA/userKeys" + ResourcesIO.EXTENSION);
                close();
                return;
            }
            LOGGER.info("Handshake complete");
        }
        else {
            LOGGER.warning("Fail initialization");
            close();
            //todo
        }
    }

    private boolean checkInitialization() throws ServerResponseException{
        String response = readFirst();
        if (response.equals("Wrong key")) {
            ResourcesIO.delete("RSA/userKeys.json");
        }
        if (RSA.decodeString(response, RSAClientKeys.getPrivateKey()).equals("INITIALIZED")) {
            write(RSA.encodeString("INITIALIZED", serverKey));
            return RSA.decodeString(readFirst(), RSAClientKeys.getPrivateKey()).equals("OK");
        }
        return false;
    }

    public boolean isClosed() {
        return isClosed;
    }

    public Key getServerKey() {
        return serverKey;
    }

    public String readFirst() throws ServerResponseException{
        List<String> list = read();
        if (!list.isEmpty()) {
            return list.getFirst();
        }
        return "";
    }

    public synchronized List<String> read() throws ServerResponseException{
        List<String> lines = new ArrayList<>();
        try {
            String inputString = dis.readUTF();
            if (initialized) {
                inputString = RSA.decodeString(inputString, RSAClientKeys.getPrivateKey());
            }
            lines = new ArrayList<>(List.of(inputString.split("\n")));
            StringBuilder stringBuilder = new StringBuilder();
            for (String line : lines)
                stringBuilder.append(line).append("\n");
            LOGGER.finer("Get message: " + stringBuilder);
        } catch (IOException e) {
            LOGGER.warning("Can't read lines\n" + LogFormatter.formatStackTrace(e));
        }
        if (lines.isEmpty() || lines.getFirst().equals("500 ERROR")) {
            close();
            throw new ServerResponseException("Server response error");
        }
        return lines;
    }

    public synchronized void write(String message) {
        if (message == null) {
            LOGGER.warning("Null message");
            return;
        }
        if (!isClosed){
            try {
                if (initialized)
                    message = RSA.encodeString(message, serverKey);
                dos.writeUTF(message);
                dos.flush();
                if (SimpleInterface.isKeepAlive() || Messenger.getChecking()) Main.simpleInterface.updateTimer();
                LOGGER.finer("Message sent:\n" + message);
            } catch (IOException e) {
                LOGGER.warning("Can't send the message\n" + LogFormatter.formatStackTrace(e));
            }
        }
        else {
            close();
        }
    }

    public synchronized void write(String[] lines) {
        if (lines == null || lines.length == 0) {
            LOGGER.warning("Null message");
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (String line : lines)
            stringBuilder.append(line).append("\n");
        String message = stringBuilder.toString();
        LOGGER.finer("Sending message:\n" + message);
        try {
            if (initialized) message = RSA.encodeString(message, serverKey);
            dos.writeUTF(message);
            dos.flush();
            if (SimpleInterface.isKeepAlive() || Messenger.getChecking()) Main.simpleInterface.updateTimer();
        } catch (IOException e) {
            LOGGER.warning("Can't send the message\n" + LogFormatter.formatStackTrace(e));
        }
    }

    public void close() {
        if (initialized && !isClosed) write("EXIT");
        isClosed = true;
        SimpleInterface.disableKeepAlive();
        try {
            dis.close();
            dos.close();
        } catch (IOException e) {
            LOGGER.warning("Can't close streams\n" + LogFormatter.formatStackTrace(e));
        }
    }
}
