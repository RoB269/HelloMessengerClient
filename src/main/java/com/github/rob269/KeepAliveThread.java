package com.github.rob269;

import com.github.rob269.SimpleInterface;
import com.github.rob269.io.ServerIO;
import com.github.rob269.io.ServerResponseException;

import java.util.logging.Logger;

public class KeepAliveThread extends Thread {
    private static final Logger LOGGER = Logger.getLogger(KeepAliveThread.class.getName());
    private long time;

    private ServerIO serverIO;
    public KeepAliveThread(ServerIO serverIO) {
        this.serverIO = serverIO;
    }

    public void updateTimer() {
        time = System.currentTimeMillis() + 5000;
    }

    @Override
    public void run() {
        updateTimer();
        while (SimpleInterface.isKeepAlive()) {
            if (System.currentTimeMillis() >= time && !serverIO.isClosed()) {
                serverIO.write("KEEP ALIVE");
                try {
                    if (!serverIO.readFirst().equals("OK")) {
                        break;
                    }
                } catch (ServerResponseException e) {
                    LOGGER.warning("Server response exception");
                    e.printStackTrace();
                    break;
                }
            }
        }
    }
}
