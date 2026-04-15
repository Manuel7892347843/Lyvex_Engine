package ui.panels;

import core.ScriptComponentRegistry;
import core.GameObject;
import core.component.Component;
import core.component.Transform;
import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiInputTextFlags;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImString;
import ui.EditorContext;
import ui.EditorPanel;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class InspectorPanel implements EditorPanel {
    private final int POS_X = 1570;
    private final int POS_Y = 19;
    private final int WIDTH = 350;
    private final int HEIGHT = 637;

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

            if (component instanceof Transform) {
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

        ImGui.end();
    }

    private void drawComponentFields(Component component, EditorContext context) {
        Field[] fields = component.getClass().getFields();

        for (Field field : fields) {
            try {
                Class<?> type = field.getType();
                String fieldName = field.getName();
                Object value = field.get(component);

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
            }
        }
    }

    @Override
    public void optionsMenu() {
    }
}