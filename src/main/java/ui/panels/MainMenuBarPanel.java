package ui.panels;

import core.Engine;
import core.ProjectManager;
import core.ProjectSettings;
import core.sorting.SortingLayerManager;
import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiDir;
import imgui.type.ImBoolean;
import imgui.type.ImString;
import ui.EditorContext;
import ui.EditorPanel;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.nio.file.Path;
import java.util.List;

public class MainMenuBarPanel implements EditorPanel {
    private final int POS_X = 0;
    private final int POS_Y = 0;
    private final int WIDTH = 1920;
    private final int HEIGHT = 10;

    private ImBoolean projectSettingsOpen = new ImBoolean(false);
    private int selectedSettingsTab = 0;
    private final ImString newLayerName = new ImString("", 64);

    @Override
    public void init(){
        ImGui.setWindowPos(POS_X, POS_Y);
        ImGui.setWindowSize(WIDTH, HEIGHT);
    }

    @Override
    public void draw(EditorContext context) {
        init();
        if (ImGui.beginMainMenuBar()) {
            if (ImGui.beginMenu("File")) {
                if (ImGui.menuItem("Open")) {
                    JFileChooser chooser = new JFileChooser();
                    chooser.setDialogTitle("Open Scene");
                    chooser.setCurrentDirectory(ProjectManager.getScenesPath().toFile());
                    chooser.setFileFilter(new FileNameExtensionFilter("Lyvex Scene (*.lyvexscene)", "lyvexscene"));

                    int result = chooser.showOpenDialog(null);
                    if (result == JFileChooser.APPROVE_OPTION) {
                        Path selectedScene = chooser.getSelectedFile().toPath();
                        context.getEngine().openScene(selectedScene);
                    }
                }

                if (ImGui.menuItem("Save")) {
                    Engine.saveCurrentScenePublic();
                }

                ImGui.endMenu();
            }

            if (ImGui.beginMenu("Edit")) {
                ImGui.endMenu();
            }

            if (ImGui.beginMenu("Project")) {
                if (ImGui.menuItem("Project Settings")) {
                    projectSettingsOpen.set(true);
                }
                ImGui.endMenu();
            }

            ImGui.endMainMenuBar();
        }

        if (projectSettingsOpen.get()) {
            drawProjectSettingsWindow(context);
        }
    }

    private void drawProjectSettingsWindow(EditorContext context) {
        ImGui.setNextWindowSize(600, 400, ImGuiCond.FirstUseEver);

        if (ImGui.begin("Project Settings", projectSettingsOpen)) {
            float sidebarWidth = 150;
            ImGui.beginChild("SettingsSidebar", sidebarWidth, 0, true);

            String[] tabs = {"Sorting Layers", "Physics", "Input", "Audio"};
            for (int i = 0; i < tabs.length; i++) {
                if (ImGui.selectable(tabs[i], selectedSettingsTab == i)) {
                    selectedSettingsTab = i;
                }
            }

            ImGui.endChild();
            ImGui.sameLine();

            ImGui.beginChild("SettingsContent", 0, 0, false);

            switch (selectedSettingsTab) {
                case 0 -> drawSortingLayersSettings(context);
                case 1 -> drawPhysicsSettings(context);
                case 2 -> drawInputSettings(context);
                case 3 -> drawAudioSettings(context);
            }

            ImGui.endChild();
        }
        ImGui.end();
    }

    private void drawSortingLayersSettings(EditorContext context) {
        ImGui.text("Sorting Layers");
        ImGui.separator();

        SortingLayerManager manager = ProjectSettings.getSortingLayerManager();
        List<String> layers = manager.getLayers();

        ImGui.text("Layers:");
        ImGui.separator();

        int layerToRemove = -1;

        for (int i = 0; i < layers.size(); i++) {
            ImGui.pushID("layer_" + i);

            boolean isDefault = layers.get(i).equals("Default");

            ImGui.text(String.valueOf(i));
            ImGui.sameLine();

            if (i > 0) {
                if (ImGui.arrowButton("##up", ImGuiDir.Up)) {
                    manager.moveLayerUp(i);
                    context.setSceneDirty(true);
                }
                ImGui.sameLine();
            } else {
                ImGui.dummy(20, 0);
                ImGui.sameLine();
            }

            if (i < layers.size() - 1) {
                if (ImGui.arrowButton("##down", ImGuiDir.Down)) {
                    manager.moveLayerDown(i);
                    context.setSceneDirty(true);
                }
                ImGui.sameLine();
            } else {
                ImGui.dummy(20, 0);
                ImGui.sameLine();
            }

            ImString layerName = new ImString(layers.get(i), 64);
            if (ImGui.inputText("##name", layerName)) {
                String name = layerName.get().trim();
                if (!name.isEmpty() && !name.equals(layers.get(i)) && !manager.layerExists(name)) {
                    manager.renameLayer(i, name);
                    context.setSceneDirty(true);
                }
            }
            ImGui.sameLine();

            if (!isDefault) {
                if (ImGui.button("X", 20, 20)) {
                    layerToRemove = i;
                }
            } else {
                ImGui.text("(default)");
            }

            ImGui.popID();
        }

        if (layerToRemove >= 0) {
            manager.removeLayer(layerToRemove);
            context.setSceneDirty(true);
        }

        ImGui.separator();

        ImGui.text("Add New Layer:");
        ImGui.inputText("##newlayer", newLayerName);
        ImGui.sameLine();
        if (ImGui.button("Add")) {
            String name = newLayerName.get().trim();
            if (!name.isEmpty() && !manager.layerExists(name)) {
                manager.addLayer(name);
                context.setSceneDirty(true);
                newLayerName.set("");
            }
        }

        ImGui.separator();
        ImGui.text("Tip: Lower index = rendered first (behind)");
        ImGui.text("Higher index = rendered last (in front)");
    }

    private void drawPhysicsSettings(EditorContext context) {
        ImGui.text("Physics Settings");
        ImGui.separator();
        ImGui.text("Coming soon...");
    }

    private void drawInputSettings(EditorContext context) {
        ImGui.text("Input Settings");
        ImGui.separator();
        ImGui.text("Coming soon...");
    }

    private void drawAudioSettings(EditorContext context) {
        ImGui.text("Audio Settings");
        ImGui.separator();
        ImGui.text("Coming soon...");
    }

    @Override
    public void optionsMenu() {
    }
}