package core;

import core.component.Component;
import core.component.Transform;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class GameObject {
    private String id;
    private String name;
    private final Transform transform;
    private GameObject parent;
    private final List<GameObject> children = new ArrayList<>();
    private final List<Component> components = new ArrayList<>();

    public GameObject() {
        this("GameObject");
    }

    public GameObject(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.transform = new Transform();
        addComponent(this.transform);
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

        if (component instanceof Transform) {
            if (!components.contains(transform)) {
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

        if (child.parent != null) {
            child.parent.children.remove(child);
        }

        child.parent = this;
        children.add(child);
    }

    public void removeChild(GameObject child) {
        if (child == null) {
            return;
        }

        if (children.remove(child)) {
            child.parent = null;
        }
    }
}