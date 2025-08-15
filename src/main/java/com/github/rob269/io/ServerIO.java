package com.github.rob269.io;

import com.github.rob269.*;
import com.github.rob269.rsa.*;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerIO implements Closeable {
    private Socket serverSocket;
    private DataOutputStream dos;
    private DataInputStream dis;
    private static UserKey serverKey;
    private boolean isClosed = false;
    private boolean initialized = false;
    private InputRouter router;

    private static final Logger LOGGER = Logger.getLogger(Thread.currentThread().getName() + ":" + ServerIO.class.getName());

    public ServerIO(Socket serverSocket) {
        try {
            this.serverSocket = serverSocket;
            dos = new DataOutputStream(serverSocket.getOutputStream());
            dis = new DataInputStream(serverSocket.getInputStream());
            router = new InputRouter(dis, this);
            router.start();
            LOGGER.fine("Output and Input streams is open");
        } catch (IOException e) {
            LOGGER.warning("Can't open streams\n" + LogFormatter.formatStackTrace(e));
        }
    }

    boolean isTry = false;
    private UserKey registerKeyRequest() {
        writeCommand(21);
        UserKey publicKey = RSAClientKeys.getPublicKey();
        writePackageCount(3);
        for (int i = 0; i < 2; i++) write(publicKey.getKey()[i]);
        write(RSA.encodeStringToByte(publicKey.getUser().getId(), serverKey));
        byte response = readCommand();
        if (response == 61 && !isTry) {
            isTry = true;
            writeCommand(22);
            writePackageCount(2);
            write(RSA.encodeStringToByte(publicKey.getUser().getId(), serverKey));
            write(RSA.encodeStringToByte(RSAClientKeys.getPassword(), serverKey));
            byte status = readCommand();
            if (status == 50)
                return registerKeyRequest();
            else if (status == 62) {
                close();//todo
            }
        }
        else if (response == 51) {
            BigInteger[] meta = new BigInteger[2];
            for (int i = 0; i < 2; i++) meta[i] = readBigint();
            UserKey key = RSAClientKeys.getPublicKey();
            key.setMeta(meta);
            return key;
        }
        return null;
    }

    private void requestServerKey() throws WrongKeyException {
        writeCommand(10);
        byte status = readCommand();
        if (status == 52){
            BigInteger[] key = new BigInteger[]{readBigint(), readBigint()};
            BigInteger[] meta = new BigInteger[]{readBigint(), readBigint()};
            serverKey = new UserKey(key, new User(readString()));
            serverKey.setMeta(meta);
            if (!serverKey.getUser().getId().equals("#SERVER#") || !RSAKeysPair.isKey(serverKey)) {
                throw new WrongKeyException("Wrong server key");
            }
        }
        else {
            throw new RuntimeException();
        }
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void init() throws WrongKeyException, InitializationException {
        if (serverKey == null) requestServerKey();
        if (RSAClientKeys.isNeedToRegister()) RSAClientKeys.register(registerKeyRequest());
        writeCommand(20);
        writePackageCount(5);
        UserKey clientKey = RSAClientKeys.getPublicKey();
        for (int i = 0; i < 2; i++) write(clientKey.getKey()[i]);
        for (int i = 0; i < 2; i++) write(clientKey.getMeta()[i]);
        write(RSA.encodeStringToByte(clientKey.getUser().getId(), serverKey));
        if (checkInitialization()) {
            initialized = true;
            try {
                serverSocket.setSoTimeout(0);
                serverSocket.setKeepAlive(true);
            } catch (SocketException e) {
                LOGGER.warning("Socket exception\n" + LogFormatter.formatStackTrace(e));
            }
            writeCommand(23);
            writePackageCount(2);
            write(RSAClientKeys.getUserId());
            write(RSAClientKeys.getPassword(), false);
            byte status = readCommand();
            if (status == 62) {
                LOGGER.warning("AUTHENTICATION ERROR");
                ResourcesIO.delete("RSA/userKeys" + ResourcesIO.EXTENSION);
                close();
                return;
            }
            LOGGER.info("Handshake complete");
        }
        else {
            close();
            throw new InitializationException("Fail initialization");
        }
    }

    private boolean checkInitialization() {
        byte response = readCommand();
        if (response == 63) {
            ResourcesIO.delete("RSA/userKeys" + ResourcesIO.EXTENSION);
        }
        else if (response == 30) {
            write(RSA.encodeByteArray(new byte[]{30}, serverKey), false);
            response = readCommand();
            return response == 50;
        }

        return false;
    }

    public boolean isClosed() {
        return isClosed;
    }

    public Key getServerKey() {
        return serverKey;
    }

    public String readString() {
        String string = new String(read());
        LOGGER.finer("Get message:\n" + string);
        return string;
    }

    public byte readCommand() {
        byte command = read()[0];
        LOGGER.finer("Get command:\n" + command);
        return command;
    }

    public BigInteger readBigint() {
        BigInteger bigint = new BigInteger(read());
        LOGGER.finer("Get message:\n" + bigint);
        return bigint;
    }

    public byte[] read() {
        byte[] result;
        if (Thread.currentThread().getName().startsWith("Main")) {
            synchronized (router.mainThreadInput) {
                while (router.mainThreadInput.isEmpty() && !isClosed) {
                    try {
                        router.mainThreadInput.wait();
                    } catch (InterruptedException e) {
                        LOGGER.warning("Threads exception\n" + LogFormatter.formatStackTrace(e));
                    }
                }
                result = router.mainThreadInput.poll();
            }
        }
        else {
            synchronized (router.sideThreadInput) {
                while (router.sideThreadInput.isEmpty() && !isClosed) {
                    try {
                        router.sideThreadInput.wait();
                    } catch (InterruptedException e) {
                        LOGGER.warning("Threads exception\n" + LogFormatter.formatStackTrace(e));
                    }
                }
                result = router.sideThreadInput.poll();
            }
        }
        return result;
    }

    public void write(String message) {
        write(message, true);
    }

    public void write(String message, boolean log) {
        if (!isClosed){
            write(message.getBytes());
            if (log) LOGGER.finer("Message sent:\n" + message);
        }
    }


    public void write(BigInteger message) {
        LOGGER.finer("Write bigint:\n" + message);
        write(message.toByteArray());
    }

    public void writeCommand(int message) {
        LOGGER.finer("Write command: " + message);
        write(new byte[]{(byte) message}, false);
    }

    public void writePackageCount(int count) {
        if (!isClosed) {
            try {
                dos.writeInt(count);
                dos.flush();
            } catch (IOException e) {
                LOGGER.warning("Can't send the message\n" + LogFormatter.formatStackTrace(e));
            }
        }
    }

    public void write(byte[] message) {
        write(message, true);
    }

    public void write(byte[] message, boolean sendPackageSize) {
        if (!isClosed) {
            LOGGER.finest("Sending byte message");
            try {
                if (initialized) message = RSA.encodeByteArray(message, serverKey);
                if (sendPackageSize) dos.writeInt(message.length);
                dos.write(message);
                dos.flush();
            } catch (IOException e) {
                LOGGER.warning("Can't send the message\n" + LogFormatter.formatStackTrace(e));
            }
        }
    }

    public void close() {
        if (initialized && !isClosed) writeCommand(99);
        isClosed = true;
        try {
            dis.close();
            dos.close();
            router.close();
        } catch (IOException e) {
            LOGGER.warning("Can't close streams\n" + LogFormatter.formatStackTrace(e));
        }
    }
}
