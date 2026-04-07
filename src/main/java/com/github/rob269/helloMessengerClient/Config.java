package com.github.rob269.helloMessengerClient;

public interface Config {

    void parseConfigFile(String file);

    void writeIp(String ip);
}
