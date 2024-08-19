package com.github.rob269.rsa;

import com.github.rob269.User;
import com.github.rob269.io.ResourcesInterface;

import java.math.BigInteger;
import java.util.logging.Logger;

public class RSAClientKeys {
    private static RSAKeys userKeys;
    private static final User user = new User("#TEST#"); //todo
    private static final Logger LOGGER = Logger.getLogger(Thread.currentThread().getName() + ":" + RSAClientKeys.class.getName());

    public static void initKeys() {
        if (ResourcesInterface.isExist("RSA/userKeys.json")) {
            try {
                RSAKeys userKeys = ResourcesInterface.readJSON("RSA/userKeys.json", RSAKeys.class);
                if (userKeys == null || userKeys.getUser() == null) {
                    throw new NullPointerException();
                }
                RSAClientKeys.userKeys = userKeys;
                LOGGER.fine("The keys have been read");
            } catch (NullPointerException e) {
                LOGGER.warning("Keys not found");
                writeNewKeys();
            }
        }
        else {
            writeNewKeys();
        }
        LOGGER.fine("The keys have been initialized");
    }

    private static void writeNewKeys() {
        BigInteger[][] keys = RSA.generateKeys(512);
        RSAClientKeys.userKeys = new RSAKeys(keys, user);
        ResourcesInterface.writeJSON("RSA/serverKeys.json", userKeys);
        LOGGER.fine("The keys were generated and written down");
    }

    public static Key getPublicKey() {
        return userKeys.getPublicKey();
    }

    public static Key getPrivateKey() {
        return userKeys.getPrivateKey();
    }
}
