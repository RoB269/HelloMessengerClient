package com.github.rob269;

import com.github.rob269.io.ServerIO;
import com.github.rob269.io.ServerResponseException;
import com.github.rob269.rsa.RSAClientKeys;
import com.github.rob269.rsa.WrongKeyException;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class Main {
    static {
        File logsDir = new File("log/");
        if (!logsDir.exists()) {
            logsDir.mkdir();
        }
    }
    private static String ip = "127.0.0.1";
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    public static ServerIO serverIO;
    public static SimpleInterface simpleInterface;

    public static void main(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-login")) {
                if (i+2<args.length) {
                    RSAClientKeys.login(args[i + 1], args[i + 2]);
                    i += 2;
                }
                else {
                    LOGGER.warning("Login exception");
                }
            }
            else if (args[i].equals("-lang")) {
                if (i+1< args.length) {
                    SimpleInterface.lang = args[i+1];
                }
            }
            else if (args[i].equals("-ip")) {
                if (i+1 < args.length) {
                    ip = args[i+1];
                }
            }
        }
        serverConnect();
    }

    public static void serverConnect() {
        if (!RSAClientKeys.isLogin()) {
            Scanner scanner = new Scanner(System.in);
            System.out.print(SimpleInterface.lang.equals("RU") ? "Имя пользователя: " : "Username: ");
            String username = scanner.nextLine();
            System.out.print(SimpleInterface.lang.equals("RU") ? "Пароль: " : "Password: ");
            String password = scanner.nextLine();
            RSAClientKeys.login(username, password);
        }
        RSAClientKeys.initKeys();
        Socket clientSocket = null;
        try {
            clientSocket = new Socket(ip, 5099);
            serverIO = new ServerIO(clientSocket);
            try {
                clientSocket.setSoTimeout((int) TimeUnit.SECONDS.toMillis(10));
            } catch (SocketException e) {
                LOGGER.warning("Time out exception");
            }
            serverIO.init();
            simpleInterface = new SimpleInterface(serverIO);
            simpleInterface.checking();
            simpleInterface.uiPanel();
        } catch (IOException e) {
            LOGGER.warning("Can't connect to server\n" + LogFormatter.formatStackTrace(e));
        } catch (WrongKeyException e) {
            LOGGER.warning(LogFormatter.formatStackTrace(e));
        } catch (ServerResponseException e) {
            LOGGER.warning("Wrong server response\n" + LogFormatter.formatStackTrace(e));
        }
        finally {
            if (clientSocket != null){
                serverIO.close();
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    LOGGER.warning("Client socket closing exception\n" + LogFormatter.formatStackTrace(e));
                }
            }
        }
    }
}
