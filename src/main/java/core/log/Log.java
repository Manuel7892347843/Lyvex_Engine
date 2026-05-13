package core.log;

import static core.log.Logs.logs;

public class Log {
    public String msg = "";

    public Log(String msg) {
        this.msg = msg;
    }

    public static void log(String msg){
        logs.add(new Log(msg));
    }
}
