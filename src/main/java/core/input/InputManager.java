package core.input;

import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;

import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;
import static org.lwjgl.glfw.GLFW.glfwSetCursorPosCallback;
import static org.lwjgl.glfw.GLFW.glfwSetKeyCallback;
import static org.lwjgl.glfw.GLFW.glfwSetMouseButtonCallback;
import static org.lwjgl.glfw.GLFW.glfwSetScrollCallback;

public class InputManager {
    private static final int KEY_COUNT = 512;
    private static final int MOUSE_BUTTON_COUNT = 32;

    private static final boolean[] keysDown = new boolean[KEY_COUNT];
    private static final boolean[] keysPressed = new boolean[KEY_COUNT];
    private static final boolean[] keysReleased = new boolean[KEY_COUNT];

    private static final boolean[] mouseButtonsDown = new boolean[MOUSE_BUTTON_COUNT];
    private static final boolean[] mouseButtonsPressed = new boolean[MOUSE_BUTTON_COUNT];
    private static final boolean[] mouseButtonsReleased = new boolean[MOUSE_BUTTON_COUNT];

    private static double mouseX;
    private static double mouseY;
    private static double lastMouseX;
    private static double lastMouseY;
    private static double deltaMouseX;
    private static double deltaMouseY;

    private static double scrollX;
    private static double scrollY;

    private static GLFWKeyCallback keyCallback;
    private static GLFWCursorPosCallback cursorPosCallback;
    private static GLFWMouseButtonCallback mouseButtonCallback;
    private static GLFWScrollCallback scrollCallback;

    private InputManager() {
    }

    public static void init(long window) {
        keyCallback = new GLFWKeyCallback() {
            @Override
            public void invoke(long window, int key, int scancode, int action, int mods) {
                if (key < 0 || key >= KEY_COUNT) return;

                if (action == GLFW_PRESS) {
                    if (!keysDown[key]) {
                        keysPressed[key] = true;
                    }
                    keysDown[key] = true;
                } else if (action == GLFW_RELEASE) {
                    keysDown[key] = false;
                    keysReleased[key] = true;
                }
            }
        };

        cursorPosCallback = new GLFWCursorPosCallback() {
            @Override
            public void invoke(long window, double xpos, double ypos) {
                mouseX = xpos;
                mouseY = ypos;
            }
        };

        mouseButtonCallback = new GLFWMouseButtonCallback() {
            @Override
            public void invoke(long window, int button, int action, int mods) {
                if (button < 0 || button >= MOUSE_BUTTON_COUNT) return;

                if (action == GLFW_PRESS) {
                    if (!mouseButtonsDown[button]) {
                        mouseButtonsPressed[button] = true;
                    }
                    mouseButtonsDown[button] = true;
                } else if (action == GLFW_RELEASE) {
                    mouseButtonsDown[button] = false;
                    mouseButtonsReleased[button] = true;
                }
            }
        };

        scrollCallback = new GLFWScrollCallback() {
            @Override
            public void invoke(long window, double xoffset, double yoffset) {
                scrollX += xoffset;
                scrollY += yoffset;
            }
        };

        glfwSetKeyCallback(window, keyCallback);
        glfwSetCursorPosCallback(window, cursorPosCallback);
        glfwSetMouseButtonCallback(window, mouseButtonCallback);
        glfwSetScrollCallback(window, scrollCallback);
    }

    public static void endFrame() {
        for (int i = 0; i < KEY_COUNT; i++) {
            keysPressed[i] = false;
            keysReleased[i] = false;
        }

        for (int i = 0; i < MOUSE_BUTTON_COUNT; i++) {
            mouseButtonsPressed[i] = false;
            mouseButtonsReleased[i] = false;
        }

        scrollX = 0.0;
        scrollY = 0.0;

        deltaMouseX = mouseX - lastMouseX;
        deltaMouseY = mouseY - lastMouseY;
        lastMouseX = mouseX;
        lastMouseY = mouseY;
    }

    public static boolean isKeyDown(int key) {
        return key >= 0 && key < KEY_COUNT && keysDown[key];
    }

    public static boolean isKeyPressed(int key) {
        return key >= 0 && key < KEY_COUNT && keysPressed[key];
    }

    public static boolean isKeyReleased(int key) {
        return key >= 0 && key < KEY_COUNT && keysReleased[key];
    }

    public static boolean isMouseButtonDown(int button) {
        return button >= 0 && button < MOUSE_BUTTON_COUNT && mouseButtonsDown[button];
    }

    public static boolean isMouseButtonPressed(int button) {
        return button >= 0 && button < MOUSE_BUTTON_COUNT && mouseButtonsPressed[button];
    }

    public static boolean isMouseButtonReleased(int button) {
        return button >= 0 && button < MOUSE_BUTTON_COUNT && mouseButtonsReleased[button];
    }

    public static float getMouseX() {
        return (float) mouseX;
    }

    public static float getMouseY() {
        return (float) mouseY;
    }

    public static float getDeltaMouseX() {
        return (float) deltaMouseX;
    }

    public static float getDeltaMouseY() {
        return (float) deltaMouseY;
    }

    public static float getScrollX() {
        return (float) scrollX;
    }

    public static float getScrollY() {
        return (float) scrollY;
    }

    public static void dispose() {
        if (keyCallback != null) keyCallback.free();
        if (cursorPosCallback != null) cursorPosCallback.free();
        if (mouseButtonCallback != null) mouseButtonCallback.free();
        if (scrollCallback != null) scrollCallback.free();
    }
}