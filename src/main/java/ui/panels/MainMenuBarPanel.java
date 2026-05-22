package ui.panels;

import core.Engine;
import core.ProjectManager;
import core.ProjectSettings;
import core.audio.AudioManager;
import core.input.InputAction;
import core.input.InputAxis;
import core.input.InputManager;
import core.input.Key;
import core.lib.SceneManager;
import core.sorting.SortingLayerManager;
import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiDir;
import imgui.type.ImBoolean;
import imgui.type.ImInt;
import imgui.type.ImString;
import ui.EditorContext;
import ui.EditorPanel;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    // Input settings
    private final ImString newAxisName = new ImString("", 64);
    private final ImString newActionName = new ImString("", 64);
    private boolean addingAxisBinding = false;
    private boolean addingActionBinding = false;
    private InputAxis currentEditAxis = null;
    private InputAction currentEditAction = null;
    private boolean newBindingPositive = true;
    private int newBindingKeyIndex = 0;
    private int newBindingMouseButton = -1;
    private boolean newBindingShift = false;
    private boolean newBindingCtrl = false;
    private boolean newBindingAlt = false;

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
        ImGui.setNextWindowSize(700, 500, ImGuiCond.FirstUseEver);

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

        if (ImGui.button("Reset to Defaults")) {
            InputManager.setupDefaultInputs();
            context.setSceneDirty(true);
        }
        ImGui.separator();

        if (ImGui.beginTabBar("InputTabs")) {
            if (ImGui.beginTabItem("Axes")) {
                drawAxesSettings(context);
                ImGui.endTabItem();
            }
            if (ImGui.beginTabItem("Actions")) {
                drawActionsSettings(context);
                ImGui.endTabItem();
            }
            ImGui.endTabBar();
        }
    }

    private void drawAxesSettings(EditorContext context) {
        Map<String, InputAxis> axes = InputManager.getAxes();

        ImGui.text("Configured Axes (" + axes.size() + ")");
        ImGui.separator();

        List<String> toRemove = new ArrayList<>();

        for (InputAxis axis : axes.values()) {
            ImGui.pushID("axis_" + axis.name);

            boolean headerOpen = ImGui.collapsingHeader(axis.name);

            ImGui.sameLine();
            if (ImGui.button("Remove##" + axis.name)) {
                toRemove.add(axis.name);
            }

            if (headerOpen) {
                float[] deadZone = {axis.deadZone};
                if (ImGui.dragFloat("Dead Zone", deadZone, 0.01f, 0f, 1f)) {
                    axis.deadZone = deadZone[0];
                    context.setSceneDirty(true);
                }

                float[] sensitivity = {axis.sensitivity};
                if (ImGui.dragFloat("Sensitivity", sensitivity, 0.1f, 0f, 10f)) {
                    axis.sensitivity = sensitivity[0];
                    context.setSceneDirty(true);
                }

                ImBoolean invert = new ImBoolean(axis.invert);
                if (ImGui.checkbox("Invert", invert)) {
                    axis.invert = invert.get();
                    context.setSceneDirty(true);
                }

                ImGui.separator();
                ImGui.text("Bindings:");

                List<InputAxis.AxisBinding> bindingsToRemove = new ArrayList<>();

                for (int i = 0; i < axis.bindings.size(); i++) {
                    InputAxis.AxisBinding binding = axis.bindings.get(i);
                    ImGui.pushID("bind_" + i);

                    String direction = binding.positive ? "[+]" : "[-]";
                    String input = binding.key != null ? binding.key.name() :
                            (binding.mouseButton >= 0 ? "Mouse" + binding.mouseButton : "?");

                    ImGui.text(direction + " " + input);
                    ImGui.sameLine();
                    if (ImGui.button("X", 20, 20)) {
                        bindingsToRemove.add(binding);
                    }

                    ImGui.popID();
                }

                for (InputAxis.AxisBinding binding : bindingsToRemove) {
                    axis.bindings.remove(binding);
                    context.setSceneDirty(true);
                }

                if (ImGui.button("Add Binding##" + axis.name)) {
                    addingAxisBinding = true;
                    currentEditAxis = axis;
                    newBindingPositive = true;
                    newBindingKeyIndex = 0;
                    newBindingMouseButton = -1;
                }

                if (addingAxisBinding && currentEditAxis == axis) {
                    ImGui.separator();
                    ImGui.text("New Binding:");

                    ImBoolean positive = new ImBoolean(newBindingPositive);
                    if (ImGui.checkbox("Positive Direction", positive)) {
                        newBindingPositive = positive.get();
                    }

                    // Key selection
                    Key[] allKeys = Key.values();
                    String[] keyNames = new String[allKeys.length + 1];
                    keyNames[0] = "None";
                    for (int i = 0; i < allKeys.length; i++) {
                        keyNames[i + 1] = allKeys[i].name();
                    }

                    ImInt keyIndex = new ImInt(newBindingKeyIndex + 1);
                    ImGui.combo("Key", keyIndex, keyNames);
                    newBindingKeyIndex = keyIndex.get() - 1;

                    // Mouse button selection
                    String[] mouseOptions = {"None", "Left (0)", "Right (1)", "Middle (2)"};
                    ImInt mouseIdx = new ImInt(newBindingMouseButton + 1);
                    ImGui.combo("Mouse Button", mouseIdx, mouseOptions);
                    newBindingMouseButton = mouseIdx.get() - 1;

                    if (ImGui.button("Confirm")) {
                        InputAxis.AxisBinding newBinding = new InputAxis.AxisBinding();
                        newBinding.positive = newBindingPositive;
                        if (newBindingKeyIndex >= 0) {
                            newBinding.key = allKeys[newBindingKeyIndex];
                        }
                        if (newBindingMouseButton >= 0) {
                            newBinding.mouseButton = newBindingMouseButton;
                        }
                        axis.bindings.add(newBinding);
                        addingAxisBinding = false;
                        currentEditAxis = null;
                        context.setSceneDirty(true);
                    }
                    ImGui.sameLine();
                    if (ImGui.button("Cancel")) {
                        addingAxisBinding = false;
                        currentEditAxis = null;
                    }
                }
            }

            ImGui.popID();
        }

        for (String name : toRemove) {
            InputManager.removeAxis(name);
            context.setSceneDirty(true);
        }

        ImGui.separator();
        ImGui.text("Add New Axis:");
        ImGui.inputText("##newaxis", newAxisName);
        ImGui.sameLine();
        if (ImGui.button("Add Axis")) {
            String name = newAxisName.get().trim();
            if (!name.isEmpty() && !axes.containsKey(name)) {
                InputManager.registerAxis(new InputAxis(name));
                newAxisName.set("");
                context.setSceneDirty(true);
            }
        }
    }

    private void drawActionsSettings(EditorContext context) {
        Map<String, InputAction> actions = InputManager.getActions();

        ImGui.text("Configured Actions (" + actions.size() + ")");
        ImGui.separator();

        List<String> toRemove = new ArrayList<>();

        for (InputAction action : actions.values()) {
            ImGui.pushID("action_" + action.name);

            boolean headerOpen = ImGui.collapsingHeader(action.name);

            ImGui.sameLine();
            if (ImGui.button("Remove##" + action.name)) {
                toRemove.add(action.name);
            }

            if (headerOpen) {
                ImGui.text("Bindings:");
                List<InputAction.ActionBinding> bindingsToRemove = new ArrayList<>();

                for (int i = 0; i < action.bindings.size(); i++) {
                    InputAction.ActionBinding binding = action.bindings.get(i);
                    ImGui.pushID("bind_" + i);

                    String modifiers = "";
                    if (binding.shift) modifiers += "Shift+";
                    if (binding.ctrl) modifiers += "Ctrl+";
                    if (binding.alt) modifiers += "Alt+";

                    String input = binding.key != null ? binding.key.name() :
                            (binding.mouseButton >= 0 ? "Mouse" + binding.mouseButton : "?");

                    ImGui.text(modifiers + input);
                    ImGui.sameLine();
                    if (ImGui.button("X", 20, 20)) {
                        bindingsToRemove.add(binding);
                    }

                    ImGui.popID();
                }

                for (InputAction.ActionBinding binding : bindingsToRemove) {
                    action.bindings.remove(binding);
                    context.setSceneDirty(true);
                }

                if (ImGui.button("Add Binding##" + action.name)) {
                    addingActionBinding = true;
                    currentEditAction = action;
                    newBindingKeyIndex = 0;
                    newBindingMouseButton = -1;
                    newBindingShift = false;
                    newBindingCtrl = false;
                    newBindingAlt = false;
                }

                if (addingActionBinding && currentEditAction == action) {
                    ImGui.separator();
                    ImGui.text("New Binding:");

                    ImBoolean shift = new ImBoolean(newBindingShift);
                    ImBoolean ctrl = new ImBoolean(newBindingCtrl);
                    ImBoolean alt = new ImBoolean(newBindingAlt);
                    if (ImGui.checkbox("Shift", shift)) newBindingShift = shift.get();
                    ImGui.sameLine();
                    if (ImGui.checkbox("Ctrl", ctrl)) newBindingCtrl = ctrl.get();
                    ImGui.sameLine();
                    if (ImGui.checkbox("Alt", alt)) newBindingAlt = alt.get();

                    Key[] allKeys = Key.values();
                    String[] keyNames = new String[allKeys.length + 1];
                    keyNames[0] = "None";
                    for (int i = 0; i < allKeys.length; i++) {
                        keyNames[i + 1] = allKeys[i].name();
                    }

                    ImInt keyIndex = new ImInt(newBindingKeyIndex + 1);
                    ImGui.combo("Key", keyIndex, keyNames);
                    newBindingKeyIndex = keyIndex.get() - 1;

                    // Mouse button selection
                    String[] mouseOptions = {"None", "Left (0)", "Right (1)", "Middle (2)"};
                    ImInt mouseIdx = new ImInt(newBindingMouseButton + 1);
                    ImGui.combo("Mouse Button", mouseIdx, mouseOptions);
                    newBindingMouseButton = mouseIdx.get() - 1;

                    if (ImGui.button("Confirm")) {
                        InputAction.ActionBinding newBinding = new InputAction.ActionBinding();
                        newBinding.shift = newBindingShift;
                        newBinding.ctrl = newBindingCtrl;
                        newBinding.alt = newBindingAlt;
                        if (newBindingKeyIndex >= 0) {
                            newBinding.key = allKeys[newBindingKeyIndex];
                        }
                        if (newBindingMouseButton >= 0) {
                            newBinding.mouseButton = newBindingMouseButton;
                        }
                        action.bindings.add(newBinding);
                        addingActionBinding = false;
                        currentEditAction = null;
                        context.setSceneDirty(true);
                    }
                    ImGui.sameLine();
                    if (ImGui.button("Cancel")) {
                        addingActionBinding = false;
                        currentEditAction = null;
                    }
                }
            }

            ImGui.popID();
        }

        for (String name : toRemove) {
            InputManager.removeAction(name);
            context.setSceneDirty(true);
        }

        ImGui.separator();
        ImGui.text("Add New Action:");
        ImGui.inputText("##newaction", newActionName);
        ImGui.sameLine();
        if (ImGui.button("Add Action")) {
            String name = newActionName.get().trim();
            if (!name.isEmpty() && !actions.containsKey(name)) {
                InputManager.registerAction(new InputAction(name));
                newActionName.set("");
                context.setSceneDirty(true);
            }
        }
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