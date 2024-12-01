package com.github.rob269;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class LogFormatter extends Formatter {
    @Override
    public String format(LogRecord record) {

        return record.getLevel().getName() + "(" + Thread.currentThread().getName() + "): " + record.getMessage().replaceAll("\n", "\n\t") + "\n";
    }

    public static String formatStackTrace(Exception e) {
        StringBuilder string = new StringBuilder(e.toString());
        StackTraceElement[] elements = e.getStackTrace();
        for (StackTraceElement el : elements) {
            string.append("\n\tat ").append(el);
        }
        return string.toString();
    }
}
