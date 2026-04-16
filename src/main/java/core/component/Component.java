package core.component;

import core.GameObject;

public class Component {
    private GameObject gameObject;
    private boolean enabled = true;
    private boolean started = false;
    private boolean awoken = false;

    public void awake() {
    }

    public void onEnable() {
    }

    public void start() {
    }

    public void update() {
    }

    public void lateUpdate() {
    }

    public void fixedUpdate() {
    }

    public void onDisable() {
    }

    public void onDestroy() {
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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return;
        }

        this.enabled = enabled;

        if (enabled) {
            onEnable();
        } else {
            onDisable();
        }
    }

    public boolean isStarted() {
        return started;
    }

    public void setStarted(boolean started) {
        this.started = started;
    }

    public boolean isAwoken() {
        return awoken;
    }

    public void setAwoken(boolean awoken) {
        this.awoken = awoken;
    }
}