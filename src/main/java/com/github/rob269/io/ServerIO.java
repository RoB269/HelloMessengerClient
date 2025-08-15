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
import java.util.logging.Logger;

public class ServerIO implements Closeable {
    private Socket serverSocket;
    private DataOutputStream dos;
    private DataInputStream dis;
    private static Key serverKey;
    private boolean isClosed = false;
    private boolean initialized = false;
    private InputRouter router;
    private static final String serverName = "#SERVER#";

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

    private void requestServerKey() throws WrongKeyException, IOException {
        writeCommand(10);
        byte status = readCommand();
        if (status == 52){
            BigInteger[] key = new BigInteger[]{readBigint(), readBigint()};
            BigInteger[] meta = new BigInteger[]{readBigint(), readBigint()};
            String user = readString();
            serverKey = new Key(key);

            BigInteger one = (RSA.decode(meta[0], Guarantor.getPublicKey()).subtract(key[0]));
            BigInteger two = (RSA.decode(meta[1], Guarantor.getPublicKey())).subtract(key[1]);
            boolean isServerKey = one.compareTo(two) == 0 && one.compareTo(BigInteger.valueOf(user.hashCode())) == 0 && user.equals(serverName);
            if (!isServerKey) {
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

    public void init() throws WrongKeyException, InitializationException, IOException {
        if (serverKey == null) requestServerKey();
        writeCommand(20);
        writePackageCount(2);
        Key clientKey = Client.getPublicKey();
        for (int i = 0; i < 2; i++) write(clientKey.getKey()[i]);
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
            write(Client.getUserId());
            write(Client.getPassword(), false);
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

    private boolean checkInitialization() throws IOException {
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

    public String readString() throws IOException {
        String string = new String(read());
        LOGGER.finer("Get message:\n" + string);
        return string;
    }

    public byte readCommand() throws IOException {
        byte command = read()[0];
        LOGGER.finer("Get command:\n" + command);
        return command;
    }

    public BigInteger readBigint() throws IOException {
        BigInteger bigint = new BigInteger(read());
        LOGGER.finer("Get bigint:\n" + bigint);
        return bigint;
    }

    public byte[] read() throws IOException{
        byte[] result;
        LOGGER.finest("Reading bytes");
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
        if (isClosed) throw new IOException();
        return result;
    }

    public void write(String message) throws IOException {
        write(message, true);
    }

    public void write(String message, boolean log) throws IOException {
        if (!isClosed){
            write(message.getBytes());
            if (log) LOGGER.finer("Message sent:\n" + message);
        }
    }


    public void write(BigInteger message) throws IOException {
        LOGGER.finer("Write bigint:\n" + message);
        write(message.toByteArray());
    }

    public void writeCommand(int message) throws IOException {
        LOGGER.finer("Write command: " + message);
        write(new byte[]{(byte) message}, false);
    }

    public void writePackageCount(int count) {
        if (!isClosed) {
            try {
                LOGGER.severe("Sending package count"); //todo delete
                dos.writeInt(count);
                dos.flush();
            } catch (IOException e) {
                LOGGER.warning("Can't send the message\n" + LogFormatter.formatStackTrace(e));
            }
        }
    }

    public void write(byte[] message) throws IOException {
        write(message, true);
    }

    public void write(byte[] message, boolean sendPackageSize) throws IOException{
        if (!isClosed) {
            LOGGER.finest("Sending byte message");
            try {
                if (initialized) message = RSA.encodeByteArray(message, serverKey);
                if (sendPackageSize) {
                    dos.writeInt(message.length);
                }
                dos.write(message);
                dos.flush();
            } catch (IOException e) {
                LOGGER.warning("Can't send the message\n" + LogFormatter.formatStackTrace(e));
            }
            return;
        }
        throw new IOException();
    }

    public void close() throws IOException {
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
