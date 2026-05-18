package ui;

import core.Engine;
import core.scene.Scene;
import core.gameobject.GameObject;

import java.util.ArrayList;
import java.util.List;

public final class EditorContext {
    private static EditorContext instance;
    private int sceneTextureId;

    private float sceneViewportX;
    private float sceneViewportY;
    private float sceneViewportWidth;
    private float sceneViewportHeight;
    private boolean sceneHovered;

    private Scene currentScene;
    private List<Runnable> sceneChangeListeners = new ArrayList<>();
    private GameObject selectedGameObject;
    private boolean sceneDirty;
    private Engine engine;

    private EditorContext() {}

    public static EditorContext getInstance() {
        if (instance == null) instance = new EditorContext();
        return instance;
    }

    public int getSceneTextureId() {
        return sceneTextureId;
    }

    public void setSceneTextureId(int sceneTextureId) {
        this.sceneTextureId = sceneTextureId;
    }

    public float getSceneViewportX() {
        return sceneViewportX;
    }

    public void setSceneViewportX(float sceneViewportX) {
        this.sceneViewportX = sceneViewportX;
    }

    public float getSceneViewportY() {
        return sceneViewportY;
    }

    public void setSceneViewportY(float sceneViewportY) {
        this.sceneViewportY = sceneViewportY;
    }

    public float getSceneViewportWidth() {
        return sceneViewportWidth;
    }

    public void setSceneViewportWidth(float sceneViewportWidth) {
        this.sceneViewportWidth = sceneViewportWidth;
    }

    public float getSceneViewportHeight() {
        return sceneViewportHeight;
    }

    public void setSceneViewportHeight(float sceneViewportHeight) {
        this.sceneViewportHeight = sceneViewportHeight;
    }

    public boolean isSceneHovered() {
        return sceneHovered;
    }

    public void setSceneHovered(boolean sceneHovered) {
        this.sceneHovered = sceneHovered;
    }

    public Scene getCurrentScene() {
        return currentScene;
    }

    public void setCurrentScene(Scene currentScene) {
        this.currentScene = currentScene;
        for (Runnable listener : sceneChangeListeners) {
            listener.run();
        }
    }

    public void addSceneChangeListener(Runnable listener) {
        sceneChangeListeners.add(listener);
    }

    public GameObject getSelectedGameObject() {
        return selectedGameObject;
    }

    public void setSelectedGameObject(GameObject selectedGameObject) {
        this.selectedGameObject = selectedGameObject;
    }

    public boolean isSceneDirty() {
        return sceneDirty;
    }

    public void setSceneDirty(boolean sceneDirty) {
        this.sceneDirty = sceneDirty;
    }

    public Engine getEngine() {
        return engine;
    }

    public void setEngine(Engine engine) {
        this.engine = engine;
    }

}