package core.lib;

import core.Engine;
import core.ProjectManager;
import ui.EditorContext;

import java.nio.file.Path;

public final class Application {

    private static final String ENGINE_NAME = "Lyvex Engine";
    private static final String ENGINE_VERSION = "1.0.0";

    private Application() {}

    public static String engineName() {
        return ENGINE_NAME;
    }

    public static String engineVersion() {
        return ENGINE_VERSION;
    }

    public static boolean isEditor() {
        return true;
    }

    public static boolean isPlaying() {
        Engine engine = EditorContext.getInstance().getEngine();
        return engine != null && engine.getEngineState();
    }

    public static boolean isPaused() {
        return false;
    }

    public static void quit() {
        System.exit(0);
    }

    public static Path projectPath() {
        return ProjectManager.getProjectRoot();
    }

    public static Path assetsPath() {
        return ProjectManager.getAssetsPath();
    }

    public static Path scriptsPath() {
        return ProjectManager.getScriptsPath();
    }

    public static Path scenesPath() {
        return ProjectManager.getScenesPath();
    }

    public static Path projectSettingsPath() {
        return ProjectManager.getProjectSettingsPath();
    }

    public static Path compiledPath() {
        return ProjectManager.getCompiledPath();
    }

    public static Path projectFilePath() {
        return ProjectManager.getProjectFilePath();
    }
}