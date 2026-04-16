package core;

import static org.lwjgl.glfw.GLFW.*;

import core.component.Component;
import core.render.FrameBuffer;
import core.render.SceneRenderer;
import org.lwjgl.opengl.GL;
import ui.EditorUI;
import ui.ImGuiLayer;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class Engine {

    /// Window size
    private static final int WINDOW_WIDTH = 1920;
    private static final int WINDOW_HEIGHT = 1080;

    /// Window title
    private static final String WINDOW_TITLE = "Lyvex Engine";

    /// Scene dimensions
    private static final int SCENE_WIDTH = 1570;
    private static final int SCENE_HEIGHT = 600;

    /// Engine state
    private static boolean isInPlayMode = false;
    private boolean isInitialized = false;

    /// Time
    private static float deltaTime = 0.0f;
    private double lastFrameTime = 0.0;

    private long window;
    private EditorUI editorUI;
    private ImGuiLayer imguiLayer;
    private SceneRenderer sceneRenderer;
    private FrameBuffer sceneFrameBuffer;
    private ScriptAutoRefreshWatcher scriptAutoRefreshWatcher;

    private static Scene currentScene;
    private static Path currentScenePath;

    public void run() {
        try {
            init();
            loop();
        } finally {
            cleanup();
        }
    }

    private void init() {
        initializeProjectSelection();

        if (!glfwInit()) {
            throw new IllegalStateException("GLFW init failed");
        }

        window = glfwCreateWindow(WINDOW_WIDTH, WINDOW_HEIGHT, WINDOW_TITLE, 0, 0);
        if (window == 0) {
            throw new RuntimeException("Window not created");
        }

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        glfwShowWindow(window);

        GL.createCapabilities();

        InputManager.init(window);

        imguiLayer = new ImGuiLayer();
        imguiLayer.init(window);

        sceneRenderer = new SceneRenderer(SCENE_WIDTH, SCENE_HEIGHT);
        sceneFrameBuffer = new FrameBuffer(SCENE_WIDTH, SCENE_HEIGHT);
        editorUI = new EditorUI();
        editorUI.getContext().setEngine(this);

        ScriptComponentRegistry.refresh();

        scriptAutoRefreshWatcher = new ScriptAutoRefreshWatcher();
        scriptAutoRefreshWatcher.start();

        loadSceneFromProjectFileOrDefault();
        editorUI.getContext().setCurrentScene(currentScene);
        sceneRenderer.setScene(currentScene);
    }

    /// Main loop
    private void loop() {
        while (!glfwWindowShouldClose(window)) {
            double currentTime = glfwGetTime();
            deltaTime = (float)(currentTime - lastFrameTime);
            lastFrameTime = currentTime;

            glfwPollEvents();

            imguiLayer.startFrame();

            editorUI.getContext().setSceneTextureId(sceneFrameBuffer.getTextureId());
            editorUI.draw();

            if (!isInPlayMode) {
                sceneRenderer.updateCamera(editorUI.getContext());
            }

            if (isInPlayMode && !isInitialized) {
                awakeScene();
                startScene();
                isInitialized = true;
            }

            if (isInPlayMode) {
                updateScene();
                lateUpdateScene();
            }

            sceneFrameBuffer.bind();
            sceneRenderer.render();
            sceneFrameBuffer.unbind();

            imguiLayer.render();
            glfwSwapBuffers(window);

            InputManager.endFrame();
        }
    }

    private void start(){
        for (GameObject rootObject : currentScene.getRootObjects()) {
            rootObject.getComponents().forEach(Component::start);
        }
    }

    private void awakeScene() {
        for (GameObject rootObject : currentScene.getRootObjects()) {
            rootObject.getComponents().forEach(component -> {
                if (!component.isAwoken()) {
                    component.awake();
                    component.setAwoken(true);
                }
            });
        }
    }

    private void startScene() {
        for (GameObject rootObject : currentScene.getRootObjects()) {
            rootObject.getComponents().forEach(component -> {
                if (component.isEnabled() && !component.isStarted()) {
                    component.start();
                    component.setStarted(true);
                }
            });
        }
    }

    private void updateScene() {
        for (GameObject rootObject : currentScene.getRootObjects()) {
            rootObject.getComponents().forEach(component -> {
                if (component.isEnabled()) {
                    component.update();
                }
            });
        }
    }

    private void lateUpdateScene() {
        for (GameObject rootObject : currentScene.getRootObjects()) {
            rootObject.getComponents().forEach(component -> {
                if (component.isEnabled()) {
                    component.lateUpdate();
                }
            });
        }
    }

    public void startPlayMode() {
        isInPlayMode = true;
    }

    public void stopPlayMode() {
        isInPlayMode = false;
    }

    public static float getDeltaTime() {
        return deltaTime;
    }

    public void openScene(Path scenePath) {
        if (scenePath == null || !Files.exists(scenePath)) {
            throw new IllegalArgumentException("Invalid scene path: " + scenePath);
        }

        try {
            currentScene = SceneSerializer.load(scenePath);
            currentScenePath = scenePath;

            if (editorUI != null) {
                editorUI.getContext().setCurrentScene(currentScene);
            }

            if (sceneRenderer != null) {
                sceneRenderer.setScene(currentScene);
            }

            saveSceneReferenceToProjectFile(scenePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to open scene: " + scenePath, e);
        }
    }

    public static void saveCurrentScenePublic() {
        if (currentScene == null || currentScenePath == null) {
            throw new IllegalStateException("Cannot save: current scene or path is null");
        }

        try {
            SceneSerializer.save(currentScene, currentScenePath);
            saveSceneReferenceToProjectFile(currentScenePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save scene", e);
        }
    }

    public void setEngineState(boolean enterInPlayMode) {
        isInPlayMode = enterInPlayMode;
    }

    public boolean getEngineState() {
        return isInPlayMode;
    }

    private void loadSceneFromProjectFileOrDefault() {
        try {
            Path projectFile = ProjectManager.getProjectFilePath();

            if (Files.exists(projectFile)) {
                String savedScenePathText = Files.readString(projectFile).trim();
                if (!savedScenePathText.isBlank()) {
                    Path savedScenePath = Path.of(savedScenePathText);
                    if (Files.exists(savedScenePath)) {
                        openScene(savedScenePath);
                        return;
                    }
                }
            }

            Path defaultScene = ProjectManager.getScenesPath().resolve("Main.lyvexscene");

            if (Files.exists(defaultScene)) {
                openScene(defaultScene);
            } else {
                currentScene = new Scene("Main Scene");
                currentScene.addRootObject(new GameObject("Camera Target"));
                SceneSerializer.save(currentScene, defaultScene);
                openScene(defaultScene);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load scene from project file", e);
        }
    }

    private static void saveSceneReferenceToProjectFile(Path scenePath) {
        try {
            Path projectFile = ProjectManager.getProjectFilePath();
            Files.writeString(
                    projectFile,
                    scenePath.toAbsolutePath().toString(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to write scene path into project file", e);
        }
    }

    private void openExistingProjectFlow() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Open Lyvex Project");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);

        int result = chooser.showOpenDialog(null);
        if (result != JFileChooser.APPROVE_OPTION) {
            throw new IllegalStateException("No project selected");
        }

        Path selectedPath = chooser.getSelectedFile().toPath();

        if (!ProjectManager.isValidProject(selectedPath)) {
            JOptionPane.showMessageDialog(
                    null,
                    "The selected folder is not a valid Lyvex project.",
                    "Invalid Project",
                    JOptionPane.ERROR_MESSAGE
            );
            openExistingProjectFlow();
            return;
        }

        ProjectManager.openProject(selectedPath);
    }

    private void createNewProjectFlow() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Parent Folder For New Project");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);

        int result = chooser.showOpenDialog(null);
        if (result != JFileChooser.APPROVE_OPTION) {
            throw new IllegalStateException("No project selected");
        }

        Path parentDirectory = chooser.getSelectedFile().toPath();

        String projectName = JOptionPane.showInputDialog(null, "Project name:");
        if (projectName == null || projectName.isBlank()) {
            throw new IllegalStateException("Invalid project name");
        }

        ProjectManager.createProject(parentDirectory, projectName);
    }

    private void initializeProjectSelection() {
        String[] options = {"Open Project", "New Project", "Cancel"};

        int choice = JOptionPane.showOptionDialog(
                null,
                "Choose how to start Lyvex Engine",
                "Lyvex Project",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                options,
                options[0]
        );

        if (choice == 0) {
            openExistingProjectFlow();
        } else if (choice == 1) {
            createNewProjectFlow();
        } else {
            throw new IllegalStateException("No project selected");
        }
    }

    private void cleanup() {
        if (scriptAutoRefreshWatcher != null) {
            scriptAutoRefreshWatcher.stop();
        }

        if (sceneRenderer != null) {
            sceneRenderer.dispose();
        }

        if (sceneFrameBuffer != null) {
            sceneFrameBuffer.dispose();
        }

        InputManager.dispose();

        if (imguiLayer != null) {
            imguiLayer.dispose();
        }

        if (window != 0) {
            glfwDestroyWindow(window);
        }

        glfwTerminate();
    }
}