package com.github.rob269.helloMessengerClient.io;

import com.github.rob269.helloMessengerClient.Client;
import com.github.rob269.helloMessengerClient.LogFormatter;
import com.github.rob269.helloMessengerClient.Main;
import com.github.rob269.helloMessengerClient.rsa.RSA;

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

    @Override
    public void run() {
        try {
            while (!isInterrupted()) {
                int inputSize = dis.readByte() & 0xff;
                byte[] input = new byte[inputSize];
                dis.readFully(input);
                if (inputSize != 130 && (serverIO.isInitialized() || input[0] == 30)) {
                    break;
                } else if (inputSize == 130) {
                    input = RSA.decodeByteArray(input, Client.getPrivateKey());
                }
                int command = input[0];

                if (command >= 0) {
                    mainThreadInput.add(new byte[]{(byte) command});
                    readPackage(input, mainThreadInput);
                    synchronized (mainThreadInput) {
                        mainThreadInput.notify();
                    }
                }
                else {
                    sideThreadInput.add(new byte[]{(byte) command});
                    readPackage(input, sideThreadInput);
                    synchronized (sideThreadInput) {
                        sideThreadInput.notify();
                    }
                }
            }
        } catch (IOException _) {
            serverIO.close();
            Main.controller.printErrorMessage("Disconnected from the server");
        }

        synchronized (mainThreadInput) {
            mainThreadInput.notify();
        }
        synchronized (sideThreadInput) {
            sideThreadInput.notify();
        }
        LOGGER.info("Input router closed");
    }

    private void readPackage(byte[] input, Deque<byte[]> deque) throws IOException{
        if (input.length > 1) {
            for (int i = 0, packages = byteArrayToInt(input); i < packages; i++) {
                int byteLength = dis.readInt();
                byte[] bytes = new byte[byteLength];
                dis.readFully(bytes);
                if (serverIO.isInitialized()) bytes = RSA.decodeByteArray(bytes, Client.getPrivateKey());
                deque.add(bytes);
            }
        }
    }

    private static int byteArrayToInt(byte[] bytes) {
        int integer = 0;
        for (int i = 1; i < bytes.length; i++) {
            integer <<= 8;
            integer |=  bytes[i] & 0xff;
        }
        return integer;
    }
}