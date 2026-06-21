package core.runtime;

import com.google.gson.Gson;
import core.ProjectManager;
import core.ProjectSettings;
import core.audio.AudioManager;
import core.input.InputManager;
import core.physics.PhysicsWorld2D;
import core.render.SceneRenderer;
import core.scene.Scene;
import core.scene.SceneSerializer;
import core.scriptutil.ScriptComponentRegistry;
import org.lwjgl.opengl.GL;
import core.ui.EditorContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.lwjgl.glfw.GLFW.*;

public class RuntimeApplication {

    private static final int DEFAULT_WINDOW_WIDTH = 1280;
    private static final int DEFAULT_WINDOW_HEIGHT = 720;
    private static final String DEFAULT_WINDOW_TITLE = "Lyvex Game";

    private static final float FIXED_TIMESTEP = 1.0f / 60.0f;

    private long window;
    private Scene currentScene;
    private SceneRenderer sceneRenderer;
    private SceneRuntime sceneRuntime;

    private ProjectSettings.ProjectData projectData;

    private float deltaTime = 0.0f;
    private double lastFrameTime = 0.0;
    private float accumulator = 0.0f;

    public void run(Path exportedProjectRoot) {
        try {
            init(exportedProjectRoot);
            loop();
        } finally {
            cleanup();
        }
    }

    private void init(Path exportedProjectRoot) {
        ProjectManager.openProject(exportedProjectRoot);
        ProjectSettings.load();

        projectData = loadProjectData();

        ScriptComponentRegistry.refresh();

        if (!glfwInit()) {
            throw new IllegalStateException("GLFW init failed");
        }

        int windowWidth = projectData.windowWidth > 0 ? projectData.windowWidth : DEFAULT_WINDOW_WIDTH;
        int windowHeight = projectData.windowHeight > 0 ? projectData.windowHeight : DEFAULT_WINDOW_HEIGHT;
        String windowTitle = projectData.gameName == null || projectData.gameName.isBlank()
                ? DEFAULT_WINDOW_TITLE
                : projectData.gameName;

        long monitor = projectData.fullscreen ? glfwGetPrimaryMonitor() : 0;

        window = glfwCreateWindow(
                windowWidth,
                windowHeight,
                windowTitle,
                monitor,
                0
        );

        if (window == 0) {
            throw new RuntimeException("Window not created");
        }

        glfwMakeContextCurrent(window);
        glfwSwapInterval(projectData.vsync ? 1 : 0);
        glfwShowWindow(window);

        GL.createCapabilities();

        AudioManager.init();
        InputManager.init(window);

        currentScene = loadStartupScene(projectData);
        EditorContext.getInstance().setCurrentScene(currentScene);

        sceneRuntime = new SceneRuntime(currentScene);

        sceneRenderer = new SceneRenderer(windowWidth, windowHeight);
        sceneRenderer.setScene(currentScene);
        sceneRenderer.setUseSceneCamera(true);
        sceneRenderer.setRuntimeMode(true);
        sceneRenderer.setTargetDisplay(1);

        sceneRuntime.registerAllColliders();
        sceneRuntime.awake();
        sceneRuntime.start();

        lastFrameTime = glfwGetTime();
    }

    private Scene loadStartupScene(ProjectSettings.ProjectData data) {
        Path scenePath = null;

        if (data.startupScene != null && !data.startupScene.isBlank()) {
            scenePath = ProjectManager.getScenesPath().resolve(data.startupScene);
        }

        if (scenePath == null || !Files.exists(scenePath)) {
            System.err.println("[Runtime] Startup scene not found: " + scenePath);
            scenePath = findFirstExistingBuildScene(data);
        }

        if (scenePath == null || !Files.exists(scenePath)) {
            throw new IllegalStateException("No valid startup scene found in exported build.");
        }

        try {
            System.out.println("[Runtime] Loading startup scene: " + scenePath);
            return SceneSerializer.load(scenePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load startup scene: " + scenePath, e);
        }
    }

    private Path findFirstExistingBuildScene(ProjectSettings.ProjectData data) {
        if (data.scenes == null || data.scenes.isEmpty()) {
            return null;
        }

        for (core.lib.SceneManager.SceneEntry scene : data.scenes) {
            if (scene == null || !scene.inBuild || scene.fileName == null || scene.fileName.isBlank()) {
                continue;
            }

            Path candidate = ProjectManager.getScenesPath().resolve(scene.fileName);

            if (Files.exists(candidate)) {
                System.err.println("[Runtime] Fallback startup scene: " + scene.fileName);
                return candidate;
            }
        }

        return null;
    }

    private ProjectSettings.ProjectData loadProjectData() {
        Path projectFile = ProjectManager.getProjectFilePath();

        if (!Files.exists(projectFile)) {
            throw new IllegalStateException("Project file not found: " + projectFile);
        }

        try {
            String json = Files.readString(projectFile);
            ProjectSettings.ProjectData data = new Gson().fromJson(json, ProjectSettings.ProjectData.class);

            if (data == null) {
                throw new IllegalStateException("Project file contains no data: " + projectFile);
            }

            return data;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read project file: " + projectFile, e);
        }
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

            while (accumulator >= FIXED_TIMESTEP) {
                if (sceneRuntime != null) {
                    sceneRuntime.fixedUpdate();
                }

                PhysicsWorld2D.getInstance().step(FIXED_TIMESTEP);
                accumulator -= FIXED_TIMESTEP;
            }

            if (sceneRuntime != null) {
                sceneRuntime.update();
                sceneRuntime.lateUpdate();
            }

            if (sceneRenderer != null) {
                sceneRenderer.render();
            }

            glfwSwapBuffers(window);
            InputManager.endFrame();
        }
    }

    private void cleanup() {
        if (sceneRuntime != null) {
            sceneRuntime.destroy();
            sceneRuntime = null;
        }

        PhysicsWorld2D.getInstance().clearColliders();

        if (sceneRenderer != null) {
            sceneRenderer.dispose();
            sceneRenderer = null;
        }

        InputManager.dispose();
        AudioManager.cleanup();

        if (window != 0) {
            glfwDestroyWindow(window);
            window = 0;
        }

        glfwTerminate();
    }
}