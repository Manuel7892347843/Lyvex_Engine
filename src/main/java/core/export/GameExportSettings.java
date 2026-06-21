package core.export;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class GameExportSettings {
    public String gameName = "LyvexGame";
    public Path destinationDirectory;

    public String startupScene = "";

    public List<SceneBuildEntry> scenes = new ArrayList<>();

    public int windowWidth = 1280;
    public int windowHeight = 720;
    public boolean fullscreen = false;
    public boolean vsync = true;

    public static class SceneBuildEntry {
        public String name;
        public String fileName;
        public boolean inBuild;

        public SceneBuildEntry(String name, String fileName, boolean inBuild) {
            this.name = name;
            this.fileName = fileName;
            this.inBuild = inBuild;
        }
    }
}