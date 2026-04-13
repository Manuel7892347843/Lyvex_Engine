package core.component;

import core.GameObject;

public class Component {
    private GameObject gameObject;

    public void start() {
    }

    public void update() {
    }

    public GameObject getGameObject() {
        return gameObject;
    }

    public void setGameObject(GameObject gameObject) {
        this.gameObject = gameObject;
    }

    public String getTypeName() {
        return getClass().getName();
    }
}