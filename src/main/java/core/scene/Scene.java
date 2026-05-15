package core.scene;

import core.gameobject.GameObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Scene {
    private String name;
    private final List<GameObject> rootObjects = new ArrayList<>();

    public Scene() {
        this("Untitled Scene");
    }

    public Scene(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<GameObject> getRootObjects() {
        return Collections.unmodifiableList(rootObjects);
    }

    public void addChildObject(GameObject parent, GameObject object) {
        if (parent != null && object != null && !rootObjects.contains(object)) {
            parent.addChild(object);
        }
    }

    public void addRootObject(GameObject object) {
        if (object != null && !rootObjects.contains(object)) {
            rootObjects.add(object);
        }
    }

    public void removeRootObject(GameObject object) {
        rootObjects.remove(object);
    }
}