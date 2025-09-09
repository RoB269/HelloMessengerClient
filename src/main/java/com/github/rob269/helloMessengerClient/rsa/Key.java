package com.github.rob269.helloMessengerClient.rsa;

import java.math.BigInteger;

public class Key {
    private final BigInteger[] key = new BigInteger[2];

    public Key(BigInteger[] key) {
        this.key[0] = key[0];
        this.key[1] = key[1];
    }

    public Key() {}

    public BigInteger[] getKey() {
        return key;
    }

    @Override
    public String toString() {
        return key[0] + "\n" + key[1] + "\n";
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Key val)) return false;
        return this.key[0].compareTo(val.getKey()[0]) == 0 && this.key[1].compareTo(val.getKey()[1]) == 0;
    }
}
