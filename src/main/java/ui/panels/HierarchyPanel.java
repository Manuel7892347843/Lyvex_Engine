package ui.panels;

import core.gameobject.GameObject;
import core.scene.Scene;
import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiTreeNodeFlags;
import imgui.flag.ImGuiWindowFlags;
import ui.EditorContext;
import ui.EditorPanel;

public class HierarchyPanel implements EditorPanel {
    private final int POS_X = 0;
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
                    GameObject newObject = new GameObject("New GameObject", context);
                    sceneRef.addRootObject(newObject);
                    context.setSelectedGameObject(newObject);
                    context.setSceneDirty(true);
                }
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

        if (context.getSelectedGameObject() == gameObject) {
            flags |= ImGuiTreeNodeFlags.Selected;
        }

        ImGui.pushID(gameObject.getId());
        boolean opened = ImGui.treeNodeEx(gameObject.getName(), flags, gameObject.getName());
        ImGui.popID();

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