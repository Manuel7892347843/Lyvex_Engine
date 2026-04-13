package core;

import static org.lwjgl.glfw.GLFW.*;

import core.render.FrameBuffer;
import core.render.SceneRenderer;
import org.lwjgl.opengl.GL;
import ui.EditorUI;
import ui.ImGuiLayer;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Engine {

    private static final int WINDOW_WIDTH = 1920;
    private static final int WINDOW_HEIGHT = 1080;
    private static final String WINDOW_TITLE = "Lyvex Engine";

    private static final int SCENE_WIDTH = 1570;
    private static final int SCENE_HEIGHT = 600;

    private long window;
    private EditorUI editorUI;
    private ImGuiLayer imguiLayer;
    private SceneRenderer sceneRenderer;
    private FrameBuffer sceneFrameBuffer;
    private ScriptAutoRefreshWatcher scriptAutoRefreshWatcher;

    private Scene currentScene;
    private Path currentScenePath;

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

        loadOrCreateDefaultScene();
        editorUI.getContext().setCurrentScene(currentScene);
        sceneRenderer.setScene(currentScene);
    }

    private void loop() {
        while (!glfwWindowShouldClose(window)) {
            glfwPollEvents();

            imguiLayer.startFrame();

            editorUI.getContext().setSceneTextureId(sceneFrameBuffer.getTextureId());
            editorUI.draw();

            sceneRenderer.updateCamera(editorUI.getContext());

            sceneFrameBuffer.bind();
            sceneRenderer.render();
            sceneFrameBuffer.unbind();

            imguiLayer.render();
            glfwSwapBuffers(window);

            InputManager.endFrame();
        }
    }

    public void saveCurrentScenePublic() {
        if (currentScene == null || currentScenePath == null) {
            throw new IllegalStateException("Cannot save: current scene or path is null");
        }

        try {
            SceneSerializer.save(currentScene, currentScenePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save scene", e);
        }
    }

    private void loadOrCreateDefaultScene() {
        try {
            currentScenePath = ProjectManager.getProjectRoot()
                    .resolve("Assets")
                    .resolve("Scenes")
                    .resolve("Main.lyvexscene");

            if (Files.exists(currentScenePath)) {
                currentScene = SceneSerializer.load(currentScenePath);
            } else {
                currentScene = new Scene("Main Scene");
                currentScene.addRootObject(new GameObject("Camera Target"));
                SceneSerializer.save(currentScene, currentScenePath);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load scene", e);
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