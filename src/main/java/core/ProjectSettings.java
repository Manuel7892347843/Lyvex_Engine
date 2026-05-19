package core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import core.lib.SceneManager;
import core.log.Log;
import core.sorting.SortingLayerManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ProjectSettings {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static SortingLayerManager sortingLayerManager;
    private static SceneManager sceneManager;

    public static class ProjectData {
        public String projectName = "";
        public List<String> sortingLayers = new ArrayList<>();
        public List<SceneManager.SceneEntry> scenes = new ArrayList<>();
        public String startupScene = "";
        public int targetFPS = 60;
    }

    public static SortingLayerManager getSortingLayerManager() {
        if (sortingLayerManager == null) {
            sortingLayerManager = new SortingLayerManager();
        }
        return sortingLayerManager;
    }

    public static SceneManager getSceneManager() {
        if (sceneManager == null) {
            sceneManager = new SceneManager();
        }
        return sceneManager;
    }

    public static void save() {
        Path projectFile = ProjectManager.getProjectFilePath();

        ProjectData data = new ProjectData();
        data.projectName = ProjectManager.getProjectRoot().getFileName().toString();

        data.sortingLayers = getSortingLayerManager().getLayers();

        data.scenes = getSceneManager().getSceneEntries();

        SceneManager.SceneEntry active = getSceneManager().getActiveSceneEntry();
        if (active != null) {
            data.startupScene = active.fileName;
        }

        try {
            Files.createDirectories(projectFile.getParent());
            Files.writeString(projectFile, GSON.toJson(data));
            Log.log("Project saved to: " + projectFile);
        } catch (IOException e) {
            System.err.println("Failed to save project settings");
            e.printStackTrace();
        }
    }

    public static void load() {
        Path projectFile = ProjectManager.getProjectFilePath();

        if (!Files.exists(projectFile)) {
            Log.log("No project file found, using defaults");
            return;
        }

        try {
            String json = Files.readString(projectFile);
            ProjectData data = GSON.fromJson(json, ProjectData.class);

            if (data == null) return;

            if (data.sortingLayers != null && !data.sortingLayers.isEmpty()) {
                getSortingLayerManager().loadLayers(data.sortingLayers);
            }

            if (data.scenes != null) {
                getSceneManager().loadFromProject(data.scenes);
            }

            Log.log("Project loaded from: " + projectFile);
        } catch (IOException e) {
            System.err.println("Failed to load project settings");
            e.printStackTrace();
        }
    }
}