package com.github.rob269;

import com.github.rob269.io.ServerInterface;
import com.github.rob269.rsa.Key;

import java.io.IOException;
import java.net.Socket;
import java.util.logging.Logger;

public class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        try (Socket clientSocket = new Socket("127.0.0.1", 5099);
             ServerInterface serverInterface = new ServerInterface(clientSocket)){
            serverInterface.init();
            System.out.println("YEEEEEEEEEEEEEE");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
