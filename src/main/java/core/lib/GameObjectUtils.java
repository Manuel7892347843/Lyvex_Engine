package core.lib;

import core.component.Component;
import core.gameobject.GameObject;
import core.scene.Scene;
import ui.EditorContext;

import java.util.ArrayList;
import java.util.List;

public final class GameObjectUtils {

    private GameObjectUtils() {}

    public static GameObject create(String name) {
        EditorContext context = EditorContext.getInstance();
        GameObject gameObject = new GameObject(name, context);

        Scene scene = context.getCurrentScene();
        if (scene != null) {
            scene.addRootObject(gameObject);
            context.setSceneDirty(true);
        }

        return gameObject;
    }

    public static GameObject findByName(String name) {
        if (name == null) {
            return null;
        }

        Scene scene = currentScene();
        if (scene == null) {
            return null;
        }

        for (GameObject rootObject : scene.getRootObjects()) {
            GameObject found = findByNameRecursive(rootObject, name);
            if (found != null) {
                return found;
            }
        }

        return null;
    }

    public static List<GameObject> findAllByName(String name) {
        List<GameObject> result = new ArrayList<>();

        if (name == null) {
            return result;
        }

        Scene scene = currentScene();
        if (scene == null) {
            return result;
        }

        for (GameObject rootObject : scene.getRootObjects()) {
            findAllByNameRecursive(rootObject, name, result);
        }

        return result;
    }

    public static <T extends Component> T findComponent(Class<T> componentType) {
        if (componentType == null) {
            return null;
        }

        Scene scene = currentScene();
        if (scene == null) {
            return null;
        }

        for (GameObject rootObject : scene.getRootObjects()) {
            T component = findComponentRecursive(rootObject, componentType);
            if (component != null) {
                return component;
            }
        }

        return null;
    }

    public static <T extends Component> List<T> findComponents(Class<T> componentType) {
        List<T> result = new ArrayList<>();

        if (componentType == null) {
            return result;
        }

        Scene scene = currentScene();
        if (scene == null) {
            return result;
        }

        for (GameObject rootObject : scene.getRootObjects()) {
            findComponentsRecursive(rootObject, componentType, result);
        }

        return result;
    }

    public static void destroy(GameObject gameObject) {
        if (gameObject == null) {
            return;
        }

        EditorContext context = EditorContext.getInstance();
        gameObject.destroy(context);
        context.setSceneDirty(true);

        if (context.getSelectedGameObject() == gameObject) {
            context.setSelectedGameObject(null);
        }
    }

    public static void select(GameObject gameObject) {
        EditorContext.getInstance().setSelectedGameObject(gameObject);
    }

    public static GameObject selected() {
        return EditorContext.getInstance().getSelectedGameObject();
    }

    public static void clearSelection() {
        EditorContext.getInstance().setSelectedGameObject(null);
    }

    public static int count() {
        Scene scene = currentScene();
        if (scene == null) {
            return 0;
        }

        int count = 0;
        for (GameObject rootObject : scene.getRootObjects()) {
            count += countRecursive(rootObject);
        }

        return count;
    }

    private static Scene currentScene() {
        return EditorContext.getInstance().getCurrentScene();
    }

    private static GameObject findByNameRecursive(GameObject current, String name) {
        if (current.getName().equals(name)) {
            return current;
        }

        for (GameObject child : current.getChildren()) {
            GameObject found = findByNameRecursive(child, name);
            if (found != null) {
                return found;
            }
        }

        return null;
    }

    private static void findAllByNameRecursive(GameObject current, String name, List<GameObject> result) {
        if (current.getName().equals(name)) {
            result.add(current);
        }

        for (GameObject child : current.getChildren()) {
            findAllByNameRecursive(child, name, result);
        }
    }

    private static <T extends Component> T findComponentRecursive(GameObject current, Class<T> componentType) {
        T component = current.getComponent(componentType);

        if (component != null) {
            return component;
        }

        for (GameObject child : current.getChildren()) {
            T found = findComponentRecursive(child, componentType);
            if (found != null) {
                return found;
            }
        }

        return null;
    }

    private static <T extends Component> void findComponentsRecursive(GameObject current, Class<T> componentType, List<T> result) {
        T component = current.getComponent(componentType);

        if (component != null) {
            result.add(component);
        }

        for (GameObject child : current.getChildren()) {
            findComponentsRecursive(child, componentType, result);
        }
    }

    private static int countRecursive(GameObject current) {
        int count = 1;

        for (GameObject child : current.getChildren()) {
            count += countRecursive(child);
        }

        return count;
    }
}