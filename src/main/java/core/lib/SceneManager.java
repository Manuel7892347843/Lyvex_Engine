package core.lib;

import core.Engine;
import core.ProjectManager;
import core.scene.Scene;
import core.scene.SceneSerializer;
import ui.EditorContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class SceneManager {
    private final List<SceneEntry> sceneEntries = new ArrayList<>();
    private int activeSceneIndex = -1;

    public static class SceneEntry {
        public String name;
        public String fileName;
        public boolean inBuild;

        public SceneEntry(String name, String fileName) {
            this.name = name;
            this.fileName = fileName;
            this.inBuild = true;
        }
    }

    public void addScene(String name, String fileName) {
        Path scenePath = ProjectManager.getScenesPath().resolve(fileName);
        if (!Files.exists(scenePath)) {
            Scene newScene = new Scene(name);
            try {
                SceneSerializer.save(newScene, scenePath);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create scene: " + fileName, e);
            }
        }
        sceneEntries.add(new SceneEntry(name, fileName));
    }

    public void removeScene(int index) {
        if (index >= 0 && index < sceneEntries.size()) {
            sceneEntries.remove(index);
            if (activeSceneIndex == index) {
                activeSceneIndex = -1;
            } else if (activeSceneIndex > index) {
                activeSceneIndex--;
            }
        }
    }

    public void renameScene(int index, String newName) {
        if (index >= 0 && index < sceneEntries.size()) {
            sceneEntries.get(index).name = newName;
        }
    }

    public void moveSceneUp(int index) {
        if (index > 0) {
            SceneEntry temp = sceneEntries.get(index);
            sceneEntries.set(index, sceneEntries.get(index - 1));
            sceneEntries.set(index - 1, temp);
            if (activeSceneIndex == index) activeSceneIndex--;
            else if (activeSceneIndex == index - 1) activeSceneIndex++;
        }
    }

    public void moveSceneDown(int index) {
        if (index < sceneEntries.size() - 1) {
            SceneEntry temp = sceneEntries.get(index);
            sceneEntries.set(index, sceneEntries.get(index + 1));
            sceneEntries.set(index + 1, temp);
            if (activeSceneIndex == index) activeSceneIndex++;
            else if (activeSceneIndex == index + 1) activeSceneIndex--;
        }
    }

    public void setSceneInBuild(int index, boolean inBuild) {
        if (index >= 0 && index < sceneEntries.size()) {
            sceneEntries.get(index).inBuild = inBuild;
        }
    }

    public List<SceneEntry> getSceneEntries() {
        return new ArrayList<>(sceneEntries);
    }

    public void loadScene(int index) {
        if (index < 0 || index >= sceneEntries.size()) return;

        SceneEntry entry = sceneEntries.get(index);
        Path scenePath = ProjectManager.getScenesPath().resolve(entry.fileName);

        try {
            Scene scene = SceneSerializer.load(scenePath);
            EditorContext.getInstance().setCurrentScene(scene);
            activeSceneIndex = index;

            notifySceneChanged(scene);

        } catch (IOException e) {
            System.err.println("Failed to load scene: " + entry.fileName);
            e.printStackTrace();
        }
    }

    private void notifySceneChanged(Scene scene) {
        Engine engine = EditorContext.getInstance().getEngine();
        if (engine != null) {
            engine.onSceneChanged(scene);
        }
    }

    public void loadSceneByName(String name) {
        for (int i = 0; i < sceneEntries.size(); i++) {
            if (sceneEntries.get(i).name.equals(name)) {
                loadScene(i);
                return;
            }
        }
    }

    public Scene getCurrentScene() {
        return EditorContext.getInstance().getCurrentScene();
    }

    public int getActiveSceneIndex() {
        return activeSceneIndex;
    }

    public void saveToProject() {
        // Salva la lista scene nel project settings JSON
        // Implementato in ProjectSettings
    }

    public void loadFromProject(List<SceneEntry> entries) {
        sceneEntries.clear();
        if (entries != null) {
            sceneEntries.addAll(entries);
        }
    }

    public SceneEntry getActiveSceneEntry() {
        if (activeSceneIndex >= 0 && activeSceneIndex < sceneEntries.size()) {
            return sceneEntries.get(activeSceneIndex);
        }
        return null;
    }
}