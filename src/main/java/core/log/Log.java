package core.log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import static core.log.Logs.*;

public class Log {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    public enum Type {
        INFO,
        WARNING,
        ERROR,
        SUCCESS
    }

    public String msg = "";
    public Type type = Type.INFO;

    public Log(Object msg) {
        this(msg, Type.INFO);
    }

    public Log(Object msg, Type type) {
        this.msg = formatMessage(msg);
        this.type = type;
    }

    private static String formatMessage(Object msg) {
        String time = LocalTime.now().format(TIME_FORMATTER);
        return "[" + time + "] " + String.valueOf(msg);
    }

    public static void info(Object msg) {
        Log log = new Log(msg, Type.INFO);
        all.add(log);
        logs.add(log);
    }

    public static void warning(Object msg) {
        Log log = new Log(msg, Type.WARNING);
        all.add(log);
        logs_warning.add(log);
    }

    public static void error(Object msg) {
        Log log = new Log(msg, Type.ERROR);
        all.add(log);
        logs_error.add(log);
    }

    public static void error(String message, Throwable throwable) {
        Log log = new Log(message + "\n" + stackTraceToString(throwable), Type.ERROR);
        all.add(log);
        logs_error.add(log);
    }

    public static void error(Throwable throwable) {
        Log log = new Log(stackTraceToString(throwable), Type.ERROR);
        all.add(log);
        logs_error.add(log);
    }

    public static void success(Object msg) {
        Log log = new Log(msg, Type.SUCCESS);
        all.add(log);
        logs_success.add(log);
    }

    public static void log(Object msg){
        info(msg);
    }

    public static void logWaring(Object msg){
        warning(msg);
    }

    public static void logWarning(Object msg){
        warning(msg);
    }

    public static void logError(Object msg){
        error(msg);
    }

    public static void logError(String message, Throwable throwable){
        error(message, throwable);
    }

    public static void logError(Throwable throwable){
        error(throwable);
    }

    public static void logSuccess(Object msg){
        success(msg);
    }

    private static String stackTraceToString(Throwable throwable) {
        if (throwable == null) {
            return "";
        }

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        throwable.printStackTrace(printWriter);
        return stringWriter.toString();
    }
}