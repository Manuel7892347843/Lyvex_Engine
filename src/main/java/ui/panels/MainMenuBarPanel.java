package ui.panels;

import core.Engine;
import core.ProjectManager;
import core.ProjectSettings;
import core.audio.AudioManager;
import core.lib.SceneManager;
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
    private final ImString addSceneName = new ImString("", 64);
    private final float[] masterVolume = new float[]{1.0f};
    private final ImBoolean audioMuted = new ImBoolean(false);

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

                // --- SCENA ---
                if (ImGui.beginMenu("Scene")) {
                    if (ImGui.menuItem("Open Scene...")) {
                        openSceneDialog(context);
                    }
                    if (ImGui.menuItem("Save Scene", "Ctrl+S")) {
                        Engine.saveCurrentScenePublic();
                    }
                    ImGui.endMenu();
                }

                ImGui.endMenu();
            }

            if (ImGui.beginMenu("Edit")) {
                ImGui.endMenu();
            }

            // --- PROGETTO ---
            if (ImGui.beginMenu("Project")) {
                if (ImGui.menuItem("Save Project", "Ctrl+Shift+S")) {
                    ProjectSettings.save();
                }
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

            String[] tabs = {"Sorting Layers", "Scenes", "Input", "Audio"};
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
                case 1 -> drawScenesSettings(context);
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

    private void drawScenesSettings(EditorContext context) {
        ImGui.text("Scene Build Settings");
        ImGui.separator();

        SceneManager manager = ProjectSettings.getSceneManager();
        List<SceneManager.SceneEntry> scenes = manager.getSceneEntries();

        ImGui.text("Scenes in Build:");
        ImGui.separator();

        int sceneToRemove = -1;
        int sceneToLoad = -1;

        for (int i = 0; i < scenes.size(); i++) {
            ImGui.pushID("scene_" + i);

            ImBoolean inBuild = new ImBoolean(scenes.get(i).inBuild);
            if (ImGui.checkbox("##inbuild", inBuild)) {
                manager.setSceneInBuild(i, inBuild.get());
                context.setSceneDirty(true);
            }
            ImGui.sameLine();

            ImGui.text(String.valueOf(i));
            ImGui.sameLine();

            if (i > 0) {
                if (ImGui.arrowButton("##up", ImGuiDir.Up)) {
                    manager.moveSceneUp(i);
                    context.setSceneDirty(true);
                }
                ImGui.sameLine();
            } else {
                ImGui.dummy(20, 0);
                ImGui.sameLine();
            }

            if (i < scenes.size() - 1) {
                if (ImGui.arrowButton("##down", ImGuiDir.Down)) {
                    manager.moveSceneDown(i);
                    context.setSceneDirty(true);
                }
                ImGui.sameLine();
            } else {
                ImGui.dummy(20, 0);
                ImGui.sameLine();
            }

            ImString sceneName = new ImString(scenes.get(i).name, 64);
            if (ImGui.inputText("##name", sceneName)) {
                manager.renameScene(i, sceneName.get().trim());
                context.setSceneDirty(true);
            }
            ImGui.sameLine();

            if (ImGui.button("Load", 50, 20)) {
                sceneToLoad = i;
            }
            ImGui.sameLine();

            if (ImGui.button("X", 20, 20)) {
                sceneToRemove = i;
            }

            ImGui.textDisabled("  File: " + scenes.get(i).fileName);

            ImGui.popID();
        }

        if (sceneToLoad >= 0) {
            manager.loadScene(sceneToLoad);
            context.setSceneDirty(true);
        }
        if (sceneToRemove >= 0) {
            manager.removeScene(sceneToRemove);
            context.setSceneDirty(true);
        }

        ImGui.separator();

        ImGui.text("Add New Scene:");
        ImGui.inputText("##newscene", addSceneName);
        ImGui.sameLine();

        String suggestedFileName = addSceneName.get().trim().replaceAll("[^a-zA-Z0-9]", "_") + ".lyvexscene";

        ImGui.textDisabled(" → " + suggestedFileName);

        if (ImGui.button("Add")) {
            String name = addSceneName.get().trim();
            if (!name.isEmpty()) {
                String fileName = name.replaceAll("[^a-zA-Z0-9]", "_") + ".lyvexscene";
                manager.addScene(name, fileName);
                addSceneName.set("");
                context.setSceneDirty(true);
            }
        }

        ImGui.separator();
        ImGui.text("Tip: Drag scenes to reorder build index");
        ImGui.text("Check 'In Build' to include in final game");
    }

    private void drawInputSettings(EditorContext context) {
        ImGui.text("Input Settings");
        ImGui.separator();
        ImGui.text("Coming soon...");
    }

    private void drawAudioSettings(EditorContext context) {
        ImGui.text("Audio Settings");
        ImGui.separator();

        masterVolume[0] = AudioManager.getMasterVolume();
        audioMuted.set(AudioManager.isMuted());

        if (ImGui.sliderFloat("Master Volume", masterVolume, 0.0f, 1.0f)) {
            AudioManager.setMasterVolume(masterVolume[0]);
            context.setSceneDirty(true);
        }

        if (ImGui.checkbox("Mute", audioMuted)) {
            AudioManager.setMuted(audioMuted.get());
            context.setSceneDirty(true);
        }

        ImGui.separator();
        ImGui.textDisabled("Tip: Master Volume controls the global listener gain.");
    }

    private void openSceneDialog(EditorContext context) {
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

    @Override
    public void optionsMenu() {
    }
}