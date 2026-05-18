package core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import core.lib.SceneManager;
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

    // --- Dati serializzabili ---

    public static class ProjectData {
        public String projectName = "";
        public List<String> sortingLayers = new ArrayList<>();
        public List<SceneManager.SceneEntry> scenes = new ArrayList<>();
        public String startupScene = "";
        public int targetFPS = 60;
    }

    // --- Getters (lazy init) ---

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

    // --- Salva nel file .lyvex ---

    public static void save() {
        Path projectFile = ProjectManager.getProjectFilePath();

        ProjectData data = new ProjectData();
        data.projectName = ProjectManager.getProjectRoot().getFileName().toString();

        // Sorting layers
        data.sortingLayers = getSortingLayerManager().getLayers();

        // Scene entries
        data.scenes = getSceneManager().getSceneEntries();

        // Startup scene
        SceneManager.SceneEntry active = getSceneManager().getActiveSceneEntry();
        if (active != null) {
            data.startupScene = active.fileName;
        }

        try {
            Files.createDirectories(projectFile.getParent());
            Files.writeString(projectFile, GSON.toJson(data));
            System.out.println("Project saved to: " + projectFile);
        } catch (IOException e) {
            System.err.println("Failed to save project settings");
            e.printStackTrace();
        }
    }

    // --- Carica dal file .lyvex ---

    public static void load() {
        Path projectFile = ProjectManager.getProjectFilePath();

        if (!Files.exists(projectFile)) {
            System.out.println("No project file found, using defaults");
            return;
        }

        try {
            String json = Files.readString(projectFile);
            ProjectData data = GSON.fromJson(json, ProjectData.class);

            if (data == null) return;

            // Carica sorting layers
            if (data.sortingLayers != null && !data.sortingLayers.isEmpty()) {
                getSortingLayerManager().loadLayers(data.sortingLayers);
            }

            // Carica scene entries
            if (data.scenes != null) {
                getSceneManager().loadFromProject(data.scenes);
            }

            System.out.println("Project loaded from: " + projectFile);
        } catch (IOException e) {
            System.err.println("Failed to load project settings");
            e.printStackTrace();
        }
    }
}