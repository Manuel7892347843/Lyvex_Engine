package core.gameobject;

import core.ProjectManager;
import core.scene.Scene;
import core.component.Component;
import core.component.Transform;
import ui.EditorContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class GameObject {
    private String id;
    private String name;
    public final Transform transform;
    private GameObject parent;
    private final List<GameObject> children = new ArrayList<>();
    private final List<Component> components = new ArrayList<>();
    private boolean destroyed = false;
    private EditorContext context;

    public GameObject(EditorContext context) {
        this("GameObject", context);
    }

    public GameObject(String name, EditorContext context) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.transform = new Transform();
        addComponent(this.transform);
        this.context = context;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Transform getTransform() {
        return transform;
    }

    public GameObject getParent() {
        return parent;
    }

    public List<GameObject> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public List<Component> getComponents() {
        return Collections.unmodifiableList(components);
    }

    public void addComponent(Component component) {
        if (component == null) {
            return;
        }

        if (component instanceof Transform newTransform) {
            if (components.contains(transform)) {
                this.transform.setPosition(newTransform.getX(), newTransform.getY());
                this.transform.setRotation(newTransform.getRotation());
                this.transform.setScale(newTransform.getScale().x, newTransform.getScale().y);
            } else {
                components.add(transform);
            }
            transform.setGameObject(this);
            return;
        }

        if (components.contains(component)) {
            return;
        }

        components.add(component);
        component.setGameObject(this);
    }

    public void removeComponent(Component component) {
        if (component == null || component == transform) {
            return;
        }

        if (components.remove(component)) {
            component.onDestroy();
            component.setGameObject(null);
        }
    }

    public <T extends Component> T getComponent(Class<T> type) {
        for (Component component : components) {
            if (type.isInstance(component)) {
                return type.cast(component);
            }
        }
        return null;
    }

    public void addChild(GameObject child) {
        if (child == null || child == this) {
            return;
        }

        // Se era un root object, rimuovilo dalla scena
        if (child.parent == null && context != null && context.getCurrentScene() != null) {
            context.getCurrentScene().removeRootObject(child);
        }

        // Stacca dal parent precedente
        if (child.parent != null) {
            child.parent.children.remove(child);
        }

        child.parent = this;
        if (!children.contains(child)) {
            children.add(child);
        }
    }

    public void removeChild(GameObject child) {
        if (child == null) {
            return;
        }

        if (children.remove(child)) {
            child.parent = null;
            if (context != null && context.getCurrentScene() != null) {
                context.getCurrentScene().addRootObject(child);
            }
        }
    }

    public GameObject findGameObject(String name){
        if (context == null || context.getCurrentScene() == null || name == null) {
            return null;
        }

        for(GameObject obj: context.getCurrentScene().getRootObjects()){
            GameObject found = findGameObjectRecursive(obj, name);
            if(found != null) {
                return found;
            }
        }

        return null;
    }

    private GameObject findGameObjectRecursive(GameObject current, String name) {
        if(current.getName().equals(name)) {
            return current;
        }

        for(GameObject child : current.getChildren()) {
            GameObject found = findGameObjectRecursive(child, name);
            if(found != null) {
                return found;
            }
        }

        return null;
    }

    public boolean isActive(){
        return true;
    }

    public void destroy(EditorContext context){
        if(destroyed)
            return;
        destroyed = true;

        for(GameObject child : new ArrayList<>(children)){
            child.destroy(context);
        }
        children.clear();

        for(Component component: new ArrayList<>(components)){
            component.onDestroy();
        }
        components.clear();

        if(parent != null){
            parent.removeChild(this);
            parent = null;
        }

        transform.setGameObject(null);
        Scene scene = context.getCurrentScene();
        scene.removeRootObject(this);
    }
}