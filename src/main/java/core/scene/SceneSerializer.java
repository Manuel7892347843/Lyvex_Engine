package core.scene;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import core.component.Component;
import core.component.ComponentData;
import core.component.Transform;
import core.component.RigidBody2D;
import core.component.tilemap.Tilemap;
import core.component.ui.UIElement;
import core.component.ui.color.UIColor;
import core.component.ui.uiElements.UIButton;
import core.component.ui.uiElements.UIImage;
import core.component.ui.uiElements.UIPanel;
import core.component.ui.uiElements.UIText;
import core.component.sprite.Sprite;
import core.component.sprite.SpriteComponent;
import core.component.sprite.SpriteLoader;
import core.assetmanager.AssetManager;
import core.gameobject.GameObject;
import core.gameobject.GameObjectData;
import core.scriptutil.ScriptComponentRegistry;
import ui.EditorContext;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
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

    public static Scene clone(Scene scene) {
        if (scene == null) {
            return null;
        }

        SceneData data = toData(scene);
        String json = GSON.toJson(data);
        SceneData clonedData = GSON.fromJson(json, SceneData.class);

        return fromData(clonedData);
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

        Class<?> clazz = component.getClass();
        while (clazz != null && clazz != Object.class) {
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                try {
                    field.setAccessible(true);
                    String fieldName = field.getName();
                    Object value = field.get(component);

                    // Salta null, static, final, transient
                    if (value == null) continue;
                    int mods = field.getModifiers();
                    if (Modifier.isStatic(mods) || Modifier.isTransient(mods)) continue;

                    Class<?> type = value.getClass();

                    // Tipi primitivi / wrapper
                    if (isPrimitiveOrWrapper(type)) {
                        data.fields.put(fieldName, value);
                        continue;
                    }

                    // vector2f / vector2D → serializza come stringa "x,y"
                    if (value instanceof core.lib.math.vector2f v2f) {
                        data.fields.put(fieldName, v2f.x + "," + v2f.y);
                        continue;
                    }
                    if (value instanceof core.lib.math.vector2D v2d) {
                        data.fields.put(fieldName, v2d.x + "," + v2d.y);
                        continue;
                    }

                    // UIColor → serializza come mappa
                    if (value instanceof UIColor c) {
                        data.fields.put(fieldName, Map.of("r", c.r, "g", c.g, "b", c.b, "a", c.a));
                        continue;
                    }

                    // Tilemap tiles → serializza come stringa "x,y:id;..."
                    if (component instanceof Tilemap tilemap && fieldName.equals("tiles")) {
                        StringBuilder sb = new StringBuilder();
                        for (Map.Entry<String, Integer> entry : tilemap.getTiles().entrySet()) {
                            if (sb.length() > 0) sb.append(";");
                            sb.append(entry.getKey()).append(":").append(entry.getValue());
                        }
                        data.fields.put(fieldName, sb.toString());
                        continue;
                    }

                    // SpriteComponent: salva solo il path, non l'oggetto Sprite
                    if (component instanceof SpriteComponent sc && fieldName.equals("sprite")) {
                        data.fields.put("spriteAssetPath", sc.getSpriteAssetPath());
                        continue;
                    }

                    // UIButton: salva solo i path, non gli oggetti Sprite
                    if (component instanceof UIButton btn) {
                        if (fieldName.equals("normalSprite")) {
                            data.fields.put("normalSpritePath", btn.getNormalSpritePath());
                            continue;
                        }
                        if (fieldName.equals("hoverSprite")) {
                            data.fields.put("hoverSpritePath", btn.getHoverSpritePath());
                            continue;
                        }
                        if (fieldName.equals("pressedSprite")) {
                            data.fields.put("pressedSpritePath", btn.getPressedSpritePath());
                            continue;
                        }
                    }

                    // UIImage: salva solo il path, non l'oggetto Sprite
                    if (component instanceof UIImage img && fieldName.equals("sprite")) {
                        data.fields.put("spriteAssetPath", img.getSpriteAssetPath());
                        continue;
                    }

                    // UIText: salva solo i dati, non lo sprite cached
                    if (component instanceof UIText txt && fieldName.equals("cachedSprite")) {
                        continue;
                    }

                    // Per gli altri tipi complessi, ignora
                } catch (IllegalAccessException ignored) {
                }
            }
            clazz = clazz.getSuperclass();
        }

        return data;
    }

    private static boolean isPrimitiveOrWrapper(Class<?> type) {
        return type.isPrimitive() ||
                type == String.class ||
                type == Float.class || type == Double.class ||
                type == Integer.class || type == Long.class ||
                type == Short.class || type == Byte.class ||
                type == Boolean.class || type == Character.class;
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
        GameObject object = new GameObject(data.name != null ? data.name : "GameObject", EditorContext.getInstance());

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

            // Post-init per SpriteComponent (ricarica lo sprite)
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

            // Post-init per Tilemap (ricarica il tileset)
            if (component instanceof Tilemap tilemap) {
                tilemap.loadTileset();
            }

            // Post-init per UIButton (ricarica gli sprite)
            if (component instanceof UIButton btn) {
                btn.setNormalSpritePath(btn.getNormalSpritePath());
                btn.setHoverSpritePath(btn.getHoverSpritePath());
                btn.setPressedSpritePath(btn.getPressedSpritePath());
            }

            // Post-init per UIImage (ricarica lo sprite)
            if (component instanceof UIImage img) {
                img.setSpriteAssetPath(img.getSpriteAssetPath());
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
            String key = entry.getKey();
            Object value = entry.getValue();

            // Normalizza il nome: rimuovi prefisso "UI" se presente (compatibilità dati vecchi)
            String normalizedKey = key;
            if (key.startsWith("UI") && key.length() > 2) {
                normalizedKey = Character.toLowerCase(key.charAt(2)) + key.substring(3);
            }

            Field field = findField(component.getClass(), normalizedKey);

            if (field != null) {
                try {
                    field.setAccessible(true);
                    Class<?> type = field.getType();

                    // Tipi primitivi / wrapper
                    if (type == float.class && value instanceof Number n) {
                        field.setFloat(component, n.floatValue());
                    } else if (type == int.class && value instanceof Number n) {
                        field.setInt(component, n.intValue());
                    } else if (type == boolean.class && value instanceof Boolean b) {
                        field.setBoolean(component, b);
                    } else if (type == String.class) {
                        field.set(component, value != null ? value.toString() : null);
                    } else if (type == double.class && value instanceof Number n) {
                        field.setDouble(component, n.doubleValue());
                    } else if (type == long.class && value instanceof Number n) {
                        field.setLong(component, n.longValue());
                    }
                    // vector2f
                    else if (type == core.lib.math.vector2f.class && value instanceof String s) {
                        String[] parts = s.split(",");
                        if (parts.length == 2) {
                            float x = Float.parseFloat(parts[0]);
                            float y = Float.parseFloat(parts[1]);
                            field.set(component, new core.lib.math.vector2f(x, y));
                        }
                    }
                    // vector2D
                    else if (type == core.lib.math.vector2D.class && value instanceof String s) {
                        String[] parts = s.split(",");
                        if (parts.length == 2) {
                            float x = Float.parseFloat(parts[0]);
                            float y = Float.parseFloat(parts[1]);
                            field.set(component, new core.lib.math.vector2D(x, y));
                        }
                    }
                    // UIColor
                    else if (type == UIColor.class && value instanceof Map<?, ?> map) {
                        float r = 1.0f, g = 1.0f, b = 1.0f, a = 1.0f;
                        if (map.get("r") instanceof Number n) r = n.floatValue();
                        if (map.get("g") instanceof Number n) g = n.floatValue();
                        if (map.get("b") instanceof Number n) b = n.floatValue();
                        if (map.get("a") instanceof Number n) a = n.floatValue();
                        field.set(component, new UIColor(r, g, b, a));
                    }
                    // Tilemap tiles string
                    else if (component instanceof Tilemap tilemap && normalizedKey.equals("tiles") && value instanceof String tilesStr) {
                        String[] entries = tilesStr.split(";");
                        for (String tileEntry : entries) {
                            if (tileEntry.isBlank()) continue;
                            String[] kv = tileEntry.split(":");
                            String[] xy = kv[0].split(",");
                            int x = Integer.parseInt(xy[0]);
                            int y = Integer.parseInt(xy[1]);
                            int id = Integer.parseInt(kv[1]);
                            tilemap.setTile(x, y, id);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Failed to apply field " + key + " to " + component.getClass().getSimpleName() + ": " + e.getMessage());
                }
            } else {
                // Campo non trovato: prova il setter
                applyViaSetter(component, normalizedKey, value);
            }
        }
    }

    private static Field findField(Class<?> clazz, String name) {
        while (clazz != null && clazz != Object.class) {
            try {
                return clazz.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }

    private static void applyViaSetter(Component component, String key, Object value) {
        String setterName = "set" + Character.toUpperCase(key.charAt(0)) + key.substring(1);
        try {
            for (var method : component.getClass().getMethods()) {
                if (method.getName().equals(setterName) && method.getParameterCount() == 1) {
                    Class<?> paramType = method.getParameterTypes()[0];
                    if (paramType == float.class && value instanceof Number n) {
                        method.invoke(component, n.floatValue());
                    } else if (paramType == int.class && value instanceof Number n) {
                        method.invoke(component, n.intValue());
                    } else if (paramType == boolean.class && value instanceof Boolean b) {
                        method.invoke(component, b);
                    } else if (paramType == String.class && value instanceof String s) {
                        method.invoke(component, s);
                    } else if (paramType == UIColor.class && value instanceof Map<?, ?> map) {
                        float r = 1.0f, g = 1.0f, b = 1.0f, a = 1.0f;
                        if (map.get("r") instanceof Number n) r = n.floatValue();
                        if (map.get("g") instanceof Number n) g = n.floatValue();
                        if (map.get("b") instanceof Number n) b = n.floatValue();
                        if (map.get("a") instanceof Number n) a = n.floatValue();
                        method.invoke(component, new UIColor(r, g, b, a));
                    }
                    return;
                }
            }
        } catch (Exception ignored) {
        }
    }

    public static GameObject cloneGameObject(GameObject gameObject) {
        if (gameObject == null) {
            return null;
        }

        GameObjectData data = toData(gameObject);
        data.id = java.util.UUID.randomUUID().toString();
        data.name = data.name + " Copy";

        return fromDataWithNewIds(data);
    }

    private static GameObject fromDataWithNewIds(GameObjectData data) {
        GameObject object = fromData(data);
        assignNewIdsRecursive(object);
        return object;
    }

    private static void assignNewIdsRecursive(GameObject object) {
        object.setId(java.util.UUID.randomUUID().toString());

        for (GameObject child : object.getChildren()) {
            assignNewIdsRecursive(child);
        }
    }

    private static UIColor extractUIColor(Map<String, Object> fields, String key) {
        Object value = fields.get(key);
        if (value == null) return null;

        if (value instanceof Map<?, ?> map) {
            float r = 1.0f, g = 1.0f, b = 1.0f, a = 1.0f;

            if (map.get("r") instanceof Number n) r = n.floatValue();
            if (map.get("g") instanceof Number n) g = n.floatValue();
            if (map.get("b") instanceof Number n) b = n.floatValue();
            if (map.get("a") instanceof Number n) a = n.floatValue();

            return new UIColor(r, g, b, a);
        }

        return null;
    }
}