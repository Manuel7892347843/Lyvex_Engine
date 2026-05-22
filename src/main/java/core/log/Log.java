package core.log;

import static core.log.Logs.*;

public class Log {
    public String msg = "";

    public Log(Object msg) {
        this.msg = msg.toString();
    }

    public static void log(Object msg){
        logs.add(new Log(msg.toString()));
    }

    public static void logWaring(Object msg){
        logs_warning.add(new Log(msg.toString()));
    }

    public static void logError(Object msg){
        logs_error.add(new Log(msg.toString()));
    }

    public static void logSuccess(Object msg){
        logs_success.add(new Log(msg.toString()));
    }
}
