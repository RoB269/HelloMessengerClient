package com.github.rob269;

import com.github.rob269.io.ServerIO;
import com.github.rob269.io.ServerResponseException;
import com.github.rob269.rsa.RSAClientKeys;
import com.github.rob269.rsa.WrongKeyException;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class Main {
    static {
        File logsDir = new File("log/");
        if (!logsDir.exists()) {
            logsDir.mkdir();
        }
    }
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    public static ServerIO serverIO;
    public static SimpleInterface simpleInterface;

    public static void main(String[] args) {
        serverConnect();
    }

    public static void serverConnect() {
        RSAClientKeys.initKeys();
        Socket clientSocket = null;
        try {
            clientSocket = new Socket("127.0.0.1", 5099);
            serverIO = new ServerIO(clientSocket);
            try {
                clientSocket.setSoTimeout((int) TimeUnit.SECONDS.toMillis(10));
            } catch (SocketException e) {
                LOGGER.warning("Time out exception");
            }
            serverIO.init();
            simpleInterface = new SimpleInterface(serverIO);
            simpleInterface.keepAlive();
            simpleInterface.uiPanel();
        } catch (IOException e) {
            LOGGER.warning("Can't connect to server");
            e.printStackTrace();
        } catch (WrongKeyException e) {
            e.printStackTrace();
        } catch (ServerResponseException e) {
            LOGGER.warning("Wrong server response");
        }
        finally {
            if (clientSocket != null){
                serverIO.close();
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    LOGGER.warning("Client socket closing exception");
                    e.printStackTrace();
                }
            }
        }
    }
}
