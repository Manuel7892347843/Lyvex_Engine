package core.lib;

import core.input.InputManager;
import core.input.Key;

public final class Input {

    private Input() {}

    public static boolean getKey(Key key) {
        return InputManager.isKeyDown(key);
    }

    public static boolean getKeyDown(Key key) {
        return InputManager.isKeyPressed(key);
    }

    public static boolean getKeyUp(Key key) {
        return InputManager.isKeyReleased(key);
    }

    public static boolean getMouseButton(int button) {
        return InputManager.isMouseButtonDown(button);
    }

    public static boolean getMouseButtonDown(int button) {
        return InputManager.isMouseButtonPressed(button);
    }

    public static boolean getMouseButtonUp(int button) {
        return InputManager.isMouseButtonReleased(button);
    }

    public static float getMouseX() {
        return InputManager.getMouseX();
    }

    public static float getMouseY() {
        return InputManager.getMouseY();
    }

    public static float getMouseDeltaX() {
        return InputManager.getDeltaMouseX();
    }

    public static float getMouseDeltaY() {
        return InputManager.getDeltaMouseY();
    }

    public static float getScrollX() {
        return InputManager.getScrollX();
    }

    public static float getScrollY() {
        return InputManager.getScrollY();
    }
}