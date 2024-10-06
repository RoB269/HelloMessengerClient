package com.github.rob269.rsa;

import com.github.rob269.UserAccount;
import com.github.rob269.io.ResourcesIO;

import java.math.BigInteger;
import java.util.logging.Logger;

public class RSAClientKeys {
    private static RSAKeys userKeys;
    private static final UserAccount user = new UserAccount("#USER#", "#TestPassword#"); //todo
    private static final Logger LOGGER = Logger.getLogger(Thread.currentThread().getName() + ":" + RSAClientKeys.class.getName());
    private static boolean needToRegister = false;

    public static void initKeys() {
        if (ResourcesIO.isExist("RSA/userKeys.json")) {
            try {
                RSAKeys userKeys = ResourcesIO.readJSON("RSA/userKeys.json", RSAKeys.class);
                if (userKeys == null || userKeys.getUser() == null) {
                    throw new NullPointerException();
                }
                RSAClientKeys.userKeys = userKeys;
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

    public static void register(UserKey key) throws WrongKeyException{
        if (key != null && needToRegister){
            userKeys.setPublicKey(key);
            ResourcesIO.writeJSON("RSA/userKeys" + ResourcesIO.EXTENSION, userKeys);
            needToRegister = false;
            LOGGER.fine("Key was registered");
        }
        else if (key == null){
            LOGGER.warning("Key is null");
            throw new WrongKeyException("Key is null");
        }
    }

    public static boolean isNeedToRegister() {
        return needToRegister;
    }

    private static void generateNewKeys() {
        BigInteger[][] keys = RSA.generateKeys();
        needToRegister = true;
        RSAClientKeys.userKeys = new RSAKeys(keys, user);
        LOGGER.fine("The keys were generated");
    }

    public static UserKey getPublicKey() {
        return userKeys.getPublicKey();
    }

    public static Key getPrivateKey() {
        return userKeys.getPrivateKey();
    }

    public static String getPassword() {
        return user.getPasswordHash();
    }
}
