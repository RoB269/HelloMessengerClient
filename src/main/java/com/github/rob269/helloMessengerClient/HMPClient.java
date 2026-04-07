package com.github.rob269.helloMessengerClient;

import com.github.rob269.helloMessengerClient.io.ResourcesIO;
import com.github.rob269.helloMessengerClient.rsa.Key;
import com.github.rob269.helloMessengerClient.rsa.RSA;
import com.github.rob269.helloMessengerClient.rsa.RSAKeysPair;

import java.math.BigInteger;
import java.util.logging.Logger;

public class HMPClient extends Client {
    private static final Logger LOGGER = Logger.getLogger(HMPClient.class.getName());
    private static RSAKeysPair userKeys;


    public static void initKeys() {
        if (userKeys == null) {
            if (ResourcesIO.isExist(ResourcesIO.getResourcesPath() + "userKeys.json")) {
                try {
                    RSAKeysPair userKeys = ResourcesIO.readJSON(ResourcesIO.getResourcesPath() + "userKeys.json", RSAKeysPair.class);
                    if (userKeys == null) {
                        throw new NullPointerException();
                    }
                    HMPClient.userKeys = userKeys;
                    LOGGER.fine("The keys have been read");
                } catch (NullPointerException e) {
                    LOGGER.warning("Keys not found");
                    generateNewKeys();
                }
            } else {
                generateNewKeys();
            }
            LOGGER.fine("The keys have been initialized");
        }
    }

    private static void generateNewKeys() {
        BigInteger[][] keys = RSA.generateKeys();
        HMPClient.userKeys = new RSAKeysPair(keys);
        ResourcesIO.writeJSON(ResourcesIO.getResourcesPath() + "userKeys.json", userKeys);
        LOGGER.fine("The keys were generated and written to the file");
    }

    public static Key getPublicKey() {
        return userKeys.getPublicKey();
    }

    public static Key getPrivateKey() {
        return userKeys.getPrivateKey();
    }

}
