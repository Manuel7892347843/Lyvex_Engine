package core;

import static org.lwjgl.glfw.GLFW.*;

import com.google.gson.Gson;
import core.audio.AudioManager;
import core.input.InputManager;
import core.lib.SceneManager;
import core.lib.Timer;
import core.physics.PhysicsWorld2D;
import core.render.FrameBuffer;
import core.render.GameFrameBuffer;
import core.render.SceneRenderer;
import core.runtime.SceneRuntime;
import core.scene.Scene;
import core.scene.SceneSerializer;
import core.scriptutil.ScriptAutoRefreshWatcher;
import core.scriptutil.ScriptComponentRegistry;
import org.lwjgl.opengl.GL;
import core.ui.EditorContext;
import core.ui.EditorUI;
import core.ui.ImGuiLayer;

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

    public static final float FIXED_TIMESTEP = 1.0f / 60.0f;

    private static boolean isInPlayMode = false;
    private boolean isInitialized = false;

    private Scene editorScene;
    private Scene playModeStartScene;
    private Scene runtimeScene;
    private SceneRuntime sceneRuntime;

    private static float deltaTime = 0.0f;
    private static double lastFrameTime = 0.0;
    private float accumulator = 0.0f;

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

        AudioManager.init();

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

            if (!isInPlayMode) {
                editorScene = newScene;
                currentScene = newScene;
                runtimeScene = null;
                sceneRuntime = null;

                if (sceneRenderer != null && newScene != null) {
                    sceneRenderer.setScene(newScene);
                }
            }
        });

        ScriptComponentRegistry.refresh();

        scriptAutoRefreshWatcher = new ScriptAutoRefreshWatcher();
        scriptAutoRefreshWatcher.start();

        loadSceneFromProjectFileOrDefault();

        editorScene = currentScene;
        editorUI.getContext().setCurrentScene(currentScene);
        sceneRenderer.setScene(currentScene);

        lastFrameTime = glfwGetTime();
    }

    private void loop() {
        while (!glfwWindowShouldClose(window)) {
            double currentTime = glfwGetTime();
            deltaTime = (float) (currentTime - lastFrameTime);
            lastFrameTime = currentTime;

            accumulator += deltaTime;
            accumulator = Math.min(accumulator, 0.25f);

            glfwPollEvents();
            InputManager.update();
            Timer.update();
            imguiLayer.startFrame();

            if (isInPlayMode && !isInitialized) {
                enterPlayMode();
            }

            if (!isInPlayMode && isInitialized) {
                exitPlayMode();
            }

            if (isInPlayMode && sceneRuntime != null) {
                while (accumulator >= FIXED_TIMESTEP) {
                    sceneRuntime.fixedUpdate();
                    PhysicsWorld2D.getInstance().step(FIXED_TIMESTEP);
                    accumulator -= FIXED_TIMESTEP;
                }

                sceneRuntime.update();
                sceneRuntime.lateUpdate();
            }

            if (isInPlayMode) {
                int selectedDisplay = editorUI.getGamePanel().getSelectedDisplay();

                gameFrameBuffer.bind();
                sceneRenderer.setUseSceneCamera(true);
                sceneRenderer.setTargetDisplay(selectedDisplay);
                sceneRenderer.render();
                gameFrameBuffer.unbind();

                editorUI.getGamePanel().setGameTextureId(gameFrameBuffer.getTextureId());
            }

            sceneFrameBuffer.bind();
            sceneRenderer.setUseSceneCamera(false);
            sceneRenderer.render();
            sceneFrameBuffer.unbind();

            editorUI.getContext().setSceneTextureId(sceneFrameBuffer.getTextureId());

            editorUI.draw();

            if (!isInPlayMode) {
                sceneRenderer.updateCamera(editorUI.getContext());
            }

            imguiLayer.render();
            glfwSwapBuffers(window);
            InputManager.endFrame();
        }
    }

    private void enterPlayMode() {
        if (currentScene == null) {
            isInPlayMode = false;
            return;
        }

        playModeStartScene = currentScene;  // SALVA la scena di partenza
        editorScene = currentScene;
        runtimeScene = SceneSerializer.clone(editorScene);

        if (runtimeScene == null) {
            isInPlayMode = false;
            return;
        }

        currentScene = runtimeScene;

        editorUI.getContext().setCurrentScene(runtimeScene);
        sceneRenderer.setScene(runtimeScene);

        accumulator = 0.0f;

        sceneRuntime = new SceneRuntime(runtimeScene);
        sceneRuntime.registerAllColliders();
        sceneRuntime.awake();
        sceneRuntime.start();

        isInitialized = true;
        editorUI.setShowGameView(true);
    }

    private void exitPlayMode() {
        if (sceneRuntime != null) {
            sceneRuntime.destroy();
            sceneRuntime = null;
        }

        PhysicsWorld2D.getInstance().clearColliders();

        runtimeScene = null;

        currentScene = playModeStartScene != null ? playModeStartScene : editorScene;
        playModeStartScene = null;

        editorUI.getContext().setCurrentScene(currentScene);
        sceneRenderer.setScene(currentScene);

        isInitialized = false;
        accumulator = 0.0f;
        editorUI.setShowGameView(false);
    }

    public void startPlayMode() {
        if (isInPlayMode || currentScene == null) {
            return;
        }

        isInPlayMode = true;
    }

    public void stopPlayMode() {
        if (!isInPlayMode) {
            return;
        }

        isInPlayMode = false;
    }

    public static float getDeltaTime() {
        return deltaTime;
    }

    public void openScene(Path scenePath) {
        if (isInPlayMode) {
            throw new IllegalStateException("Cannot open a scene while Play Mode is active.");
        }

        if (scenePath == null || !Files.exists(scenePath)) {
            throw new IllegalArgumentException("Invalid scene path: " + scenePath);
        }

        try {
            currentScene = SceneSerializer.load(scenePath);
            editorScene = currentScene;
            runtimeScene = null;
            sceneRuntime = null;
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
        Scene sceneToSave = currentScene;

        if (isInPlayMode) {
            throw new IllegalStateException("Cannot save while Play Mode is active.");
        }

        if (sceneToSave == null) {
            throw new IllegalStateException("Cannot save: current scene is null");
        }

        try {
            if (currentScenePath == null) {
                currentScenePath = createDefaultScenePath(sceneToSave);
            }

            Files.createDirectories(currentScenePath.getParent());
            SceneSerializer.save(sceneToSave, currentScenePath);
            updateStartupScene(currentScenePath);
            ProjectSettings.save();

            System.out.println("Scene saved: " + currentScenePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save scene", e);
        }
    }

    private static Path createDefaultScenePath(Scene scene) throws IOException {
        Path scenesDir = ProjectManager.getScenesPath();
        Files.createDirectories(scenesDir);

        String sceneName = scene.getName();
        if (sceneName == null || sceneName.isBlank()) {
            sceneName = "Untitled Scene";
        }

        String fileName = sceneName.trim().replaceAll("[^a-zA-Z0-9]", "_");
        if (fileName.isBlank()) {
            fileName = "Untitled_Scene";
        }

        Path scenePath = scenesDir.resolve(fileName + ".lyvexscene");
        int counter = 1;

        while (Files.exists(scenePath)) {
            scenePath = scenesDir.resolve(fileName + "_" + counter + ".lyvexscene");
            counter++;
        }

        return scenePath;
    }

    public void setEngineState(boolean enterInPlayMode) {
        isInPlayMode = enterInPlayMode;
    }

    public boolean getEngineState() {
        return isInPlayMode;
    }

    private void loadSceneFromProjectFileOrDefault() {
        Path projectFile = ProjectManager.getProjectFilePath();

        if (!Files.exists(projectFile)) {
            createAndSaveDefaultScene("Untitled Scene");
            return;
        }

        try {
            String json = Files.readString(projectFile);
            Gson gson = new Gson();
            ProjectSettings.ProjectData data = gson.fromJson(json, ProjectSettings.ProjectData.class);

            if (data == null || data.startupScene == null || data.startupScene.isBlank()) {
                createAndSaveDefaultScene("Untitled Scene");
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
                createAndSaveDefaultScene(data.startupScene.replace(".lyvexscene", ""));
                System.err.println("Startup scene not found, created a new one: " + scenePath);
            }

        } catch (IOException e) {
            System.err.println("Failed to load project file");
            e.printStackTrace();

            createAndSaveDefaultScene("Untitled Scene");
        }
    }

    private static void createAndSaveDefaultScene(String sceneName) {
        try {
            Scene defaultScene = new Scene(sceneName);
            currentScene = defaultScene;
            currentScenePath = createDefaultScenePath(defaultScene);

            Files.createDirectories(currentScenePath.getParent());
            SceneSerializer.save(defaultScene, currentScenePath);
            updateStartupScene(currentScenePath);

            EditorContext.getInstance().setCurrentScene(defaultScene);

            System.out.println("Created default scene: " + currentScenePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create default scene", e);
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
            initializeProjectSelection();
            return;
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
            initializeProjectSelection();
            return;
        }

        Path parentDirectory = chooser.getSelectedFile().toPath();

        String projectName = JOptionPane.showInputDialog(null, "Project name:");
        if (projectName == null || projectName.isBlank()) {
            JOptionPane.showMessageDialog(
                    null,
                    "Invalid project name.",
                    "Project Creation Error",
                    JOptionPane.ERROR_MESSAGE
            );
            initializeProjectSelection();
            return;
        }

        try {
            ProjectManager.createProject(parentDirectory, projectName);

            JOptionPane.showMessageDialog(
                    null,
                    "Project created successfully.",
                    "Lyvex Project",
                    JOptionPane.INFORMATION_MESSAGE
            );
        } catch (Exception e) {
            e.printStackTrace();

            JOptionPane.showMessageDialog(
                    null,
                    "Failed to create project:\n" + e.getMessage(),
                    "Project Creation Error",
                    JOptionPane.ERROR_MESSAGE
            );

            initializeProjectSelection();
        }
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
            initializeProjectSelection();
        }
    }

    public EditorUI getEditorUI() {
        return editorUI;
    }

    public void onSceneChanged(Scene scene) {
        if (isInPlayMode) {
            if (sceneRuntime != null) {
                sceneRuntime.destroy();
                sceneRuntime = null;
            }

            PhysicsWorld2D.getInstance().clearColliders();

            runtimeScene = SceneSerializer.clone(scene);
            if (runtimeScene == null) {
                isInPlayMode = false;
                isInitialized = false;
                editorUI.setShowGameView(false);
                return;
            }

            currentScene = runtimeScene;

            if (sceneRenderer != null) {
                sceneRenderer.setScene(runtimeScene);
            }
            if (editorUI != null) {
                editorUI.getContext().setCurrentScene(runtimeScene);
            }

            sceneRuntime = new SceneRuntime(runtimeScene);
            sceneRuntime.registerAllColliders();
            sceneRuntime.awake();
            sceneRuntime.start();

            accumulator = 0.0f;
            return;
        }

        if (sceneRenderer != null) {
            sceneRenderer.setScene(scene);
        }
        if (editorUI != null) {
            editorUI.getContext().setCurrentScene(scene);
        }
        currentScene = scene;
        editorScene = scene;
        runtimeScene = null;
        sceneRuntime = null;
    }

    private void cleanup() {
        if (sceneRuntime != null) {
            sceneRuntime.destroy();
            sceneRuntime = null;
        }

        if (scriptAutoRefreshWatcher != null) {
            scriptAutoRefreshWatcher.stop();
            scriptAutoRefreshWatcher = null;
        }

        if (sceneRenderer != null) {
            sceneRenderer.dispose();
            sceneRenderer = null;
        }

        if (sceneFrameBuffer != null) {
            sceneFrameBuffer.dispose();
            sceneFrameBuffer = null;
        }

        if (gameFrameBuffer != null) {
            gameFrameBuffer.dispose();
            gameFrameBuffer = null;
        }

        InputManager.dispose();

        AudioManager.cleanup();

        if (imguiLayer != null) {
            imguiLayer.dispose();
            imguiLayer = null;
        }

        if (window != 0) {
            glfwDestroyWindow(window);
            window = 0;
        }

        glfwTerminate();
    }
}