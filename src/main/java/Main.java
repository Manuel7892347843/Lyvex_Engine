import core.Engine;
import core.runtime.RuntimeApplication;

import java.nio.file.Path;

public class Main {
    public static void main(String[] args) {
        if (args.length >= 2 && args[0].equalsIgnoreCase("--runtime")) {
            Path projectRoot = Path.of(stripWrappingQuotes(args[1]));
            new RuntimeApplication().run(projectRoot);
            return;
        }

        new Engine().run();
    }

    private static String stripWrappingQuotes(String value) {
        if (value == null) {
            return "";
        }

        String cleaned = value.trim();

        while (cleaned.startsWith("\"")) {
            cleaned = cleaned.substring(1);
        }

        while (cleaned.endsWith("\"")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }

        return cleaned;
    }
}
