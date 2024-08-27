package com.github.rob269;

import com.github.rob269.io.ServerInterface;
import com.github.rob269.rsa.RSAClientKeys;
import com.github.rob269.rsa.WrongKeyException;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.logging.Logger;

public class Main {
    static {
        File logsDir = new File("log/");
        if (!logsDir.exists()) {
            logsDir.mkdir();
        }
    }
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        RSAClientKeys.initKeys();
        try (Socket clientSocket = new Socket("127.0.0.1", 5099);
            ServerInterface serverInterface = new ServerInterface(clientSocket)){
            serverInterface.init();
            if (!serverInterface.isClosed()) LOGGER.info("YEEEEEEEEE");
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (WrongKeyException e) {
            LOGGER.warning("Wrong server key");
            e.printStackTrace();
        }
    }
}
