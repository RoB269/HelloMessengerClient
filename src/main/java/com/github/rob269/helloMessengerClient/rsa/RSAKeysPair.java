package com.github.rob269.helloMessengerClient.rsa;

import com.github.rob269.helloMessengerClient.Main;
import com.google.gson.annotations.SerializedName;

import java.math.BigInteger;

public class RSAKeysPair {
    @SerializedName("public_key")
    private Key publicKey;
    @SerializedName("private_key")
    private final Key privateKey;

    public RSAKeysPair(Key publicKey, Key privateKey) {
        this.publicKey = publicKey;
        this.privateKey = privateKey;
    }

    public RSAKeysPair(BigInteger[][] keys) {
        this.publicKey = new Key(keys[0]);
        this.privateKey = new Key(keys[1]);
    }

    public Key getPublicKey() {
        return publicKey;
    }

    public Key getPrivateKey() {
        return privateKey;
    }

    public void setPublicKey(Key publicKey) {
        this.publicKey = publicKey;
    }

    @Override
    public String toString() {
        return "public_key:\n" +
                publicKey.toString() +
                "private_key:\n" +
                privateKey.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof RSAKeysPair keys)) return false;
        return this.publicKey.equals(keys.publicKey) && this.privateKey.equals(keys.privateKey);
    }
}
