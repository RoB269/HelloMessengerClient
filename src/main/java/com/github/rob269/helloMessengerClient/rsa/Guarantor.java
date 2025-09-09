package com.github.rob269.helloMessengerClient.rsa;

public class Guarantor {
    private static Key publicKey = null;

    public static void init(Key publicKey) {
        if (Guarantor.publicKey == null) {
            Guarantor.publicKey = publicKey;
        }
    }

    public static Key getPublicKey() {
        return publicKey;
    }
}