package core.ui.panels;

import core.Engine;
import core.ProjectManager;
import core.ProjectSettings;
import core.assetmanager.AssetManager;
import core.audio.AudioManager;
import core.export.GameExportSettings;
import core.export.GameExporter;
import core.gameobject.GameObject;
import core.lib.SceneManager;
import core.log.Log;
import core.physics.PhysicsLayerManager;
import core.scene.SceneSerializer;
import core.sorting.SortingLayerManager;
import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiDir;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import imgui.type.ImInt;
import imgui.type.ImString;
import core.ui.EditorContext;
import core.ui.EditorPanel;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class MainMenuBarPanel implements EditorPanel {
    private final int POS_X = 0;
    private final int POS_Y = 0;
    private final int WIDTH = 1920;
    private final int HEIGHT = 10;

    private final ImBoolean projectSettingsOpen = new ImBoolean(false);
    private int selectedSettingsTab = 0;
    private final ImString newLayerName = new ImString("", 64);
    private final ImString addSceneName = new ImString("", 64);
    private final float[] masterVolume = new float[]{1.0f};
    private final ImBoolean audioMuted = new ImBoolean(false);
    private final ImString codeEditorPathInput = new ImString("", 256);
    private final ImString zipName = new ImString("Assets.zip", 256);
    private final ImBoolean zipExportOpen = new ImBoolean(false);
    private Path zipDestinationDirectory;

    private final ImBoolean exportGameOpen = new ImBoolean(false);
    private final ImString exportGameName = new ImString("LyvexGame", 128);
    private final ImInt exportWindowWidth = new ImInt(1280);
    private final ImInt exportWindowHeight = new ImInt(720);
    private final ImBoolean exportFullscreen = new ImBoolean(false);
    private final ImBoolean exportVsync = new ImBoolean(true);
    private Path exportDestinationDirectory;
    private int exportStartupSceneIndex = 0;
    private final List<ImBoolean> exportSceneInBuild = new ArrayList<>();

    @Override
    public void init(){
        ImGui.setWindowPos(POS_X, POS_Y);
        ImGui.setWindowSize(WIDTH, HEIGHT);
    }

    @Override
    public void draw(EditorContext context) throws IOException {
        init();

        if (ImGui.beginMainMenuBar()) {
            if (ImGui.beginMenu("File")) {
                if (ImGui.beginMenu("Scene")) {
                    if (ImGui.menuItem("Open Scene...")) {
                        openSceneDialog(context);
                    }

                    if (ImGui.menuItem("Save Scene", "Ctrl+S")) {
                        saveSceneSafely();
                    }

                    ImGui.endMenu();
                }

                ImGui.endMenu();
            }

            if (ImGui.beginMenu("Edit")) {
                if (ImGui.menuItem("Delete")) {
                    GameObject selected = context.getSelectedGameObject();

                    if (selected != null) {
                        selected.destroy(context);
                    } else {
                        Log.logWarning("No GameObject selected.");
                    }
                }

                if (ImGui.menuItem("Duplicate")) {
                    duplicateSelectedGameObject(context);
                }

                ImGui.endMenu();
            }

            if (ImGui.beginMenu("Project")) {
                if (ImGui.menuItem("Save Project", "Ctrl+Shift+S")) {
                    ProjectSettings.save();
                }

                if (ImGui.menuItem("Project Settings")) {
                    projectSettingsOpen.set(true);
                    syncCodeEditorPathInput();
                }

                ImGui.endMenu();
            }

            if (ImGui.beginMenu("Tools")) {
                if (ImGui.menuItem("Export Game")) {
                    openExportGameWindow();
                }

                if (ImGui.menuItem("Zip Assets")) {
                    zipExportOpen.set(true);
                }

                ImGui.endMenu();
            }

            ImGui.endMainMenuBar();
        }

        if (projectSettingsOpen.get()) {
            drawProjectSettingsWindow(context);
        }

        if (zipExportOpen.get()) {
            drawZipExportWindow();
        }

        if (exportGameOpen.get()) {
            drawExportGameWindow();
        }
    }

    private void duplicateSelectedGameObject(EditorContext context) {
        GameObject selected = context.getSelectedGameObject();

        if (selected == null) {
            Log.logWarning("No GameObject selected.");
            return;
        }

        GameObject duplicate = SceneSerializer.cloneGameObject(selected);

        if (duplicate != null) {
            duplicate.getTransform().translate(new core.lib.math.vector2D(0.5f, 0.5f));
            context.getCurrentScene().addRootObject(duplicate);
            context.setSelectedGameObject(duplicate);
            context.setSceneDirty(true);
            Log.logSuccess("Duplicated GameObject: " + selected.getName());
        }
    }

    private void openExportGameWindow() {
        syncExportWindowFromProject();
        exportGameOpen.set(true);
    }

    private void syncExportWindowFromProject() {
        String projectName = ProjectManager.getProjectRoot().getFileName().toString();
        exportGameName.set(projectName == null || projectName.isBlank() ? "LyvexGame" : projectName);

        exportWindowWidth.set(1280);
        exportWindowHeight.set(720);
        exportFullscreen.set(false);
        exportVsync.set(true);

        SceneManager manager = ProjectSettings.getSceneManager();
        List<SceneManager.SceneEntry> scenes = manager.getSceneEntries();

        exportSceneInBuild.clear();

        for (SceneManager.SceneEntry scene : scenes) {
            exportSceneInBuild.add(new ImBoolean(scene.inBuild));
        }

        exportStartupSceneIndex = Math.max(0, manager.getActiveSceneIndex());

        if (exportStartupSceneIndex >= scenes.size()) {
            exportStartupSceneIndex = 0;
        }
    }

    private void drawExportGameWindow() {
        float windowWidth = 720.0f;
        float windowHeight = 560.0f;

        ImGui.setNextWindowSize(windowWidth, windowHeight, ImGuiCond.FirstUseEver);

        int flags = ImGuiWindowFlags.NoCollapse;

        if (ImGui.begin("Export Game", exportGameOpen, flags)) {
            ImGui.text("Build Settings");
            ImGui.separator();

            ImGui.inputText("Game Name", exportGameName);

            ImGui.inputInt("Window Width", exportWindowWidth);
            ImGui.inputInt("Window Height", exportWindowHeight);

            if (exportWindowWidth.get() < 320) {
                exportWindowWidth.set(320);
            }

            if (exportWindowHeight.get() < 180) {
                exportWindowHeight.set(180);
            }

            ImGui.checkbox("Fullscreen", exportFullscreen);
            ImGui.checkbox("VSync", exportVsync);

            ImGui.separator();

            if (exportDestinationDirectory == null) {
                ImGui.textWrapped("Destination: not selected");
            } else {
                ImGui.textWrapped("Destination: " + exportDestinationDirectory);
            }

            if (ImGui.button("Choose Destination")) {
                JFileChooser chooser = new JFileChooser();
                chooser.setDialogTitle("Select Export Folder");
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                chooser.setAcceptAllFileFilterUsed(false);

                int result = chooser.showSaveDialog(null);
                if (result == JFileChooser.APPROVE_OPTION) {
                    exportDestinationDirectory = chooser.getSelectedFile().toPath();
                }
            }

            ImGui.separator();

            drawExportScenesSection();

            ImGui.separator();

            if (ImGui.button("Export", 120, 28)) {
                exportGameFromWindow();
            }

            ImGui.sameLine();

            if (ImGui.button("Cancel", 120, 28)) {
                exportGameOpen.set(false);
            }
        }

        ImGui.end();
    }

    private void drawExportScenesSection() {
        SceneManager manager = ProjectSettings.getSceneManager();
        List<SceneManager.SceneEntry> scenes = manager.getSceneEntries();

        ImGui.text("Scenes");
        ImGui.textDisabled("Choose which scenes are included in the final game.");
        ImGui.separator();

        if (scenes.isEmpty()) {
            ImGui.textColored(1.0f, 0.3f, 0.3f, 1.0f, "No scenes found in Project Settings.");
            return;
        }

        while (exportSceneInBuild.size() < scenes.size()) {
            exportSceneInBuild.add(new ImBoolean(true));
        }

        for (int i = 0; i < scenes.size(); i++) {
            SceneManager.SceneEntry scene = scenes.get(i);

            ImGui.pushID("export_scene_" + i);

            ImGui.checkbox("##inbuild", exportSceneInBuild.get(i));
            ImGui.sameLine();

            boolean startup = exportStartupSceneIndex == i;
            if (ImGui.radioButton("Startup", startup)) {
                exportStartupSceneIndex = i;
                exportSceneInBuild.get(i).set(true);
            }

            ImGui.sameLine();
            ImGui.text(scene.name);
            ImGui.sameLine();
            ImGui.textDisabled("(" + scene.fileName + ")");

            ImGui.popID();
        }

        ImGui.separator();
        ImGui.textDisabled("Tip: The startup scene is the first scene loaded by the exported game.");
    }

    private void exportGameFromWindow() {
        if (exportDestinationDirectory == null) {
            Log.logWarning("Choose an export destination first.");
            return;
        }

        String gameName = exportGameName.get();

        if (gameName == null || gameName.isBlank()) {
            Log.logWarning("Game name cannot be empty.");
            return;
        }

        SceneManager manager = ProjectSettings.getSceneManager();
        List<SceneManager.SceneEntry> scenes = manager.getSceneEntries();

        if (scenes.isEmpty()) {
            Log.logWarning("No scenes available to export.");
            return;
        }

        if (exportStartupSceneIndex < 0 || exportStartupSceneIndex >= scenes.size()) {
            Log.logWarning("Invalid startup scene.");
            return;
        }

        exportSceneInBuild.get(exportStartupSceneIndex).set(true);

        GameExportSettings settings = new GameExportSettings();
        settings.gameName = gameName;
        settings.destinationDirectory = exportDestinationDirectory;
        settings.windowWidth = exportWindowWidth.get();
        settings.windowHeight = exportWindowHeight.get();
        settings.fullscreen = exportFullscreen.get();
        settings.vsync = exportVsync.get();
        settings.startupScene = scenes.get(exportStartupSceneIndex).fileName;

        for (int i = 0; i < scenes.size(); i++) {
            SceneManager.SceneEntry scene = scenes.get(i);
            boolean inBuild = i < exportSceneInBuild.size() && exportSceneInBuild.get(i).get();

            settings.scenes.add(new GameExportSettings.SceneBuildEntry(
                    scene.name,
                    scene.fileName,
                    inBuild
            ));
        }

        try {
            GameExporter.exportCurrentProject(settings);
            exportGameOpen.set(false);
        } catch (Exception e) {
            Log.logError("Export failed.", e);

            JOptionPane.showMessageDialog(
                    null,
                    "Export failed:\n" + e.getMessage(),
                    "Export Game",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void syncCodeEditorPathInput() {
        String path = ProjectSettings.getCodeEditorPath();
        codeEditorPathInput.set(path != null ? path : "");
    }

    private void drawProjectSettingsWindow(EditorContext context) throws IOException {
        ImGui.setNextWindowSize(700, 500, ImGuiCond.FirstUseEver);

        if (ImGui.begin("Project Settings", projectSettingsOpen)) {
            float sidebarWidth = 150;
            ImGui.beginChild("SettingsSidebar", sidebarWidth, 0, true);

            String[] tabs = {"Sorting Layers", "Scenes", "Audio", "Code Editor", "Physics Layers"};
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
                case 2 -> drawAudioSettings(context);
                case 3 -> drawCodeEditorSettings(context);
                case 4 -> drawPhysicsLayersSettings(context);
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
    }

    private void drawScenesSettings(EditorContext context) {
        ImGui.text("Scene Build Settings");
        ImGui.separator();

        SceneManager manager = ProjectSettings.getSceneManager();
        List<SceneManager.SceneEntry> scenes = manager.getSceneEntries();

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
    }

    private void drawCodeEditorSettings(EditorContext context) throws IOException {
        ImGui.text("Code Editor Settings");
        ImGui.separator();

        if (ImGui.inputText("Code Editor Path", codeEditorPathInput)) {
            ProjectSettings.setCodeEditorPath(codeEditorPathInput.get());
            context.setSceneDirty(true);
        }

        ImGui.sameLine();

        if (ImGui.button("Browse...")) {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Select Code Editor");

            int result = chooser.showOpenDialog(null);

            if (result == JFileChooser.APPROVE_OPTION) {
                String selectedPath = chooser.getSelectedFile().getAbsolutePath();
                codeEditorPathInput.set(selectedPath);
                ProjectSettings.setCodeEditorPath(selectedPath);
                context.setSceneDirty(true);
            }
        }

        ImGui.sameLine();

        if (ImGui.button("Open")) {
            String path = codeEditorPathInput.get();

            if (path == null || path.trim().isEmpty()) {
                return;
            }

            ProcessBuilder pb = new ProcessBuilder(path.trim(), ProjectManager.getProjectRoot().toString());
            pb.start();
        }
    }

    private void drawPhysicsLayersSettings(EditorContext context) {
        ImGui.text("Physics Layers");
        ImGui.separator();

        PhysicsLayerManager manager = ProjectSettings.getPhysicsLayerManager();
        List<PhysicsLayerManager.PhysicsLayer> layers = manager.getLayers();

        int layerToRemove = -1;

        for (int i = 0; i < layers.size(); i++) {
            ImGui.pushID("physlayer_" + i);
            PhysicsLayerManager.PhysicsLayer layer = layers.get(i);

            boolean isDefault = i == 0;

            ImGui.text(String.valueOf(i));
            ImGui.sameLine();

            ImString layerName = new ImString(layer.name, 64);
            if (ImGui.inputText("##name", layerName)) {
                String name = layerName.get().trim();

                if (!name.isEmpty() && !name.equals(layer.name) && !manager.layerExists(name)) {
                    manager.renameLayer(i, name);
                    context.setSceneDirty(true);
                }
            }

            ImGui.sameLine();
            ImGui.textDisabled("bit " + layer.bit);
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
        ImGui.inputText("##newphyslayer", newLayerName);
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
        ImGui.text("Collision Matrix:");
        ImGui.textDisabled("Check which layers can collide with each other");

        float cellSize = 80;

        ImGui.text("");

        for (int col = 0; col < layers.size() && col < 8; col++) {
            ImGui.sameLine(cellSize * (col + 1));
            String shortName = layers.get(col).name.length() > 6
                    ? layers.get(col).name.substring(0, 6)
                    : layers.get(col).name;
            ImGui.text(shortName);
        }

        for (int row = 0; row < layers.size() && row < 8; row++) {
            PhysicsLayerManager.PhysicsLayer rowLayer = layers.get(row);

            ImGui.text(rowLayer.name.length() > 6
                    ? rowLayer.name.substring(0, 6)
                    : rowLayer.name);

            for (int col = 0; col < layers.size() && col < 8; col++) {
                ImGui.sameLine(cellSize * (col + 1));

                boolean canCollide = manager.canLayersCollide(row, col);
                ImBoolean collide = new ImBoolean(canCollide);

                ImGui.pushID("collide_" + row + "_" + col);

                if (ImGui.checkbox("##c", collide)) {
                    manager.toggleLayerInMask(row, col, collide.get());
                    manager.toggleLayerInMask(col, row, collide.get());
                    context.setSceneDirty(true);
                }

                ImGui.popID();
            }
        }
    }

    private void drawZipExportWindow() {
        float windowWidth = 500.0f;
        float windowHeight = 180.0f;

        ImGui.setNextWindowSize(windowWidth, windowHeight, ImGuiCond.Always);

        int windowFlags = ImGuiWindowFlags.NoMove |
                ImGuiWindowFlags.NoResize |
                ImGuiWindowFlags.NoCollapse;

        if (ImGui.begin("Export Assets ZIP", zipExportOpen, windowFlags)) {
            ImGui.text("Zip your Assets");
            ImGui.separator();

            ImGui.inputText("Zip file name", zipName);

            if (zipDestinationDirectory == null) {
                ImGui.text("Destination: not selected");
            } else {
                ImGui.textWrapped("Destination: " + zipDestinationDirectory);
            }

            if (ImGui.button("Choose destination")) {
                JFileChooser chooser = new JFileChooser();
                chooser.setDialogTitle("Choose ZIP destination folder");
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                chooser.setAcceptAllFileFilterUsed(false);

                int result = chooser.showSaveDialog(null);

                if (result == JFileChooser.APPROVE_OPTION) {
                    zipDestinationDirectory = chooser.getSelectedFile().toPath();
                }
            }

            ImGui.sameLine();

            if (ImGui.button("Zip")) {
                if (zipDestinationDirectory == null) {
                    Log.logWarning("Choose a destination folder before exporting Assets.");
                } else {
                    try {
                        AssetManager.exportAllAssetsToZip(zipDestinationDirectory, zipName.get());
                        Log.logSuccess("Assets exported to ZIP successfully.");
                        zipExportOpen.set(false);
                    } catch (IOException e) {
                        Log.logError("Failed to export Assets ZIP.", e);
                    } catch (IllegalArgumentException e) {
                        Log.logError(e.getMessage());
                    }
                }
            }

            ImGui.sameLine();

            if (ImGui.button("Cancel")) {
                zipExportOpen.set(false);
            }
        }

        ImGui.end();
    }

    private void saveSceneSafely() {
        try {
            Engine.saveCurrentScenePublic();
            System.out.println("Scene saved successfully.");
        } catch (Exception e) {
            System.err.println("Failed to save scene.");
            e.printStackTrace();

            JOptionPane.showMessageDialog(
                    null,
                    "Failed to save scene:\n" + e.getMessage(),
                    "Save Scene Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
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