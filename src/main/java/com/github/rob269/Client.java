package com.github.rob269;

import com.github.rob269.io.ResourcesIO;
import com.github.rob269.rsa.Key;
import com.github.rob269.rsa.RSA;
import com.github.rob269.rsa.RSAKeysPair;

import java.math.BigInteger;
import java.util.logging.Logger;

public class Client {
    private static boolean isLogin = false;
    private static RSAKeysPair userKeys;
    private static User user = null;
    private static final Logger LOGGER = Logger.getLogger(Client.class.getName());

    public static void initKeys() {
        if (ResourcesIO.isExist("RSA/userKeys.json")) {
            try {
                RSAKeysPair userKeys = ResourcesIO.readJSON("RSA/userKeys.json", RSAKeysPair.class);
                if (userKeys == null) {
                    throw new NullPointerException();
                }
                Client.userKeys = userKeys;
                LOGGER.fine("The keys have been read");
            } catch (NullPointerException e) {
                LOGGER.warning("Keys not found");
                generateNewKeys();
            }
        }
        else {
            generateNewKeys();
        }
        LOGGER.fine("The keys have been initialized");
    }

    public static void login(String username, String password) {
        user = new User(username, password);
        isLogin = true;
    }

    public static boolean isLogin() {
        return isLogin;
    }

    private static void generateNewKeys() {
        BigInteger[][] keys = RSA.generateKeys();
        Client.userKeys = new RSAKeysPair(keys);
        ResourcesIO.writeJSON("RSA/userKeys"+ResourcesIO.EXTENSION, userKeys);
        LOGGER.fine("The keys were generated and written to the file");
    }

    public static Key getPublicKey() {
        return userKeys.getPublicKey();
    }

    public static Key getPrivateKey() {
        return userKeys.getPrivateKey();
    }

    public static String getPassword() {
        return user.getPassword();
    }

    public static String getUserId() {
        return user.getId();
    }
}
