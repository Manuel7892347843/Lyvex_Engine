package core;

import static org.lwjgl.glfw.GLFW.*;

import com.google.gson.Gson;
import core.component.Component;
import core.gameobject.GameObject;
import core.input.InputManager;
import core.lib.SceneManager;
import core.render.FrameBuffer;
import core.render.GameFrameBuffer;
import core.render.SceneRenderer;
import core.scene.Scene;
import core.scene.SceneSerializer;
import core.scriptutil.ScriptAutoRefreshWatcher;
import core.scriptutil.ScriptComponentRegistry;
import org.lwjgl.opengl.GL;
import ui.EditorContext;
import ui.EditorUI;
import ui.ImGuiLayer;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class Engine {

    private static final int WINDOW_WIDTH = 1920;
    private static final int WINDOW_HEIGHT = 1080;
    private static final String WINDOW_TITLE = "Lyvex Engine";
    private static final int SCENE_WIDTH = 1570;
    private static final int SCENE_HEIGHT = 600;

    private static boolean isInPlayMode = false;
    private boolean isInitialized = false;

    private static float deltaTime = 0.0f;
    private double lastFrameTime = 0.0;

    private long window;
    private EditorUI editorUI;
    private ImGuiLayer imguiLayer;
    private SceneRenderer sceneRenderer;
    private FrameBuffer sceneFrameBuffer;
    private GameFrameBuffer gameFrameBuffer;
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

        ProjectSettings.load();

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
        gameFrameBuffer = new GameFrameBuffer(SCENE_WIDTH, SCENE_HEIGHT);

        editorUI = new EditorUI();
        editorUI.getContext().setEngine(this);
        editorUI.getContext().addSceneChangeListener(() -> {
            Scene newScene = editorUI.getContext().getCurrentScene();
            if (sceneRenderer != null && newScene != null) {
                sceneRenderer.setScene(newScene);
            }
            currentScene = newScene;
        });

        ScriptComponentRegistry.refresh();

        scriptAutoRefreshWatcher = new ScriptAutoRefreshWatcher();
        scriptAutoRefreshWatcher.start();

        loadSceneFromProjectFileOrDefault();
        editorUI.getContext().setCurrentScene(currentScene);
        sceneRenderer.setScene(currentScene);
    }

    private void loop() {
        while (!glfwWindowShouldClose(window)) {
            double currentTime = glfwGetTime();
            deltaTime = (float)(currentTime - lastFrameTime);
            lastFrameTime = currentTime;

            glfwPollEvents();
            imguiLayer.startFrame();

            // === GESTIONE STATO PLAY MODE (PRIMA del rendering) ===
            if (isInPlayMode && !isInitialized) {
                awakeScene();
                startScene();
                isInitialized = true;
                editorUI.setShowGameView(true);  // ← QUI, prima di draw()
            }

            if (!isInPlayMode && isInitialized) {
                isInitialized = false;
                editorUI.setShowGameView(false); // ← QUI, prima di draw()
            }

            // === UPDATE LOGICA (solo in play) ===
            if (isInPlayMode) {
                updateScene();
                lateUpdateScene();
            }

            // === RENDERING FRAMEBUFFER ===

            // Game view (solo se in play mode)
            if (isInPlayMode) {
                gameFrameBuffer.bind();
                sceneRenderer.setUseSceneCamera(true);
                sceneRenderer.render();
                gameFrameBuffer.unbind();
                editorUI.getGamePanel().setGameTextureId(gameFrameBuffer.getTextureId());
            }

            // Scene view (sempre, per l'editor)
            sceneFrameBuffer.bind();
            sceneRenderer.setUseSceneCamera(false);
            sceneRenderer.render();
            sceneFrameBuffer.unbind();

            editorUI.getContext().setSceneTextureId(sceneFrameBuffer.getTextureId());

            // === UI ===
            editorUI.draw();

            // === CAMERA EDITOR (solo se non in play) ===
            if (!isInPlayMode) {
                sceneRenderer.updateCamera(editorUI.getContext());
            }

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
            awakeGameObjectRecursive(rootObject);
        }
    }

    private void awakeGameObjectRecursive(GameObject gameObject) {
        for (Component component : gameObject.getComponents()) {
            if (!component.isAwoken()) {
                component.awake();
                component.setAwoken(true);
            }
        }
        for (GameObject child : gameObject.getChildren()) {
            awakeGameObjectRecursive(child);
        }
    }

    private void startScene() {
        for (GameObject rootObject : currentScene.getRootObjects()) {
            startGameObjectRecursive(rootObject);
        }
    }

    private void startGameObjectRecursive(GameObject gameObject) {
        for (Component component : gameObject.getComponents()) {
            if (component.isEnabled() && !component.isStarted()) {
                component.start();
                component.setStarted(true);
            }
        }
        for (GameObject child : gameObject.getChildren()) {
            startGameObjectRecursive(child);
        }
    }

    private void updateScene() {
        for (GameObject rootObject : currentScene.getRootObjects()) {
            updateGameObjectRecursive(rootObject);
        }
    }

    private void updateGameObjectRecursive(GameObject gameObject) {
        for (Component component : gameObject.getComponents()) {
            if (component.isEnabled()) {
                component.update();
            }
        }
        for (GameObject child : gameObject.getChildren()) {
            updateGameObjectRecursive(child);
        }
    }

    private void lateUpdateScene() {
        for (GameObject rootObject : currentScene.getRootObjects()) {
            lateUpdateGameObjectRecursive(rootObject);
        }
    }

    private void lateUpdateGameObjectRecursive(GameObject gameObject) {
        for (Component component : gameObject.getComponents()) {
            if (component.isEnabled()) {
                component.lateUpdate();
            }
        }
        for (GameObject child : gameObject.getChildren()) {
            lateUpdateGameObjectRecursive(child);
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

            updateStartupScene(scenePath);

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
            ProjectSettings.save();
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
        Path projectFile = ProjectManager.getProjectFilePath();

        // Se il file progetto non esiste, crea scena vuota
        if (!Files.exists(projectFile)) {
            Scene defaultScene = new Scene("Untitled Scene");
            currentScene = defaultScene;
            EditorContext.getInstance().setCurrentScene(defaultScene);
            return;
        }

        try {
            // Leggi il JSON
            String json = Files.readString(projectFile);
            Gson gson = new Gson();
            ProjectSettings.ProjectData data = gson.fromJson(json, ProjectSettings.ProjectData.class);

            if (data == null || data.startupScene == null || data.startupScene.isBlank()) {
                Scene defaultScene = new Scene("Untitled Scene");
                currentScene = defaultScene;
                EditorContext.getInstance().setCurrentScene(defaultScene);
                return;
            }

            Path scenePath = ProjectManager.getScenesPath().resolve(data.startupScene);

            if (Files.exists(scenePath)) {
                Scene scene = SceneSerializer.load(scenePath);
                currentScene = scene;
                EditorContext.getInstance().setCurrentScene(scene);
                currentScenePath = scenePath;
                System.out.println("Loaded startup scene: " + data.startupScene);
            } else {
                Scene defaultScene = new Scene(data.startupScene.replace(".lyvexscene", ""));
                currentScene = defaultScene;
                EditorContext.getInstance().setCurrentScene(defaultScene);
                System.err.println("Startup scene not found: " + scenePath);
            }

        } catch (IOException e) {
            System.err.println("Failed to load project file");
            e.printStackTrace();

            Scene defaultScene = new Scene("Untitled Scene");
            currentScene = defaultScene;
            EditorContext.getInstance().setCurrentScene(defaultScene);
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

    private static void updateStartupScene(Path scenePath) {
        Path scenesDir = ProjectManager.getScenesPath();
        Path relativePath = scenesDir.relativize(scenePath);
        String fileName = relativePath.toString().replace('\\', '/');

        SceneManager manager = ProjectSettings.getSceneManager();
        boolean found = false;
        for (int i = 0; i < manager.getSceneEntries().size(); i++) {
            if (manager.getSceneEntries().get(i).fileName.equals(fileName)) {
                manager.loadScene(i);
                found = true;
                break;
            }
        }

        if (!found) {
            String sceneName = fileName.replace(".lyvexscene", "");
            manager.addScene(sceneName, fileName);
        }

        ProjectSettings.save();
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

    public EditorUI getEditorUI() {
        return editorUI;
    }

    public void onSceneChanged(Scene scene) {
        if (sceneRenderer != null) {
            sceneRenderer.setScene(scene);
        }
        if (editorUI != null) {
            editorUI.getContext().setCurrentScene(scene);
        }
        currentScene = scene;
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

        if (gameFrameBuffer != null) {
            gameFrameBuffer.dispose();
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