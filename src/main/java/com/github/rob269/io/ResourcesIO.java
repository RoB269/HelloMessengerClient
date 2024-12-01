package com.github.rob269.io;

import com.github.rob269.LogFormatter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class ResourcesIO {
    private static final Logger LOGGER = Logger.getLogger(Thread.currentThread().getName() + ":" + ResourcesIO.class.getName());
    public static final String ROOT_FOLDER = "resources/";
    public static final String EXTENSION = ".json";

    public static List<String> read(String filePath) {
        List<String> lines = new ArrayList<>();
        File file = new File(ROOT_FOLDER + filePath);
        if (file.exists()) {
            try {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    lines.add(line);
                }
                bufferedReader.close();
            } catch (IOException e) {
                LOGGER.warning("Can't read the file (" + filePath + ") " + e);
            }
        }
        else {
            LOGGER.warning("File is not exist (" + filePath + ")");
        }
        return lines;
    }

    public synchronized static void write(String filePath, List<String> lines, boolean append) {
        File file = new File(ROOT_FOLDER + filePath);
        if (file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                LOGGER.warning("Can't create new file (" + filePath + ")\n" + LogFormatter.formatStackTrace(e));
            }
        }
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, append), StandardCharsets.UTF_8));
            for (int i = 0; i < lines.size(); i++) {
                bufferedWriter.write(lines.get(i) + "\n");
            }
            bufferedWriter.flush();
            bufferedWriter.close();
        } catch (IOException e) {
            LOGGER.warning("Can't write to file (" + filePath + ") " + e);
        }
    }

    public synchronized static void write(String filePath, List<String> lines) {
        write(filePath, lines, false);
    }

    public synchronized static void delete(String filePath) {
        File file = new File(ROOT_FOLDER+filePath);
        if (file.exists()){
            file.delete();
        }
    }

    public static boolean isExist(String filePath) {
        return new File(ROOT_FOLDER + filePath).exists();
    }

    public synchronized static void writeObject(Object object, String path) {
        File file = new File(ROOT_FOLDER + path);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        try (FileOutputStream fos = new FileOutputStream(ROOT_FOLDER+path);
             ObjectOutputStream oos = new ObjectOutputStream(fos)){

            oos.writeObject(object);
        } catch (IOException e) {
            LOGGER.warning("Can't write object\n" + LogFormatter.formatStackTrace(e));
        }
    }

    public static Object readObject(String path) {
        if (new File(ROOT_FOLDER+path).exists()) {
            try (FileInputStream fis = new FileInputStream(ROOT_FOLDER + path);
                 ObjectInputStream ois = new ObjectInputStream(fis)) {
                return ois.readObject();
            } catch (IOException e) {
                LOGGER.warning("Object read exception\n" + LogFormatter.formatStackTrace(e));
            } catch (ClassNotFoundException e) {
                LOGGER.warning("Class not found\n" + LogFormatter.formatStackTrace(e));
            }
        }
        return null;
    }

    public synchronized static void writeJSON(String filePath, Object object) {
        File file = new File(ROOT_FOLDER + filePath);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                LOGGER.warning("Can't create new file (" + ROOT_FOLDER + filePath + ") " + e);
            }
        }
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8));
            GsonBuilder builder = new GsonBuilder();
            builder.setPrettyPrinting();
            builder.serializeNulls();
            Gson gson = builder.create();
            bufferedWriter.write(gson.toJson(object));
            bufferedWriter.flush();
            bufferedWriter.close();
        } catch (IOException e) {
            LOGGER.warning("Can't write to file (" + ROOT_FOLDER + filePath + ") " + e);
        }
    }

    public static <T> T readJSON(String filePath, Class<T> classOfT) {
        File file = new File(ROOT_FOLDER + filePath);
        if (file.exists()) {
            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));){
                Gson gson = new Gson();
                T object = gson.fromJson(bufferedReader, classOfT);
                return object;
            } catch (IOException e) {
                LOGGER.warning("Can't read the file (" + ROOT_FOLDER + filePath + ") " + e);
            }
        }
        return null;
    }
}
