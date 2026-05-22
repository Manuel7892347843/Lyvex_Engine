package core.input;

import static org.lwjgl.glfw.GLFW.*;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;

import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;
import static org.lwjgl.glfw.GLFW.glfwSetCursorPosCallback;
import static org.lwjgl.glfw.GLFW.glfwSetKeyCallback;
import static org.lwjgl.glfw.GLFW.glfwSetMouseButtonCallback;
import static org.lwjgl.glfw.GLFW.glfwSetScrollCallback;

public class InputManager {
    private static long window;
    private static final int KEY_COUNT = 512;
    private static final int MOUSE_BUTTON_COUNT = 32;

    private static final boolean[] keysDown = new boolean[KEY_COUNT];
    private static final boolean[] keysPressed = new boolean[KEY_COUNT];
    private static final boolean[] keysReleased = new boolean[KEY_COUNT];

    private static final boolean[] mouseButtonsDown = new boolean[MOUSE_BUTTON_COUNT];
    private static final boolean[] mouseButtonsPressed = new boolean[MOUSE_BUTTON_COUNT];
    private static final boolean[] mouseButtonsReleased = new boolean[MOUSE_BUTTON_COUNT];
    private static final boolean[] keys = new boolean[GLFW_KEY_LAST + 1];
    private static final boolean[] mouseButtons = new boolean[GLFW_MOUSE_BUTTON_LAST + 1];

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

    public static void init(long windowHandle) {
        window = windowHandle;

        keyCallback = new GLFWKeyCallback() {
            @Override
            public void invoke(long window, int key, int scancode, int action, int mods) {
                if (key < 0 || key >= KEY_COUNT) return;
                if (action == GLFW_PRESS) {
                    if (!keysDown[key]) keysPressed[key] = true;
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
                    if (!mouseButtonsDown[button]) mouseButtonsPressed[button] = true;
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

    public static void update() {
        if (window == 0) {
            return;
        }

        double[] x = new double[1];
        double[] y = new double[1];

        glfwGetCursorPos(window, x, y);

        mouseX = x[0];
        mouseY = y[0];

        for (int i = 0; i < mouseButtons.length; i++) {
            mouseButtons[i] = glfwGetMouseButton(window, i) == GLFW_PRESS;
        }
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

    public static boolean isKeyDown(Key key) {
        return key != null && isKeyDown(key.getKeyCode());
    }

    public static boolean isKeyPressed(int key) {
        return key >= 0 && key < KEY_COUNT && keysPressed[key];
    }

    public static boolean isKeyPressed(Key key) {
        return key != null && isKeyPressed(key.getKeyCode());
    }

    public static boolean isKeyReleased(int key) {
        return key >= 0 && key < KEY_COUNT && keysReleased[key];
    }

    public static boolean isKeyReleased(Key key) {
        return key != null && isKeyReleased(key.getKeyCode());
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

    private static final Map<String, InputAxis> axes = new HashMap<>();
    private static final Map<String, InputAction> actions = new HashMap<>();

    public static void registerAxis(InputAxis axis) {
        axes.put(axis.name, axis);
    }

    public static void registerAction(InputAction action) {
        actions.put(action.name, action);
    }

    public static void removeAxis(String name) {
        axes.remove(name);
    }

    public static void removeAction(String name) {
        actions.remove(name);
    }

    public static Map<String, InputAxis> getAxes() {
        return new HashMap<>(axes);
    }

    public static Map<String, InputAction> getActions() {
        return new HashMap<>(actions);
    }

    public static float getAxisValue(String axisName) {
        InputAxis axis = axes.get(axisName);
        if (axis == null) return 0f;

        float value = 0f;
        for (InputAxis.AxisBinding binding : axis.bindings) {
            float bindingValue = getAxisBindingValue(binding);
            if (binding.positive) value += bindingValue;
            else value -= bindingValue;
        }

        value = Math.max(-1f, Math.min(1f, value));

        if (Math.abs(value) < axis.deadZone) value = 0f;
        if (axis.invert) value = -value;

        return value * axis.sensitivity;
    }

    public static boolean isActionPressed(String actionName) {
        InputAction action = actions.get(actionName);
        if (action == null) return false;

        for (InputAction.ActionBinding binding : action.bindings) {
            if (checkActionBinding(binding)) return true;
        }
        return false;
    }

    public static boolean isActionDown(String actionName) {
        InputAction action = actions.get(actionName);
        if (action == null) return false;

        for (InputAction.ActionBinding binding : action.bindings) {
            if (checkActionBindingDown(binding)) return true;
        }
        return false;
    }

    public static boolean isActionUp(String actionName) {
        InputAction action = actions.get(actionName);
        if (action == null) return false;

        for (InputAction.ActionBinding binding : action.bindings) {
            if (checkActionBindingUp(binding)) return true;
        }
        return false;
    }

    private static float getAxisBindingValue(InputAxis.AxisBinding binding) {
        if (binding.key != null && isKeyDown(binding.key)) return 1f;
        if (binding.mouseButton >= 0 && isMouseButtonDown(binding.mouseButton)) return 1f;
        return 0f;
    }

    private static boolean checkActionBinding(InputAction.ActionBinding binding) {
        if (!checkModifiers(binding)) return false;
        if (binding.key != null) return isKeyDown(binding.key);
        if (binding.mouseButton >= 0) return isMouseButtonDown(binding.mouseButton);
        return false;
    }

    private static boolean checkActionBindingDown(InputAction.ActionBinding binding) {
        if (!checkModifiers(binding)) return false;
        if (binding.key != null) return isKeyPressed(binding.key);
        if (binding.mouseButton >= 0) return isMouseButtonPressed(binding.mouseButton);
        return false;
    }

    private static boolean checkActionBindingUp(InputAction.ActionBinding binding) {
        if (!checkModifiers(binding)) return false;
        if (binding.key != null) return isKeyReleased(binding.key);
        if (binding.mouseButton >= 0) return isMouseButtonReleased(binding.mouseButton);
        return false;
    }

    private static boolean checkModifiers(InputAction.ActionBinding binding) {
        if (binding.shift && !isKeyDown(Key.LEFT_SHIFT) && !isKeyDown(Key.RIGHT_SHIFT)) return false;
        if (binding.ctrl && !isKeyDown(Key.LEFT_CONTROL) && !isKeyDown(Key.RIGHT_CONTROL)) return false;
        if (binding.alt && !isKeyDown(Key.LEFT_ALT) && !isKeyDown(Key.RIGHT_ALT)) return false;
        return true;
    }

    // ========== Setup default ==========
    public static void setupDefaultInputs() {
        // Axis: Horizontal
        InputAxis horizontal = new InputAxis("Horizontal");
        horizontal.bindings.add(new InputAxis.AxisBinding(true, Key.D));
        horizontal.bindings.add(new InputAxis.AxisBinding(true, Key.RIGHT));
        horizontal.bindings.add(new InputAxis.AxisBinding(false, Key.A));
        horizontal.bindings.add(new InputAxis.AxisBinding(false, Key.LEFT));
        registerAxis(horizontal);

        // Axis: Vertical
        InputAxis vertical = new InputAxis("Vertical");
        vertical.bindings.add(new InputAxis.AxisBinding(true, Key.W));
        vertical.bindings.add(new InputAxis.AxisBinding(true, Key.UP));
        vertical.bindings.add(new InputAxis.AxisBinding(false, Key.S));
        vertical.bindings.add(new InputAxis.AxisBinding(false, Key.DOWN));
        registerAxis(vertical);

        // Action: Jump
        InputAction jump = new InputAction("Jump");
        jump.bindings.add(new InputAction.ActionBinding(Key.SPACE));
        registerAction(jump);

        // Action: Interact
        InputAction interact = new InputAction("Interact");
        interact.bindings.add(new InputAction.ActionBinding(Key.E));
        registerAction(interact);

        // Action: Attack (mouse left)
        InputAction attack = new InputAction("Attack");
        attack.bindings.add(new InputAction.ActionBinding(0)); // mouse button 0
        registerAction(attack);

        // Action: Sprint (shift + W)
        InputAction sprint = new InputAction("Sprint");
        sprint.bindings.add(new InputAction.ActionBinding(Key.W, false, false, false));
        registerAction(sprint);
    }

    public static void dispose() {
        if (keyCallback != null) keyCallback.free();
        if (cursorPosCallback != null) cursorPosCallback.free();
        if (mouseButtonCallback != null) mouseButtonCallback.free();
        if (scrollCallback != null) scrollCallback.free();
    }
}