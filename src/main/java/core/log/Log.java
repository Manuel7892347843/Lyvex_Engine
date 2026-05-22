package core.log;

import static core.log.Logs.*;

public class Log {
    public String msg = "";

    public Log(String msg) {
        this.msg = msg;
    }

    public static void log(String msg){
        logs.add(new Log(msg));
    }

    public static void logWaring(String msg){
        logs_warning.add(new Log(msg));
    }

    public static void logError(String msg){
        logs_error.add(new Log(msg));
    }

    public static void logSuccess(String msg){
        logs_success.add(new Log(msg));
    }
}
