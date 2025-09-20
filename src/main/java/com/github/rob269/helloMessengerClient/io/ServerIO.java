package com.github.rob269.helloMessengerClient.io;

import com.github.rob269.helloMessengerClient.AuthenticationException;
import com.github.rob269.helloMessengerClient.Client;
import com.github.rob269.helloMessengerClient.InitializationException;
import com.github.rob269.helloMessengerClient.LogFormatter;
import com.github.rob269.helloMessengerClient.rsa.Guarantor;
import com.github.rob269.helloMessengerClient.rsa.Key;
import com.github.rob269.helloMessengerClient.rsa.RSA;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class ServerIO implements Closeable {
    private static final Map<Byte, String> commands = new HashMap<>();
    static {
        commands.put((byte) -10, "Sending new message");
        commands.put((byte) 0, "Exit");
        commands.put((byte) 99, "Exit");
        commands.put((byte) 10, "Get Server public key");
        commands.put((byte) 20, "User sending his public key");
        commands.put((byte) 21, "Login");
        commands.put((byte) 30, "Check initialization");
        commands.put((byte) 50, "OK");
        commands.put((byte) 51, "Sending server public key");
        commands.put((byte) 52, "User successfully authenticated");
        commands.put((byte) 53, "Sending chats");
        commands.put((byte) 54, "Chat was created");
        commands.put((byte) 55, "Message is successfully sent");
        commands.put((byte) 56, "Sending messages from database");
        commands.put((byte) 57, "You joined the chat");
        commands.put((byte) 60, "Error");
        commands.put((byte) 61, "Authentication error");
        commands.put((byte) 62, "Chat already exist");
        commands.put((byte) 63, "Wrong chat name");
        commands.put((byte) 64, "The user does not exist");
        commands.put((byte) 65, "Forbidden symbol");
        commands.put((byte) 66, "Message too long");
        commands.put((byte) 67, "Chat doesn't exist");
        commands.put((byte) 68, "User is not in the chat");
        commands.put((byte) 69, "Chat is blocked");
        commands.put((byte) 70, "User blocked in this chat");
        commands.put((byte) 71, "Wrong params");
        commands.put((byte) 72, "User already in the chat");
        commands.put((byte) 80, "Get chats");
        commands.put((byte) 81, "Create new private chat");
        commands.put((byte) 82, "Create new public chat");
        commands.put((byte) 83, "Send message");
        commands.put((byte) 84, "Get messages");
        commands.put((byte) 85, "Add user to the chat");
        commands.put((byte) 86, "Join the chat");
        commands.put((byte) 90, "Ping");
    }
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
            router.setName("InputRouterThread");
            router.start();
            LOGGER.fine("Output and Input streams is open");
        } catch (IOException e) {
            LOGGER.warning("Can't open streams\n" + LogFormatter.formatStackTrace(e));
        }
    }

    private void requestServerKey() throws WrongKeyException, IOException {
        writeCommand(10);
        byte status = readCommand();
        if (status == 51){
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

    public void init() throws WrongKeyException, InitializationException, IOException, AuthenticationException {
        if (serverKey == null) requestServerKey();
        HMPBatch batch = writeBatch(20, 2, false);
        Key clientKey = Client.getPublicKey();
        for (int i = 0; i < 2; i++) batch.write(clientKey.getKey()[i]);
        if (checkInitialization()) {
            initialized = true;
            try {
                serverSocket.setSoTimeout(0);
                serverSocket.setKeepAlive(true);
            } catch (SocketException e) {
                LOGGER.warning("Socket exception\n" + LogFormatter.formatStackTrace(e));
            }
            batch = writeBatch(21, 2, false);
            batch.write(Client.getUsername());
            batch.write(Client.getPassword());
            byte status = readCommand();
            if (status == 61) {
                throw new AuthenticationException();
            }
            else if (status == 52) LOGGER.info("Handshake complete");
            else if (status == 65){
                LOGGER.warning("Username contains forbidden symbols");
                close();
            }
            else close();
        }
        else {
            close();
            throw new InitializationException("Fail initialization");
        }
    }

    private boolean checkInitialization() throws IOException {
        byte response = readCommand();
        if (response == 30) {
            write(RSA.encodeByteArray(new byte[]{30}, serverKey), true);
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

    public int ping() {
        long start = System.currentTimeMillis();
        try {
            writeCommand(90);
            byte response = readCommand();
            if (response == 90) return (int) (System.currentTimeMillis() - start);
        } catch (IOException e) {
            LOGGER.warning("Connection exception\n" + LogFormatter.formatStackTrace(e));
        }
        return -1;
    }

    public long readLong() throws IOException {
        byte[] bytes = read();
        long val = 0;
        for (byte b : bytes) {
            val = val << 8;
            val = (val | (b & 0xff));
        }
        return val;
    }

    public String readString() throws IOException {
        return readString(true);
    }

    public String readString(boolean log) throws IOException {
        String string = new String(read());
        if (log) LOGGER.finer("Get message:\n" + string);
        return string;
    }

    public byte readCommand() throws IOException {
        byte command = read()[0];
        LOGGER.finer("Get command: " + command + " [R:" + commands.get(command) + "]");
        return command;
    }

    public BigInteger readBigint() throws IOException {
        BigInteger bigint = new BigInteger(read());
        LOGGER.finer("Get bigint:\n" + bigint);
        return bigint;
    }

    public byte[] read() throws IOException{
        byte[] result = null;
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
        else if (Thread.currentThread().getName().startsWith("Side")){
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

    public HMPBatch writeBatch(int command, int batchSize, boolean log) {
        return new HMPBatch((byte) command, this, batchSize, log);
    }

    public void writeCommand(int message) throws IOException{
        writeCommand(message, 0);
    }

    public void writeCommand(int message, int packageCount) throws IOException {
        if (packageCount == 0) write(new byte[]{(byte) message}, true);
        else {
            byte[] command = intToByteArray(packageCount);
            command[0] = (byte) message;
            write(command, true);
        }
        LOGGER.finer("Write command: " + message + " [W:" + commands.get((byte) message) + "]");
    }

    private static byte[] intToByteArray(int integer) {
        byte[] bytes = new byte[4];
        int i;
        for (i = 3; i >= 0 && integer != 0; i--) {
            bytes[i] = (byte) integer;
            integer >>>= 8;
        }
        byte[] toReturn = new byte[4-i];
        System.arraycopy(bytes, i+1, toReturn, 1, toReturn.length-1);
        return toReturn;
    }

    synchronized void write(byte[] message, boolean sendCommand) throws IOException{
        if (!isClosed) {
            LOGGER.finest("Sending byte message");
            try {
                if (initialized) message = RSA.encodeByteArray(message, serverKey);

                if (sendCommand) dos.writeByte(message.length);
                else dos.writeInt(message.length);

                dos.write(message);
                dos.flush();
            } catch (IOException e) {
                LOGGER.warning("Can't send the message");
                throw e;
            }
            return;
        }
        throw new IOException();
    }

    public synchronized void close() {
        if (!isClosed) {
            try {
                if (!serverSocket.isClosed()) writeCommand(initialized ? 99 : 0);
            } catch (IOException _) {}
            isClosed = true;
            try {
                dis.close();
                dos.close();
                router.close();
                serverSocket.close();
            } catch (IOException e) {
                LOGGER.warning("Can't close streams\n" + LogFormatter.formatStackTrace(e));
            }
        }
    }
}
