package com.github.rob269.io;

import com.github.rob269.LogFormatter;
import com.github.rob269.rsa.RSA;
import com.github.rob269.rsa.RSAClientKeys;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

public class InputRouter extends Thread {
    private static final Logger LOGGER = Logger.getLogger(InputRouter.class.getName());
    private DataInputStream dis;
    private boolean isClosed = false;
    public final Deque<byte[]> mainThreadInput = new ArrayDeque<>();
    public final Deque<byte[]> sideThreadInput = new ArrayDeque<>();
    private final ServerIO serverIO;

    public InputRouter(DataInputStream dis, ServerIO serverIO) {
        this.dis = dis;
        this.serverIO = serverIO;
    }

    public void close() {
        isClosed = true;
        interrupt();
    }

    private static final List<Integer> commandsWithContinue = new ArrayList<>(List.of(new Integer[]{52, 51}));
    @Override
    public void run() {
        try {
            while (!isClosed) {
                byte[] input = new byte[129];
                int inputSize = dis.read(input);
                int command;
                if (inputSize == -1) {
                    break;
                } else if (inputSize == 129) {
                    command = RSA.decodeByteArray(input, RSAClientKeys.getPrivateKey())[0];
                } else {
                    command = input[0];
                }
                if (command >= 0) {
                    mainThreadInput.add(new byte[]{(byte) command});
                    if (commandsWithContinue.contains(command)) {
                        int packages = dis.readInt();
                        for (int i = 0; i < packages; i++) {
                            int byteLength = dis.readInt();
                            byte[] bytes = new byte[byteLength];
                            dis.read(bytes);
                            if (serverIO.isInitialized()) bytes = RSA.decodeByteArray(bytes, RSAClientKeys.getPrivateKey());
                            mainThreadInput.add(bytes);
                        }
                    }
                    synchronized (mainThreadInput) {
                        mainThreadInput.notify();
                    }
                }
            }
        }  catch (IOException ignored) {
            LOGGER.info("Input router timeout");
        }
        serverIO.close();
        synchronized (mainThreadInput) {
            mainThreadInput.notify();
        }
//        synchronized (sideThreadInput) {
//            sideThreadInput.notify();
//        }
        LOGGER.info("Input router closed");
    }
}