package ui.panels;

import core.assetmanager.AssetManager;
import core.gameobject.GameObject;
import core.math.vector2D;
import core.math.vector3f;
import core.scriptutil.ScriptComponentRegistry;
import core.component.sprite.SpriteLoader;
import core.component.Component;
import core.component.sprite.Sprite;
import core.component.sprite.SpriteComponent;
import core.component.Transform;
import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiInputTextFlags;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImString;
import ui.EditorContext;
import ui.EditorPanel;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class InspectorPanel implements EditorPanel {
    private final int POS_X = 1570;
    private final int POS_Y = 19;
    private final int WIDTH = 350;
    private final int HEIGHT = 637;

    private SpriteComponent spritePickerTarget;

    @Override
    public void init() {
        ImGui.setNextWindowPos(POS_X, POS_Y, ImGuiCond.Always);
        ImGui.setNextWindowSize(WIDTH, HEIGHT, ImGuiCond.Always);
    }

    @Override
    public void draw(EditorContext context) {
        init();

        ImGui.begin("Inspector", ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoCollapse);
        ImGui.text("Inspector");
        ImGui.separator();

        GameObject selected = context.getSelectedGameObject();

        if (selected == null) {
            ImGui.text("No object selected");
            ImGui.end();
            return;
        }

        ImGui.text("Selected GameObject");
        ImGui.separator();
        ImGui.text("ID: " + selected.getId());

        if(ImGui.button("Add GameObject Children")){
            GameObject child = new GameObject("New Child", context);
            context.getCurrentScene().addChildObject(context.getSelectedGameObject(), child);
            context.setSelectedGameObject(child);
            context.setSceneDirty(true);
        }

        if(ImGui.button("Remove GameObject")){
            selected.destroy(context);
            context.setSelectedGameObject(null);
            context.setSceneDirty(true);
        }

        ImString name = new ImString(selected.getName(), 128);
        if (ImGui.inputText("Name", name, ImGuiInputTextFlags.EnterReturnsTrue)) {
            selected.setName(name.get());
            context.setSceneDirty(true);
        }

        ImGui.separator();
        ImGui.text("Components: " + selected.getComponents().size());

        if (ImGui.button("Add Component")) {
            ImGui.openPopup("AddComponentPopup");
        }

        if (ImGui.beginPopup("AddComponentPopup")) {
            for (Class<? extends Component> componentClass : ScriptComponentRegistry.getComponentClasses()) {
                String componentName = componentClass.getSimpleName();
                if (ImGui.menuItem(componentName)) {
                    try {
                        Component component = componentClass.getDeclaredConstructor().newInstance();
                        selected.addComponent(component);
                        context.setSceneDirty(true);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to create component: " + componentName, e);
                    }
                }
            }
            ImGui.endPopup();
        }

        ImGui.separator();
        ImGui.text("Component List:");

        List<Component> removableComponents = new ArrayList<>();

        for (Component component : selected.getComponents()) {
            ImGui.separator();
            ImGui.text(component.getClass().getSimpleName());

            drawComponentFields(component, context);

            if (component instanceof SpriteComponent spriteComponent) {
                drawSpriteField(spriteComponent, context);
            }

            if (component instanceof Transform transform) {
                drawTransformField(transform, context);
                ImGui.text("Transform is required and cannot be removed.");
            } else {
                if (ImGui.button("Remove##" + component.hashCode())) {
                    removableComponents.add(component);
                }
            }
        }

        for (Component component : removableComponents) {
            selected.removeComponent(component);
            context.setSceneDirty(true);
        }

        drawSpritePickerPopup(context);

        ImGui.end();
    }

    private void drawComponentFields(Component component, EditorContext context) {
        Field[] fields = component.getClass().getFields();

        for (Field field : fields) {
            try {
                Class<?> type = field.getType();
                String fieldName = field.getName();
                Object value = field.get(component);

                if(type == vector2D.class || type.getSimpleName().equals("vector2D")){
                    vector2D vec = (vector2D) value;
                    if (vec == null) {
                        vec = new vector2D(0, 0);
                        field.set(component, vec);
                    }

                    float[] v = { vec.x, vec.y };
                    if (ImGui.dragFloat2(fieldName, v, 0.05f)) {
                        vec.x = v[0];
                        vec.y = v[1];
                        context.setSceneDirty(true);
                    }
                    continue;
                }

                if(type == vector3f.class || type.getSimpleName().equals("vector3f")){
                    vector3f vec = (vector3f) value;
                    if(vec == null){
                        vec = new vector3f(0, 0, 0);
                        field.set(component, vec);
                    }

                    float[] v = {vec.x, vec.y, vec.z};
                    if(ImGui.dragFloat3(fieldName, v, 0.05f)){
                        vec.x = v[0];
                        vec.y = v[1];
                        vec.z = v[2];
                        context.setSceneDirty(true);
                    }
                    continue;
                }

                if (type == float.class) {
                    float[] v = { field.getFloat(component) };
                    if (ImGui.dragFloat(fieldName, v, 0.05f)) {
                        field.setFloat(component, v[0]);
                        context.setSceneDirty(true);
                    }
                } else if (type == int.class) {
                    int[] v = { field.getInt(component) };
                    if (ImGui.dragInt(fieldName, v, 1.0f)) {
                        field.setInt(component, v[0]);
                        context.setSceneDirty(true);
                    }
                } else if (type == boolean.class) {
                    boolean current = field.getBoolean(component);
                    if (ImGui.button(fieldName + ": " + (current ? "true" : "false"))) {
                        field.setBoolean(component, !current);
                        context.setSceneDirty(true);
                    }
                } else if (type == String.class) {
                    ImString str = new ImString(value != null ? value.toString() : "", 256);
                    if (ImGui.inputText(fieldName, str)) {
                        field.set(component, str.get());
                        context.setSceneDirty(true);
                    }
                }
            } catch (Exception ignored) {
                ignored.printStackTrace();
            }
        }
    }

    public void drawTransformField(Transform transform, EditorContext context){
        ImGui.separator();
        ImGui.text("Position");
        float[] pos = {transform.getX(), transform.getY()};
        if(ImGui.dragFloat2("##Position", pos, 0.05f)){
            transform.setPosition(pos[0], pos[1]);
            context.setSceneDirty(true);
        }

        ImGui.text("Rotation");
        float[] rot = { transform.getRotation() };
        if (ImGui.dragFloat("##Rotation", rot, 0.5f)) {
            transform.setRotation(rot[0]);
            context.setSceneDirty(true);
        }

        ImGui.text("Scale");
        float[] scale = {transform.getScale().x, transform.getScale().y};
        if(ImGui.dragFloat2("##Scale", scale, 0.05f)){
            transform.setScale(scale[0], scale[1]);
            context.setSceneDirty(true);
        }
    }

    private void drawSpriteField(SpriteComponent spriteComponent, EditorContext context) {
        ImGui.separator();
        ImGui.text("Sprite");

        String spritePath = spriteComponent.getSpriteAssetPath();
        if (spritePath == null || spritePath.isBlank()) {
            ImGui.text("Selected: None");
        } else {
            ImGui.text("Selected: " + spritePath);
        }

        if (ImGui.button("Choose Sprite")) {
            spritePickerTarget = spriteComponent;
            ImGui.openPopup("SpritePickerPopup");
        }
    }

    private void drawSpritePickerPopup(EditorContext context) {
        if (ImGui.beginPopup("SpritePickerPopup")) {
            Path assetsRoot = AssetManager.getAssetPath();

            ImGui.text("Select a sprite from Assets");
            ImGui.separator();

            try (Stream<Path> paths = Files.walk(assetsRoot)) {
                paths.filter(Files::isRegularFile)
                        .filter(this::isImageFile)
                        .forEach(path -> {
                            String relative = assetsRoot.relativize(path).toString().replace('\\', '/');
                            if (ImGui.selectable(relative)) {
                                if (spritePickerTarget != null) {
                                    spritePickerTarget.setSpriteAssetPath(relative);

                                    try {
                                        Path realPath = assetsRoot.resolve(relative);
                                        Sprite sprite = SpriteLoader.loadFromFile(realPath);
                                        spritePickerTarget.setSprite(sprite);
                                    } catch (Exception e) {
                                        throw new RuntimeException("Failed to load sprite: " + relative, e);
                                    }
                                }
                                context.setSceneDirty(true);
                                ImGui.closeCurrentPopup();
                            }
                        });
            } catch (IOException e) {
                ImGui.text("Failed to read assets folder");
            }

            if (ImGui.button("Close")) {
                ImGui.closeCurrentPopup();
            }

            ImGui.endPopup();
        }
    }

    private boolean isImageFile(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".png")
                || name.endsWith(".jpg")
                || name.endsWith(".jpeg")
                || name.endsWith(".bmp")
                || name.endsWith(".gif");
    }

    @Override
    public void optionsMenu() {
    }
}