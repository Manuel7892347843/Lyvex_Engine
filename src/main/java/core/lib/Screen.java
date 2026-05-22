package core.lib;

import ui.EditorContext;

public final class Screen {

    private Screen() {}

    public static int width() {
        return Math.round(gameWidth());
    }

    public static int height() {
        return Math.round(gameHeight());
    }

    public static float gameWidth() {
        return EditorContext.getInstance().getGameViewportWidth();
    }

    public static float gameHeight() {
        return EditorContext.getInstance().getGameViewportHeight();
    }

    public static float sceneWidth() {
        return EditorContext.getInstance().getSceneViewportWidth();
    }

    public static float sceneHeight() {
        return EditorContext.getInstance().getSceneViewportHeight();
    }

    public static float aspectRatio() {
        float height = gameHeight();

        if (height == 0f) {
            return 0f;
        }

        return gameWidth() / height;
    }

    public static float centerX() {
        return gameWidth() * 0.5f;
    }

    public static float centerY() {
        return gameHeight() * 0.5f;
    }

    public static boolean isLandscape() {
        return gameWidth() >= gameHeight();
    }

    public static boolean isPortrait() {
        return gameHeight() > gameWidth();
    }

    public static boolean isGameHovered() {
        return EditorContext.getInstance().isGameHovered();
    }

    public static boolean isSceneHovered() {
        return EditorContext.getInstance().isSceneHovered();
    }
}