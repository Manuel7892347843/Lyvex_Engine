package core.runtime;

import core.component.Component;
import core.gameobject.GameObject;
import core.physics.Collider2D;
import core.physics.PhysicsWorld2D;
import core.scene.Scene;

import java.util.ArrayList;

public class SceneRuntime {

    private final Scene scene;

    public SceneRuntime(Scene scene) {
        this.scene = scene;
    }

    public void awake() {
        for (GameObject rootObject : new ArrayList<>(scene.getRootObjects())) {
            awakeGameObjectRecursive(rootObject);
        }
    }

    public void start() {
        for (GameObject rootObject : new ArrayList<>(scene.getRootObjects())) {
            startGameObjectRecursive(rootObject);
        }
    }

    public void fixedUpdate() {
        for (GameObject rootObject : new ArrayList<>(scene.getRootObjects())) {
            fixedUpdateGameObjectRecursive(rootObject);
        }
    }

    public void update() {
        for (GameObject rootObject : new ArrayList<>(scene.getRootObjects())) {
            updateGameObjectRecursive(rootObject);
        }
    }

    public void lateUpdate() {
        for (GameObject rootObject : new ArrayList<>(scene.getRootObjects())) {
            lateUpdateGameObjectRecursive(rootObject);
        }
    }

    public void destroy() {
        PhysicsWorld2D.getInstance().clearColliders();

        for (GameObject rootObject : new ArrayList<>(scene.getRootObjects())) {
            destroyGameObjectRecursive(rootObject);
        }
    }

    public void registerAllColliders() {
        PhysicsWorld2D.getInstance().clearColliders();

        for (GameObject rootObject : new ArrayList<>(scene.getRootObjects())) {
            registerCollidersRecursive(rootObject);
        }
    }

    private void awakeGameObjectRecursive(GameObject gameObject) {
        for (Component component : new ArrayList<>(gameObject.getComponents())) {
            if (!component.isAwoken()) {
                component.awake();
                component.setAwoken(true);
            }
        }

        for (GameObject child : new ArrayList<>(gameObject.getChildren())) {
            awakeGameObjectRecursive(child);
        }
    }

    private void startGameObjectRecursive(GameObject gameObject) {
        for (Component component : new ArrayList<>(gameObject.getComponents())) {
            if (component.isEnabled() && !component.isStarted()) {
                component.start();
                component.setStarted(true);
            }
        }

        for (GameObject child : new ArrayList<>(gameObject.getChildren())) {
            startGameObjectRecursive(child);
        }
    }

    private void fixedUpdateGameObjectRecursive(GameObject gameObject) {
        for (Component component : new ArrayList<>(gameObject.getComponents())) {
            if (component.isEnabled()) {
                component.fixedUpdate();
            }
        }

        for (GameObject child : new ArrayList<>(gameObject.getChildren())) {
            fixedUpdateGameObjectRecursive(child);
        }
    }

    private void updateGameObjectRecursive(GameObject gameObject) {
        for (Component component : new ArrayList<>(gameObject.getComponents())) {
            if (component.isEnabled()) {
                component.update();
            }
        }

        for (GameObject child : new ArrayList<>(gameObject.getChildren())) {
            updateGameObjectRecursive(child);
        }
    }

    private void lateUpdateGameObjectRecursive(GameObject gameObject) {
        for (Component component : new ArrayList<>(gameObject.getComponents())) {
            if (component.isEnabled()) {
                component.lateUpdate();
            }
        }

        for (GameObject child : new ArrayList<>(gameObject.getChildren())) {
            lateUpdateGameObjectRecursive(child);
        }
    }

    private void destroyGameObjectRecursive(GameObject gameObject) {
        for (Component component : new ArrayList<>(gameObject.getComponents())) {
            component.onDestroy();
            component.setAwoken(false);
            component.setStarted(false);
        }

        for (GameObject child : new ArrayList<>(gameObject.getChildren())) {
            destroyGameObjectRecursive(child);
        }
    }

    private void registerCollidersRecursive(GameObject gameObject) {
        for (Component component : new ArrayList<>(gameObject.getComponents())) {
            if (component instanceof Collider2D collider) {
                PhysicsWorld2D.getInstance().registerCollider(collider);
            }
        }

        for (GameObject child : new ArrayList<>(gameObject.getChildren())) {
            registerCollidersRecursive(child);
        }
    }
}