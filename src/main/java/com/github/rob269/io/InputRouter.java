package com.github.rob269.io;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;

public class InputRouter extends Thread {
    private DataInputStream dis;
    private boolean isClosed = false;
    public final Deque<byte[]> mainThreadInput = new ArrayDeque<>();
    public final Deque<byte[]> sideThreadInput = new ArrayDeque<>();

    public InputRouter(DataInputStream dis) {
        this.dis = dis;
    }

    public void close() {
        isClosed = true;
        interrupt();
    }

    @Override
    public void run() {
        while (!isClosed) {
            try {
                int command = dis.readByte();
                if (command>=0) {
                    mainThreadInput.add(new byte[]{(byte) command});
                    if (command/10%10 == 2) {
                        int packages = dis.readInt();
                        for (int i = 0; i < packages; i++) {
                            int byteLength = dis.readInt();
                            byte[] bytes = new byte[byteLength];
                            dis.read(bytes);
                            mainThreadInput.add(bytes);
                        }
                    }
                    mainThreadInput.notify();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}