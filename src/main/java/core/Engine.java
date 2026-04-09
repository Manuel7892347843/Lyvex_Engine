package core;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

import imgui.ImGui;
import org.lwjgl.opengl.GL;
import ui.ImGuiLayer;

public class Engine {

    private static final int WINDOW_WIDTH = 1280;
    private static final int WINDOW_HEIGHT = 720;
    private static final String WINDOW_TITLE = "Lyvex Engine";

    private long window;
    private ImGuiLayer imguiLayer;

    public void run() {
        try {
            init();
            loop();
        } finally {
            cleanup();
        }
    }

    private void init() {
        if (!glfwInit()) {
            throw new IllegalStateException("GLFW init fallito");
        }

        window = glfwCreateWindow(WINDOW_WIDTH, WINDOW_HEIGHT, WINDOW_TITLE, 0, 0);
        if (window == 0) {
            throw new RuntimeException("Finestra non creata");
        }

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        glfwShowWindow(window);

        GL.createCapabilities();
        glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

        imguiLayer = new ImGuiLayer();
        imguiLayer.init(window);
    }

    private void loop() {
        while (!glfwWindowShouldClose(window)) {
            glfwPollEvents();

            imguiLayer.startFrame();

            ImGui.begin("Debug");
            ImGui.text("Funziona!");
            ImGui.end();

            glClear(GL_COLOR_BUFFER_BIT);

            imguiLayer.render();
            glfwSwapBuffers(window);
        }
    }

    private void cleanup() {
        if (imguiLayer != null) {
            imguiLayer.dispose();
        }
        if (window != 0) {
            glfwDestroyWindow(window);
        }
        glfwTerminate();
    }
}