package ui.panels;

import core.ProjectManager;
import core.ProjectSettings;
import core.assetmanager.AssetManager;
import core.scriptutil.ScriptComponentRegistry;
import imgui.ImGui;
import imgui.flag.ImGuiMouseButton;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImString;
import ui.EditorContext;
import ui.EditorPanel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class AssetsPanel implements EditorPanel {
    private final int POS_X = 999;
    private final int POS_Y = 656;
    private final int WIDTH = 920;
    private final int HEIGHT = 353;

    private boolean showCreateFolderInput = false;
    private final ImString folderName = new ImString(256);

    private boolean showCreateComponentInput = false;
    private final ImString componentName = new ImString(256);

    private boolean showCreateSceneInput = false;
    private final ImString sceneName = new ImString(256);

    private Path currentDirectory = AssetManager.getAssetPath();

    @Override
    public void init() {
        ImGui.setWindowPos(POS_X, POS_Y);
        ImGui.setWindowSize(WIDTH, HEIGHT);
    }

    @Override
    public void draw(EditorContext context){
        ImGui.begin("Assets", ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoCollapse);

        if (ImGui.button("Back")) {
            Path assetsRoot = AssetManager.getAssetPath();

            if (!currentDirectory.equals(assetsRoot)) {
                currentDirectory = currentDirectory.getParent();
            }
        }

        ImGui.sameLine();
        ImGui.text("Current directory: " + currentDirectory);

        drawDirectoryContent(context);

        init();

        if (ImGui.isWindowHovered() && ImGui.isMouseClicked(ImGuiMouseButton.Right)) {
            ImGui.openPopup("AssetsOptionsMenu");
        }

        if (showCreateFolderInput) {
            createFolder();
        }

        if (showCreateComponentInput) {
            createComponent();
        }

        if (showCreateSceneInput) {
            createScene();
        }

        optionsMenu();

        ImGui.end();
    }

    @Override
    public void optionsMenu(){
        if (ImGui.beginPopup("AssetsOptionsMenu")) {
            if (ImGui.menuItem("Create folder")) {
                showCreateFolderInput = true;
                folderName.set("");
            }

            if (ImGui.menuItem("Create new component")) {
                showCreateComponentInput = true;
                componentName.set("");
            }

            if (ImGui.menuItem("Create new scene")) {
                showCreateSceneInput = true;
                sceneName.set("");
            }

            if(ImGui.menuItem("Show in Explorer")){
                try {
                    ProcessBuilder pb = new ProcessBuilder("explorer.exe", currentDirectory.toString());
                    pb.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            ImGui.endPopup();
        }
    }

    private void createFolder(){
        ImGui.begin("Create Folder", ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoCollapse);
        ImGui.setWindowPos((float) (1920 / 2) - 100, (float) (1080 / 2) - 50);
        ImGui.setWindowSize(270, 80);
        ImGui.inputText("Folder name", folderName);

        if (ImGui.button("Create")) {
            AssetManager.createFolder(currentDirectory, folderName.get());
            showCreateFolderInput = false;
        }

        ImGui.sameLine();

        if (ImGui.button("Cancel")) {
            showCreateFolderInput = false;
        }

        ImGui.end();
    }

    private void createComponent(){
        ImGui.begin("Create New Component", ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoCollapse);
        ImGui.setWindowPos((float) (1920 / 2) - 160, (float) (1080 / 2) - 50);
        ImGui.setWindowSize(330, 80);
        ImGui.inputText("Component name", componentName);

        if (ImGui.button("Create")) {
            AssetManager.createNewComponent(AssetManager.getScriptsPath(), componentName.get());
            ScriptComponentRegistry.refresh();
            showCreateComponentInput = false;
        }

        ImGui.sameLine();

        if (ImGui.button("Cancel")) {
            showCreateComponentInput = false;
        }

        ImGui.end();
    }

    private void createScene(){
        ImGui.begin("Create New Scene", ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoCollapse);
        ImGui.setWindowPos((float) (1920 / 2) - 160, (float) (1080 / 2) - 50);
        ImGui.setWindowSize(330, 80);
        ImGui.inputText("Scene name", sceneName);

        if (ImGui.button("Create")) {
            AssetManager.createNewScene(currentDirectory, sceneName.get());
            showCreateSceneInput = false;
        }

        ImGui.sameLine();

        if (ImGui.button("Cancel")) {
            showCreateSceneInput = false;
        }

        ImGui.end();
    }

    private void drawDirectoryContent(EditorContext context) {
        try (Stream<Path> paths = Files.list(currentDirectory)) {
            paths.forEach(path -> {
                String name = path.getFileName().toString();

                if (Files.isDirectory(path)) {
                    if (ImGui.selectable("[DIR] " + name)) {
                        currentDirectory = path;
                    }
                } else if (name.endsWith(".lyvexscene")) {
                    if (ImGui.selectable("[SCENE] " + name)) {
                        context.getEngine().openScene(path);
                    }
                } else {
                    if (ImGui.selectable("[FILE] " + name)) {
                        try {
                            if (name.endsWith(".java")) {
                                try {
                                    String editorPath = ProjectSettings.getCodeEditorPath();
                                    if (editorPath == null || editorPath.isEmpty()) {
                                        ProcessBuilder pb = new ProcessBuilder("explorer.exe",
                                                ProjectManager.getProjectRoot().toString());
                                        pb.start();
                                    } else {
                                        ProcessBuilder pb = new ProcessBuilder(
                                                editorPath,
                                                ProjectManager.getProjectRoot().toString(),
                                                path.toString()
                                        );
                                        pb.start();
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                            else if(name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg")){
                                ProcessBuilder pb = new ProcessBuilder("explorer.exe", path.toString());
                                pb.start();
                            }
                            else if(name.endsWith(".txt")){
                                ProcessBuilder pb = new ProcessBuilder("notepad.exe", path.toString());
                                pb.start();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}