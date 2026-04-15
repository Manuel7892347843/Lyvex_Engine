package core;

import ui.panels.ConsolePanel;

import static core.Logs.logs;

public class Log {
    public String msg = "";

    public Log(String msg) {
        this.msg = msg;
    }

    public static void log(String msg){
        logs.add(new Log(msg));
    }
}
