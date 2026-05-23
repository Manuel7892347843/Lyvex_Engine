package core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
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
            Log.logSuccess("Project saved to: " + projectFile);
        } catch (IOException e) {
            Log.logError("Failed to save project settings: \n" + e);
        }
    }

    public static void load() {
        Path projectFile = ProjectManager.getProjectFilePath();

        if (!Files.exists(projectFile)) {
            Log.logWaring("No project file found, using defaults");
            save();
            return;
        }

        try {
            if (Files.size(projectFile) == 0) {
                Log.logWaring("Project file is empty, recreating defaults");
                save();
                return;
            }

            String json = Files.readString(projectFile);

            if (json == null || json.isBlank()) {
                Log.logWaring("Project file is blank, recreating defaults");
                save();
                return;
            }

            ProjectData data = GSON.fromJson(json, ProjectData.class);

            if (data == null) {
                Log.logWaring("Project file contains no data, using defaults");
                save();
                return;
            }

            if (data.sortingLayers != null && !data.sortingLayers.isEmpty()) {
                getSortingLayerManager().loadLayers(data.sortingLayers);
            }

            if (data.scenes != null) {
                getSceneManager().loadFromProject(data.scenes);
            }

            Log.logSuccess("Project loaded from: " + projectFile);
        } catch (IOException | JsonSyntaxException e) {
            System.err.println("Failed to load project settings");
            e.printStackTrace();

            Log.logWaring("Recreating project settings with defaults");
            save();
        }
    }
}