package com.github.rob269.helloMessengerClient;

import com.github.rob269.helloMessengerClient.io.ResourcesIO;
import com.github.rob269.helloMessengerClient.rsa.Guarantor;
import com.github.rob269.helloMessengerClient.rsa.Key;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class HMPConfig implements Config {
    private static final Logger LOGGER = Logger.getLogger(HMPConfig.class.getName());
    private static final List<String> configList = List.of(new String[]{"guarantor_public_key", "server_ip", "port"});
    private static String configFilePath = "";

    @Override
    public void parseConfigFile(String file) {
        if (configFilePath.isEmpty()) configFilePath = file;
        try {
            if (!ResourcesIO.isExist(file)) {
                ResourcesIO.write(file, new ArrayList<>());
                throw new RuntimeException();
            }
            Map<String, String[]> values = new HashMap<>();
            StringBuilder builder = new StringBuilder();
            List<String> lines = ResourcesIO.read(file);
            for (String line : lines) builder.append(line);
            String[] fileLines = builder.toString().replaceAll(" ", "").split(";");
            for (String line : fileLines) {
                for (String config : configList) {
                    if (line.startsWith(config)) {
                        values.put(config, line.split("=")[1].split(","));
                    }
                }
            }
            if (values.containsKey("guarantor_public_key")) {
                Guarantor.init(new Key(new BigInteger[]{new BigInteger(values.get("guarantor_public_key")[0]), new BigInteger(values.get("guarantor_public_key")[1])}));
                if (values.containsKey("server_ip")) {
                    Main.setServerIp(values.get("server_ip")[0]);
                }
                if (values.containsKey("port")) {
                    Main.setPort(Integer.parseInt(values.get("port")[0]));
                }
            } else throw new RuntimeException();
            if (file.equals("defConfig")) {
                ResourcesIO.write(configFilePath, lines);
            }
        } catch (RuntimeException e) {
            if (!file.equals("defConfig")) {
                LOGGER.warning("The configuration file doesn't contain the necessary data");
                parseConfigFile("defConfig");
            }
            else {
                LOGGER.severe("Default configuration file doesn't exist");
                throw new RuntimeException();
            }
        }
    }

    @Override
    public void writeIp(String ip) {
        ResourcesIO.write(configFilePath, List.of("\nserver_ip = " + ip + ";"), true);
    }
}
