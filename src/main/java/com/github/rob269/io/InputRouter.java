package com.github.rob269.io;

import com.github.rob269.Client;
import com.github.rob269.rsa.RSA;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

public class InputRouter extends Thread {
    private static final Logger LOGGER = Logger.getLogger(InputRouter.class.getName());
    private final DataInputStream dis;
    public final Deque<byte[]> mainThreadInput = new ArrayDeque<>();
    public final Deque<byte[]> sideThreadInput = new ArrayDeque<>();
    private final ServerIO serverIO;

    public InputRouter(DataInputStream dis, ServerIO serverIO) {
        this.dis = dis;
        this.serverIO = serverIO;
    }

    public void close() {
        interrupt();
    }

    private static final List<Integer> commandsWithContinue = new ArrayList<>(List.of(new Integer[]{52, 51}));
    @Override
    public void run() {
        try {
            while (!isInterrupted()) {
                byte[] input = new byte[130];
                int inputSize = dis.read(input);
                int command;
                if (inputSize == -1) {
                    break;
                } else if (inputSize == 130) {
                    command = RSA.decodeByteArray(input, Client.getPrivateKey())[0];
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
                            if (serverIO.isInitialized()) bytes = RSA.decodeByteArray(bytes, Client.getPrivateKey());
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