package ui.panels;

import core.GameObject;
import core.Scene;
import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiTreeNodeFlags;
import imgui.flag.ImGuiWindowFlags;
import ui.EditorContext;
import ui.EditorPanel;

public class HierarchyPanel implements EditorPanel {
    private final int POS_X = 0;
    private final int POS_Y = 0;
    private final int WIDTH = 350;
    private final int HEIGHT = 600;

    @Override
    public void init() {
        ImGui.setNextWindowPos(POS_X, POS_Y, ImGuiCond.Always);
        ImGui.setNextWindowSize(WIDTH, HEIGHT, ImGuiCond.Always);
    }

    @Override
    public void draw(EditorContext context) {
        init();

        ImGui.begin("Hierarchy", ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoCollapse);

        Scene scene = context.getCurrentScene();
        if (scene == null) {
            ImGui.text("No scene loaded");
        } else {
            for (GameObject root : scene.getRootObjects()) {
                drawGameObjectNode(root, context);
            }
        }

        if (ImGui.beginPopupContextWindow()) {
            if (ImGui.menuItem("Create Root Object")) {
                Scene sceneRef = context.getCurrentScene();
                if (sceneRef != null) {
                    GameObject newObject = new GameObject("New GameObject");
                    sceneRef.addRootObject(newObject);
                    context.setSelectedGameObject(newObject);
                    context.setSceneDirty(true);
                }
            }

            if (ImGui.menuItem("Save Scene")) {
                if (context.getEngine() == null) {
                    ImGui.endPopup();
                    ImGui.end();
                    throw new IllegalStateException("Engine is null in EditorContext");
                }

                context.getEngine().saveCurrentScenePublic();
                context.setSceneDirty(false);
            }

            ImGui.endPopup();
        }

        ImGui.end();
    }

    private void drawGameObjectNode(GameObject gameObject, EditorContext context) {
        int flags = ImGuiTreeNodeFlags.OpenOnArrow | ImGuiTreeNodeFlags.SpanAvailWidth;
        if (gameObject.getChildren().isEmpty()) {
            flags |= ImGuiTreeNodeFlags.Leaf;
        }

        boolean opened = ImGui.treeNodeEx(gameObject.getId(), flags, gameObject.getName());

        if (ImGui.isItemClicked()) {
            context.setSelectedGameObject(gameObject);
        }

        if (opened) {
            for (GameObject child : gameObject.getChildren()) {
                drawGameObjectNode(child, context);
            }
            ImGui.treePop();
        }
    }

    @Override
    public void optionsMenu() {
    }
}