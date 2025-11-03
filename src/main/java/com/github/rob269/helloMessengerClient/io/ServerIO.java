package com.github.rob269.helloMessengerClient.io;

import com.github.rob269.helloMessengerClient.AuthenticationException;
import com.github.rob269.helloMessengerClient.InitializationException;

import java.io.IOException;
import java.math.BigInteger;

public interface ServerIO {

    Batch writeBatch(int command, int batchSize, boolean log);

    void init() throws WrongKeyException, InitializationException, IOException, AuthenticationException;

    String readString(boolean log) throws IOException;

    default String readString() throws IOException {
        return readString(true);
    }

    byte readCommand() throws IOException;

    BigInteger readBigint() throws IOException;

    void writeCommand(int message) throws IOException;

    void writeCommand(int message, int packageCount) throws IOException;

    boolean isClosed();

    void close();
}
