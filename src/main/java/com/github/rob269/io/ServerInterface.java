package com.github.rob269.io;

import com.github.rob269.User;
import com.github.rob269.rsa.Key;
import com.github.rob269.rsa.RSA;
import com.github.rob269.rsa.RSAKeys;
import com.github.rob269.rsa.RSAClientKeys;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.net.Socket;
import java.util.*;
import java.util.logging.Logger;

public class ServerInterface implements AutoCloseable{
    private OutputStreamWriter osw;
    private Scanner scanner;
    private Key serverKey;

    private static final Logger LOGGER = Logger.getLogger(Thread.currentThread().getName() + ":" + ServerInterface.class.getName());

    public ServerInterface(Socket clientSocket) {
        try {
            osw = new OutputStreamWriter(clientSocket.getOutputStream());
            scanner = new Scanner(clientSocket.getInputStream());
            LOGGER.fine("Output and Input streams is open");
        } catch (IOException e) {
            LOGGER.warning("Can't open streams");
            e.printStackTrace();
        }
    }

    public void init() {
        write("GET RSA KEY");
        List<String> keyString = read();
        serverKey = new Key(new BigInteger[]{
                new BigInteger(keyString.getFirst()),
                new BigInteger(keyString.get(1))
        }, new User(keyString.getLast()));
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
        }
        else {
            LOGGER.warning("Wrong server key");
        }
    }

    public List<String> read() {
        List<String> lines = new ArrayList<>();
        while (scanner.hasNext()) {
            lines.add(scanner.nextLine());
        }
        return lines;
    }

    public void write(String message) {
        if (message == null) {
            LOGGER.warning("Null message");
            return;
        }
        try {
            osw.write(message);
            osw.flush();
            LOGGER.fine("Message sent");
        } catch (IOException e) {
            LOGGER.warning("Can't write the message by outputStreamWriter");
            e.printStackTrace();
        }
    }

    public void write(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            LOGGER.warning("Null message");
            return;
        }
        for (int i = 0; i < lines.size(); i++) {
            try {
                osw.write(lines.get(i) + "\n");
            } catch (IOException e) {
                LOGGER.warning("Can't send message");
                e.printStackTrace();
            }
        }
        try {
            osw.flush();
        } catch (IOException e) {
            LOGGER.warning("Can't flush message");
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            scanner.close();
            osw.close();
        } catch (IOException e) {
            LOGGER.warning("Can't close streams");
            e.printStackTrace();
        }
    }
}
