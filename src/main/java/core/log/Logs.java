package core.log;

import java.util.ArrayList;
import java.util.List;

public class Logs {
    public static List<Log> all = new ArrayList<>();
    public static List<Log> logs = new ArrayList<>();
    public static List<Log> logs_warning = new ArrayList<>();
    public static List<Log> logs_error = new ArrayList<>();
    public static List<Log> logs_success = new ArrayList<>();

    public static void clear() {
        all.clear();
        logs.clear();
        logs_warning.clear();
        logs_error.clear();
        logs_success.clear();
    }
}