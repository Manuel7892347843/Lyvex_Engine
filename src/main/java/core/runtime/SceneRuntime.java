package core.runtime;

import core.component.Component;
import core.gameobject.GameObject;
import core.physics.Collider2D;
import core.physics.PhysicsWorld2D;
import core.scene.Scene;

public class SceneRuntime {

    private final Scene scene;

    public SceneRuntime(Scene scene) {
        this.scene = scene;
    }

    public void awake() {
        for (GameObject rootObject : scene.getRootObjects()) {
            awakeGameObjectRecursive(rootObject);
        }
    }

    public void start() {
        for (GameObject rootObject : scene.getRootObjects()) {
            startGameObjectRecursive(rootObject);
        }
    }

    public void fixedUpdate() {
        for (GameObject rootObject : scene.getRootObjects()) {
            fixedUpdateGameObjectRecursive(rootObject);
        }
    }

    public void update() {
        for (GameObject rootObject : scene.getRootObjects()) {
            updateGameObjectRecursive(rootObject);
        }
    }

    public void lateUpdate() {
        for (GameObject rootObject : scene.getRootObjects()) {
            lateUpdateGameObjectRecursive(rootObject);
        }
    }

    public void destroy() {
        PhysicsWorld2D.getInstance().clearColliders();

        for (GameObject rootObject : scene.getRootObjects()) {
            destroyGameObjectRecursive(rootObject);
        }
    }

    public void registerAllColliders() {
        PhysicsWorld2D.getInstance().clearColliders();

        for (GameObject rootObject : scene.getRootObjects()) {
            registerCollidersRecursive(rootObject);
        }
    }

    private void awakeGameObjectRecursive(GameObject gameObject) {
        for (Component component : gameObject.getComponents()) {
            if (!component.isAwoken()) {
                component.awake();
                component.setAwoken(true);
            }
        }

        for (GameObject child : gameObject.getChildren()) {
            awakeGameObjectRecursive(child);
        }
    }

    private void startGameObjectRecursive(GameObject gameObject) {
        for (Component component : gameObject.getComponents()) {
            if (component.isEnabled() && !component.isStarted()) {
                component.start();
                component.setStarted(true);
            }
        }

        for (GameObject child : gameObject.getChildren()) {
            startGameObjectRecursive(child);
        }
    }

    private void fixedUpdateGameObjectRecursive(GameObject gameObject) {
        for (Component component : gameObject.getComponents()) {
            if (component.isEnabled()) {
                component.fixedUpdate();
            }
        }

        for (GameObject child : gameObject.getChildren()) {
            fixedUpdateGameObjectRecursive(child);
        }
    }

    private void updateGameObjectRecursive(GameObject gameObject) {
        for (Component component : gameObject.getComponents()) {
            if (component.isEnabled()) {
                component.update();
            }
        }

        for (GameObject child : gameObject.getChildren()) {
            updateGameObjectRecursive(child);
        }
    }

    private void lateUpdateGameObjectRecursive(GameObject gameObject) {
        for (Component component : gameObject.getComponents()) {
            if (component.isEnabled()) {
                component.lateUpdate();
            }
        }

        for (GameObject child : gameObject.getChildren()) {
            lateUpdateGameObjectRecursive(child);
        }
    }

    private void destroyGameObjectRecursive(GameObject gameObject) {
        for (Component component : gameObject.getComponents()) {
            component.onDestroy();
            component.setAwoken(false);
            component.setStarted(false);
        }

        for (GameObject child : gameObject.getChildren()) {
            destroyGameObjectRecursive(child);
        }
    }

    private void registerCollidersRecursive(GameObject gameObject) {
        for (Component component : gameObject.getComponents()) {
            if (component instanceof Collider2D collider) {
                PhysicsWorld2D.getInstance().registerCollider(collider);
            }
        }

        for (GameObject child : gameObject.getChildren()) {
            registerCollidersRecursive(child);
        }
    }
}