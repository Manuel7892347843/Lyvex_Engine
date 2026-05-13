package core.scene;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import core.scriptutil.ScriptComponentRegistry;
import core.component.sprite.SpriteLoader;
import core.assetmanager.AssetManager;
import core.component.Component;
import core.component.ComponentData;
import core.component.sprite.Sprite;
import core.component.sprite.SpriteComponent;
import core.gameobject.GameObject;
import core.gameobject.GameObjectData;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class SceneSerializer {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    public static void save(Scene scene, Path path) throws IOException {
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }

        SceneData data = toData(scene);
        Files.writeString(path, GSON.toJson(data));
    }

    public static Scene load(Path path) throws IOException {
        String json = Files.readString(path);
        SceneData data = GSON.fromJson(json, SceneData.class);

        if (data == null) {
            return new Scene("Untitled Scene");
        }

        return fromData(data);
    }

    private static SceneData toData(Scene scene) {
        SceneData data = new SceneData();
        data.name = scene.getName();

        for (GameObject root : scene.getRootObjects()) {
            data.rootObjects.add(toData(root));
        }

        return data;
    }

    private static GameObjectData toData(GameObject object) {
        GameObjectData data = new GameObjectData();
        data.id = object.getId();
        data.name = object.getName();

        for (Component component : object.getComponents()) {
            data.components.add(toComponentData(component));
        }

        for (GameObject child : object.getChildren()) {
            data.children.add(toData(child));
        }

        return data;
    }

    private static ComponentData toComponentData(Component component) {
        ComponentData data = new ComponentData();

        data.type = component.getClass().getName();

        Field[] fields = component.getClass().getDeclaredFields();
        for (Field field : fields) {
            try {
                field.setAccessible(true);
                Object value = field.get(component);

                if ("sprite".equals(field.getName())) {
                    continue;
                }

                data.fields.put(field.getName(), value);
            } catch (IllegalAccessException ignored) {
            }
        }

        return data;
    }

    private static Scene fromData(SceneData data) {
        Scene scene = new Scene(data.name != null ? data.name : "Untitled Scene");

        if (data.rootObjects != null) {
            for (GameObjectData rootData : data.rootObjects) {
                scene.addRootObject(fromData(rootData));
            }
        }

        return scene;
    }

    private static GameObject fromData(GameObjectData data) {
        GameObject object = new GameObject(data.name != null ? data.name : "GameObject");

        if (data.id != null && !data.id.isBlank()) {
            object.setId(data.id);
        }

        if (data.components != null) {
            for (ComponentData componentData : data.components) {
                Component component = fromComponentData(componentData);
                if (component != null) {
                    object.addComponent(component);
                }
            }
        }

        if (data.children != null) {
            for (GameObjectData childData : data.children) {
                object.addChild(fromData(childData));
            }
        }

        return object;
    }

    private static Component fromComponentData(ComponentData data) {
        if (data == null || data.type == null || data.type.isBlank()) {
            return null;
        }

        try {
            Class<? extends Component> componentClass = ScriptComponentRegistry.findComponentClass(data.type);
            if (componentClass == null) {
                System.err.println("Component class not found: " + data.type);
                return null;
            }

            Component component = componentClass.getDeclaredConstructor().newInstance();

            if (data.fields != null) {
                applyFields(component, data.fields);
            }

            if (component instanceof SpriteComponent spriteComponent) {
                String assetPath = spriteComponent.getSpriteAssetPath();
                if (assetPath != null && !assetPath.isBlank()) {
                    try {
                        Path realPath = AssetManager.getAssetPath().resolve(assetPath);
                        Sprite sprite = SpriteLoader.loadFromFile(realPath);
                        spriteComponent.setSprite(sprite);
                    } catch (Exception e) {
                        System.err.println("Failed to reload sprite: " + assetPath);
                        e.printStackTrace();
                    }
                }
            }

            return component;
        } catch (Exception e) {
            System.err.println("Failed to restore component: " + data.type);
            e.printStackTrace();
            return null;
        }
    }

    private static void applyFields(Component component, Map<String, Object> fields) {
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            try {
                Field field = component.getClass().getDeclaredField(entry.getKey());
                field.setAccessible(true);
                Class<?> type = field.getType();
                Object value = entry.getValue();

                if (type == float.class && value instanceof Number number) {
                    field.setFloat(component, number.floatValue());
                } else if (type == int.class && value instanceof Number number) {
                    field.setInt(component, number.intValue());
                } else if (type == boolean.class && value instanceof Boolean bool) {
                    field.setBoolean(component, bool);
                } else if (type == String.class) {
                    field.set(component, value != null ? value.toString() : null);
                }
            } catch (Exception ignored) {
            }
        }
    }
}