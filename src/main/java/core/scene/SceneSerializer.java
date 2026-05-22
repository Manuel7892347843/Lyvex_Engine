package core.scene;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import core.component.Transform;
import core.component.tilemap.Tilemap;
import core.component.ui.color.UIColor;
import core.component.ui.uiElements.UIButton;
import core.component.ui.uiElements.UIImage;
import core.component.ui.uiElements.UIPanel;
import core.component.ui.uiElements.UIText;
import core.lib.math.vector2D;
import core.scriptutil.ScriptComponentRegistry;
import core.component.sprite.SpriteLoader;
import core.assetmanager.AssetManager;
import core.component.Component;
import core.component.ComponentData;
import core.component.sprite.Sprite;
import core.component.sprite.SpriteComponent;
import core.gameobject.GameObject;
import core.gameobject.GameObjectData;
import ui.EditorContext;

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

        if (component instanceof Transform transform) {
            data.fields.put("x", transform.getX());
            data.fields.put("y", transform.getY());
            data.fields.put("rotation", transform.getRotation());
            data.fields.put("scaleX", transform.getScale().x);
            data.fields.put("scaleY", transform.getScale().y);
            return data;
        }

        if (component instanceof SpriteComponent sc) {
            data.fields.put("spriteAssetPath", sc.getSpriteAssetPath());
            data.fields.put("sortingLayer", sc.getSortingLayer());
            data.fields.put("sortingOrder", sc.getSortingOrder());
            return data;
        }

        if (component instanceof Tilemap tilemap) {
            data.fields.put("tileSize", tilemap.getTileSize());
            data.fields.put("pixelsPerUnit", tilemap.getPixelsPerUnit());
            data.fields.put("tilesetPath", tilemap.getTilesetPath());
            data.fields.put("sortingLayer", tilemap.getSortingLayer());
            data.fields.put("sortingOrder", tilemap.getSortingOrder());

            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Integer> entry : tilemap.getTiles().entrySet()) {
                if (sb.length() > 0) sb.append(";");
                sb.append(entry.getKey()).append(":").append(entry.getValue());
            }
            data.fields.put("tiles", sb.toString());
            return data;
        }

        if (component instanceof UIButton btn) {
            data.fields.put("UIx", btn.getX());
            data.fields.put("UIy", btn.getY());
            data.fields.put("UIwidth", btn.getWidth());
            data.fields.put("UIheight", btn.getHeight());
            data.fields.put("UIpivotX", btn.getPivotX());
            data.fields.put("UIpivotY", btn.getPivotY());
            data.fields.put("UIorder", btn.getOrder());
            data.fields.put("visible", btn.isVisible());
            data.fields.put("normalColor", btn.getNormalColor());
            data.fields.put("hoverColor", btn.getHoverColor());
            data.fields.put("pressedColor", btn.getPressedColor());
            data.fields.put("normalSpritePath", btn.getNormalSpritePath());
            data.fields.put("hoverSpritePath", btn.getHoverSpritePath());
            data.fields.put("pressedSpritePath", btn.getPressedSpritePath());

            return data;
        }

        if(component instanceof UIPanel panel){
            data.fields.put("UIx", panel.getX());
            data.fields.put("UIy", panel.getY());
            data.fields.put("UIwidth", panel.getWidth());
            data.fields.put("UIheight", panel.getHeight());
            data.fields.put("UIpivotX", panel.getPivotX());
            data.fields.put("UIpivotY", panel.getPivotY());
            data.fields.put("UIorder", panel.getOrder());
            data.fields.put("visible", panel.isVisible());
            data.fields.put("color", panel.getColor());
            return data;
        }

        if(component instanceof UIText txt){
            data.fields.put("UIx", txt.getX());
            data.fields.put("UIy", txt.getY());
            data.fields.put("UIwidth", txt.getWidth());
            data.fields.put("UIheight", txt.getHeight());
            data.fields.put("UIpivotX", txt.getPivotX());
            data.fields.put("UIpivotY", txt.getPivotY());
            data.fields.put("UIorder", txt.getOrder());
            data.fields.put("visible", txt.isVisible());
            data.fields.put("text", txt.getText());
            data.fields.put("fontSize", txt.getFontSize());
            data.fields.put("fontName", txt.getFontName());
            data.fields.put("fontAssetPath", txt.getFontAssetPath());
            data.fields.put("color", txt.getColor());
            return data;
        }

        if (component instanceof UIImage image) {
            data.fields.put("UIx", image.getX());
            data.fields.put("UIy", image.getY());
            data.fields.put("UIwidth", image.getWidth());
            data.fields.put("UIheight", image.getHeight());
            data.fields.put("UIpivotX", image.getPivotX());
            data.fields.put("UIpivotY", image.getPivotY());
            data.fields.put("UIorder", image.getOrder());
            data.fields.put("visible", image.isVisible());
            data.fields.put("tint", image.getTint());
            data.fields.put("spriteAssetPath", image.getSpriteAssetPath());
            return data;
        }

        Field[] fields = component.getClass().getDeclaredFields();
        for (Field field : fields) {
            try {
                field.setAccessible(true);
                Object value = field.get(component);

                if ("sprite".equals(field.getName())) {
                    continue;
                }

                if (value != null && !isPrimitiveOrWrapper(value.getClass())) {
                    continue;
                }

                data.fields.put(field.getName(), value);
            } catch (IllegalAccessException ignored) {
            }
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

        ComponentData transformData = null;
        if (data.components != null) {
            for (ComponentData cd : data.components) {
                if ("core.component.Transform".equals(cd.type)) {
                    transformData = cd;
                    break;
                }
            }
        }

        if (data.components != null) {
            for (ComponentData componentData : data.components) {
                Component component = fromComponentData(componentData);
                if (component != null) {
                    object.addComponent(component);
                }
            }
        }

        if (transformData != null && transformData.fields != null) {
            Map<String, Object> fields = transformData.fields;
            Transform t = object.getTransform();
            if (fields.containsKey("x") && fields.get("x") instanceof Number n) {
                t.setPosition(n.floatValue(), t.getY());
            }
            if (fields.containsKey("y") && fields.get("y") instanceof Number n) {
                t.setPosition(t.getX(), n.floatValue());
            }
            if (fields.containsKey("rotation") && fields.get("rotation") instanceof Number n) {
                t.setRotation(n.floatValue());
            }
            if (fields.containsKey("scaleX") && fields.get("scaleX") instanceof Number n) {
                vector2D currentScale = t.getScale();
                t.setScale(n.floatValue(), currentScale.y);
            }
            if (fields.containsKey("scaleY") && fields.get("scaleY") instanceof Number n) {
                vector2D currentScale = t.getScale();
                t.setScale(currentScale.x, n.floatValue());
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
        if (component instanceof Transform transform) {
            if (fields.containsKey("x") && fields.get("x") instanceof Number n) {
                transform.setPosition(n.floatValue(), transform.getY());
            }
            if (fields.containsKey("y") && fields.get("y") instanceof Number n) {
                transform.setPosition(transform.getX(), n.floatValue());
            }
            if (fields.containsKey("rotation") && fields.get("rotation") instanceof Number n) {
                transform.setRotation(n.floatValue());
            }
            if (fields.containsKey("scaleX") && fields.get("scaleX") instanceof Number n) {
                vector2D currentScale = transform.getScale();
                transform.setScale(n.floatValue(), currentScale.y);
            }
            if (fields.containsKey("scaleY") && fields.get("scaleY") instanceof Number n) {
                vector2D currentScale = transform.getScale();
                transform.setScale(currentScale.x, n.floatValue());
            }
            return;
        }

        if (component instanceof SpriteComponent sc) {
            if (fields.containsKey("spriteAssetPath") && fields.get("spriteAssetPath") instanceof String s) {
                sc.setSpriteAssetPath(s);
                if (s != null && !s.isBlank()) {
                    try {
                        Path realPath = AssetManager.getAssetPath().resolve(s);
                        Sprite sprite = SpriteLoader.loadFromFile(realPath);
                        sc.setSprite(sprite);
                    } catch (Exception e) {
                        System.err.println("Failed to reload sprite: " + s);
                    }
                }
            }
            if (fields.containsKey("sortingLayer") && fields.get("sortingLayer") instanceof String s) {
                sc.setSortingLayer(s);
            }
            if (fields.containsKey("sortingOrder") && fields.get("sortingOrder") instanceof Number n) {
                sc.setSortingOrder(n.intValue());
            }
            return;
        }

        if (component instanceof Tilemap tilemap) {
            if (fields.containsKey("tileSize") && fields.get("tileSize") instanceof Number n) {
                tilemap.setTileSize(n.intValue());
            }
            if (fields.containsKey("pixelsPerUnit") && fields.get("pixelsPerUnit") instanceof Number n) {
                tilemap.setPixelsPerUnit(n.floatValue());
            }
            if (fields.containsKey("tilesetPath") && fields.get("tilesetPath") instanceof String s) {
                tilemap.setTilesetPath(s);
                tilemap.loadTileset();
            }
            if (fields.containsKey("sortingLayer") && fields.get("sortingLayer") instanceof String s) {
                tilemap.setSortingLayer(s);
            }
            if (fields.containsKey("sortingOrder") && fields.get("sortingOrder") instanceof Number n) {
                tilemap.setSortingOrder(n.intValue());
            }
            if (fields.containsKey("tiles") && fields.get("tiles") instanceof String tilesStr) {
                String[] entries = tilesStr.split(";");
                for (String entry : entries) {
                    if (entry.isBlank()) continue;
                    String[] kv = entry.split(":");
                    String[] xy = kv[0].split(",");
                    int x = Integer.parseInt(xy[0]);
                    int y = Integer.parseInt(xy[1]);
                    int id = Integer.parseInt(kv[1]);
                    tilemap.setTile(x, y, id);
                }
            }
            return;
        }

        if (component instanceof UIButton btn) {
            if (fields.containsKey("UIx") && fields.get("UIx") instanceof Number n)
                btn.setX(n.floatValue());
            if (fields.containsKey("UIy") && fields.get("UIy") instanceof Number n)
                btn.setY(n.floatValue());
            if (fields.containsKey("UIwidth") && fields.get("UIwidth") instanceof Number n)
                btn.setWidth(n.floatValue());
            if (fields.containsKey("UIheight") && fields.get("UIheight") instanceof Number n)
                btn.setHeight(n.floatValue());
            if (fields.containsKey("visible") && fields.get("visible") instanceof Boolean b)
                btn.setVisible(b);
            if (fields.containsKey("UIorder") && fields.get("UIorder") instanceof Number n)
                btn.setOrder(n.intValue());
            if (fields.containsKey("UIpivotX") && fields.get("UIpivotX") instanceof Number n)
                btn.setPivotX(n.floatValue());
            if (fields.containsKey("UIpivotY") && fields.get("UIpivotY") instanceof Number n)
                btn.setPivotY(n.floatValue());
            if (fields.containsKey("normalSpritePath") && fields.get("normalSpritePath") instanceof String s)
                btn.setNormalSpritePath(s);
            if (fields.containsKey("hoverSpritePath") && fields.get("hoverSpritePath") instanceof String s)
                btn.setHoverSpritePath(s);
            if (fields.containsKey("pressedSpritePath") && fields.get("pressedSpritePath") instanceof String s)
                btn.setPressedSpritePath(s);

            btn.setNormalColor(extractUIColor(fields, "normalColor"));
            btn.setHoverColor(extractUIColor(fields, "hoverColor"));
            btn.setPressedColor(extractUIColor(fields, "pressedColor"));

            return;
        }

        if(component instanceof UIPanel panel){
            if (fields.containsKey("UIx") && fields.get("UIx") instanceof Number n)
                panel.setX(n.floatValue());
            if (fields.containsKey("UIy") && fields.get("UIy") instanceof Number n)
                panel.setY(n.floatValue());
            if (fields.containsKey("UIwidth") && fields.get("UIwidth") instanceof Number n)
                panel.setWidth(n.floatValue());
            if (fields.containsKey("UIheight") && fields.get("UIheight") instanceof Number n)
                panel.setHeight(n.floatValue());
            if(fields.containsKey("visible") && fields.get("visible") instanceof Boolean b)
                panel.setVisible(b);
            if (fields.containsKey("UIorder") && fields.get("UIorder") instanceof Number n)
                panel.setOrder(n.intValue());
            if (fields.containsKey("UIpivotX") && fields.get("UIpivotX") instanceof Number n)
                panel.setPivotX(n.floatValue());
            if (fields.containsKey("UIpivotY") && fields.get("UIpivotY") instanceof Number n)
                panel.setPivotY(n.floatValue());

            panel.setColor(extractUIColor(fields, "color"));

            return;
        }

        if(component instanceof UIText txt){
            if (fields.containsKey("UIx") && fields.get("UIx") instanceof Number n)
                txt.setX(n.floatValue());
            if (fields.containsKey("UIy") && fields.get("UIy") instanceof Number n)
                txt.setY(n.floatValue());
            if (fields.containsKey("UIwidth") && fields.get("UIwidth") instanceof Number n)
                txt.setWidth(n.floatValue());
            if (fields.containsKey("UIheight") && fields.get("UIheight") instanceof Number n)
                txt.setHeight(n.floatValue());
            if(fields.containsKey("visible") && fields.get("visible") instanceof Boolean b)
                txt.setVisible(b);
            if (fields.containsKey("UIorder") && fields.get("UIorder") instanceof Number n)
                txt.setOrder(n.intValue());
            if (fields.containsKey("UIpivotX") && fields.get("UIpivotX") instanceof Number n)
                txt.setPivotX(n.floatValue());
            if (fields.containsKey("UIpivotY") && fields.get("UIpivotY") instanceof Number n)
                txt.setPivotY(n.floatValue());
            if(fields.containsKey("text") && fields.get("text") instanceof String s)
                txt.setText((String) fields.get("text"));
            if(fields.containsKey("fontSize") && fields.get("fontSize") instanceof Number n)
                txt.setFontSize(n.intValue());
            if(fields.containsKey("fontName") && fields.get("fontName") instanceof String s)
                txt.setFontName((String) fields.get("fontName"));
            if(fields.containsKey("fontAssetPath") && fields.get("fontAssetPath") instanceof String s)
                txt.setFontAssetPath((String) fields.get("fontAssetPath"));

            txt.setColor(extractUIColor(fields, "color"));

            return;
        }

        if (component instanceof UIImage image) {
            if (fields.containsKey("UIx") && fields.get("UIx") instanceof Number n)
                image.setX(n.floatValue());
            if (fields.containsKey("UIy") && fields.get("UIy") instanceof Number n)
                image.setY(n.floatValue());
            if (fields.containsKey("UIwidth") && fields.get("UIwidth") instanceof Number n)
                image.setWidth(n.floatValue());
            if (fields.containsKey("UIheight") && fields.get("UIheight") instanceof Number n)
                image.setHeight(n.floatValue());
            if (fields.containsKey("visible") && fields.get("visible") instanceof Boolean b)
                image.setVisible(b);
            if (fields.containsKey("UIorder") && fields.get("UIorder") instanceof Number n)
                image.setOrder(n.intValue());
            if (fields.containsKey("UIpivotX") && fields.get("UIpivotX") instanceof Number n)
                image.setPivotX(n.floatValue());
            if (fields.containsKey("UIpivotY") && fields.get("UIpivotY") instanceof Number n)
                image.setPivotY(n.floatValue());
            if (fields.containsKey("spriteAssetPath") && fields.get("spriteAssetPath") instanceof String s)
                image.setSpriteAssetPath(s);

            image.setTint(extractUIColor(fields, "tint"));

            return;
        }

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