package com.github.rob269.io;

import com.github.rob269.User;
import com.github.rob269.rsa.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.Socket;
import java.util.*;
import java.util.logging.Logger;

public class ServerInterface implements AutoCloseable {
    private DataOutputStream dos;
    private DataInputStream dis;
    private Key serverKey;
    private boolean isClosed = false;
    private boolean initialized = false;

    private static final Logger LOGGER = Logger.getLogger(Thread.currentThread().getName() + ":" + ServerInterface.class.getName());

    public ServerInterface(Socket clientSocket) {
        try {
            dos = new DataOutputStream(clientSocket.getOutputStream());
            dis = new DataInputStream(clientSocket.getInputStream());
            LOGGER.fine("Output and Input streams is open");
        } catch (IOException e) {
            LOGGER.warning("Can't open streams");
            e.printStackTrace();
        }
    }

    public void init() throws WrongKeyException {
        write("GET RSA KEY");
        List<String> keyString = read();

        if (keyString.size() >= 5) {
            serverKey = new Key(new BigInteger[]{
                    new BigInteger(keyString.getFirst()),
                    new BigInteger(keyString.get(1))
            }, new User(keyString.get(4)));
            serverKey.setMeta(new BigInteger[]{
                    new BigInteger(keyString.get(2)),
                    new BigInteger(keyString.get(3))
            });
            if (RSAKeys.isKey(serverKey)) {
                write("KEY");
                Key clientKey = RSAClientKeys.getPublicKey();
                List<String> key = new ArrayList<>(List.of(clientKey.getKey()[0].toString(), clientKey.getKey()[1].toString(),
                        clientKey.getMeta()[0].toString(), clientKey.getMeta()[1].toString(), RSA.encodeString(clientKey.getUser().getId(), serverKey)));
                write(key);
                if (checkInitialization()) {
                    initialized = true;
                    LOGGER.info("YEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE");
                    close();//todo
                }
                else {
                    LOGGER.warning("Fail initialization");
                    close();
                    //todo
                }
            } else {
                throw new WrongKeyException("Wrong server key");
            }
        }
        else {
            LOGGER.warning("Wrong key format");
        }
    }

    private boolean checkInitialization() {
        if (RSA.decodeString(readFirst(), RSAClientKeys.getPrivateKey()).equals("INITIALIZED")) {
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

    public String readFirst(){
        List<String> list = read();
        if (!list.isEmpty()) {
            return list.getFirst();
        }
        return "";
    }

    public List<String> read() {
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
            LOGGER.warning("Can't read lines");
            e.printStackTrace();
        }
        return lines;
    }

    public void write(String message) {
        if (message == null) {
            LOGGER.warning("Null message");
            return;
        }
        try {
            if (initialized)
                message = RSA.encodeString(message, serverKey);
            dos.writeUTF(message);
            dos.flush();
            LOGGER.finer("Message sent:\n" + message);
        } catch (IOException e) {
            LOGGER.warning("Can't send the message");
            e.printStackTrace();
        }
    }

    public void write(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
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
        } catch (IOException e) {
            LOGGER.warning("Can't send the message");
            e.printStackTrace();
        }
    }

    public void close() {
        isClosed = true;
        try {
            dis.close();
            dos.close();
        } catch (IOException e) {
            LOGGER.warning("Can't close streams");
            e.printStackTrace();
        }
    }
}
