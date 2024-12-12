package com.github.rob269;

import com.github.rob269.io.ServerIO;
import com.github.rob269.io.ServerResponseException;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CheckingThread extends KeepAliveThread {
    private static final Logger LOGGER = Logger.getLogger(CheckingThread.class.getName());

    public CheckingThread(ServerIO serverIO, int timeRange) {
        super(serverIO, timeRange);
    }

    @Override
    public void run() {
        while (Messenger.getChecking() && !isInterrupted()) {
            if (System.currentTimeMillis() >= time && !serverIO.isClosed()) {
                updateTimer();
                serverIO.write("CHECK", Level.ALL);
                try {
                    if (serverIO.readFirst().equals("CHECK YES")) {
                        Main.simpleInterface.updateMessages();
                    }
                } catch (ServerResponseException e) {
                    LOGGER.warning("Server response exception\n" + LogFormatter.formatStackTrace(e));
                }
            }
            else if (serverIO.isClosed()) {
                break;
            }
        }
    }
}
