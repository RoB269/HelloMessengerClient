package com.github.rob269;

import com.github.rob269.io.ServerIO;
import com.github.rob269.io.ServerResponseException;

import java.util.logging.Logger;

public class KeepAliveThread extends Thread {
    private static final Logger LOGGER = Logger.getLogger(KeepAliveThread.class.getName());
    protected int timeRange;
    protected long time;

    protected ServerIO serverIO;
    public KeepAliveThread(ServerIO serverIO, int timeRange) {
        this.serverIO = serverIO;
        this.timeRange = timeRange;
    }

    public void updateTimer() {
        time = System.currentTimeMillis() + timeRange;
    }

    @Override
    public void run() {
        updateTimer();
        while (SimpleInterface.isKeepAlive()) {
            if (System.currentTimeMillis() >= time && !serverIO.isClosed()) {
                serverIO.write("KEEP ALIVE");
                try {
                    if (!serverIO.readFirst().equals("OK KEEP ALIVE")) {
                        break;
                    }
                } catch (ServerResponseException e) {
                    LOGGER.warning("Server response exception\n" + LogFormatter.formatStackTrace(e));
                    break;
                }
            } else if (serverIO.isClosed()) {
                break;
            }
        }
    }
}
