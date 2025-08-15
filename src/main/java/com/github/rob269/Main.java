package com.github.rob269;

import com.github.rob269.io.ServerIO;
import com.github.rob269.rsa.WrongKeyException;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
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
    private static String serverIp = "127.0.0.1";
    private static boolean mini = false;
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    public static ServerIO serverIO;
//    public static SimpleUserInterface simpleUserInterface; todo

    public static void main(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-login")) {
                if (i+2<args.length) {
                    Client.login(args[i + 1], args[i + 2]);
                    i += 2;
                }
                else {
                    LOGGER.warning("Login arguments exception");
                }
            }
            else if (args[i].equals("-lang")) {
                if (i+1< args.length) {
//                    SimpleUserInterface.lang = args[++i]; todo
                }
            }
            else if (args[i].equals("-ip")) {
                if (i+1 < args.length) {
                    serverIp = args[++i];
                }
            } else if (args[i].equals("-mini")) {
                mini = true;
            }
        }
        serverConnect();
    }

    public static boolean isMini() {
        return mini;
    }

    public static void serverConnect() {
        if (!Client.isLogin()) {
            Scanner scanner = new Scanner(System.in);
//            System.out.print(SimpleUserInterface.lang.equals("RU") ? "Имя пользователя: " : "Username: "); todo
            String username = scanner.nextLine();
//            System.out.print(SimpleUserInterface.lang.equals("RU") ? "Пароль: " : "Password: ");
            String password = scanner.nextLine();
            Client.login(username, password);
        }
        Client.initKeys();
        Socket clientSocket = null;
        try {
            Thread.currentThread().setName("MainConnectionThread");
            clientSocket = new Socket(serverIp, 5099);
            serverIO = new ServerIO(clientSocket);
            clientSocket.setSoTimeout((int) TimeUnit.SECONDS.toMillis(10));
            serverIO.init();
//            simpleUserInterface = new SimpleUserInterface(serverIO); todo
//            simpleUserInterface.uiPanel();
//            Scanner scanner = new Scanner(System.in);
//            serverIO.writeCommand(scanner.nextInt());
//            scanner.nextInt();
        } catch (IOException e) {
            LOGGER.warning("Can't connect to server\n" + LogFormatter.formatStackTrace(e));
        } catch (WrongKeyException e) {
            LOGGER.warning(LogFormatter.formatStackTrace(e));
        } catch (InitializationException e) {
            LOGGER.warning("Fail initialization");
        }
        finally {
            if (clientSocket != null){
                try {
                    serverIO.close();
                    clientSocket.close();
                } catch (IOException e) {
                    LOGGER.warning("Client socket closing exception\n" + LogFormatter.formatStackTrace(e));
                }
            }
        }
    }
}