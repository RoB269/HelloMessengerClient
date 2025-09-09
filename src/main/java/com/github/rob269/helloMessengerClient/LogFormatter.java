package com.github.rob269.helloMessengerClient;

import java.time.LocalDateTime;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class LogFormatter extends Formatter {
    private static final int MAX_TRACE_SIZE = 10;

    @Override
    public String format(LogRecord record) {
        LocalDateTime dateTime = LocalDateTime.now();
        return record.getSourceClassName() + "\n [" + dateTime.toString().substring(11) + "]" +  record.getLevel().getName() + "(" + Thread.currentThread().getName() + "): " + record.getMessage().replaceAll("\n", "\n\t") + "\n";
    }

    public static String formatStackTrace(Exception e) {
        StringBuilder string = new StringBuilder(e.toString());
        StackTraceElement[] elements = e.getStackTrace();
        for (int i = 0; i < elements.length && i < MAX_TRACE_SIZE; i++) {
            string.append("\n\tat ").append(elements[i]);
        }
        if (elements.length > MAX_TRACE_SIZE) {
            string.append("\n\t... ").append(elements.length-MAX_TRACE_SIZE).append(" elements");
        }
        return string.toString();
    }
}
