package core.component.ui.uiElements;

import core.assetmanager.AssetManager;
import core.component.sprite.Sprite;
import core.component.sprite.SpriteLoader;
import core.component.ui.UIElement;
import core.component.ui.color.UIColor;
import core.input.InputManager;

import java.nio.file.Path;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;

public class UIButton extends UIElement {
    public Sprite normalSprite;
    public Sprite hoverSprite;
    public Sprite pressedSprite;

    public String normalSpritePath = "";
    public String hoverSpritePath = "";
    public String pressedSpritePath = "";

    public UIColor normalColor = new UIColor(1, 1, 1, 1);
    public UIColor hoverColor = new UIColor(0.9f, 0.9f, 0.9f, 1);
    public UIColor pressedColor = new UIColor(0.75f, 0.75f, 0.75f, 1);

    private transient boolean hovered;
    private transient boolean pressed;
    private transient Runnable onClick;

    public Sprite getNormalSprite() {
        return normalSprite;
    }

    public UIButton setNormalSprite(Sprite normalSprite) {
        this.normalSprite = normalSprite;
        return this;
    }

    public String getNormalSpritePath() {
        return normalSpritePath;
    }

    public UIButton setNormalSpritePath(String path) {
        this.normalSpritePath = path;
        this.normalSprite = loadSprite(path);
        return this;
    }

    public Sprite getHoverSprite() {
        return hoverSprite;
    }

    public UIButton setHoverSprite(Sprite hoverSprite) {
        this.hoverSprite = hoverSprite;
        return this;
    }

    public String getHoverSpritePath() {
        return hoverSpritePath;
    }

    public UIButton setHoverSpritePath(String path) {
        this.hoverSpritePath = path;
        this.hoverSprite = loadSprite(path);
        return this;
    }

    public Sprite getPressedSprite() {
        return pressedSprite;
    }

    public UIButton setPressedSprite(Sprite pressedSprite) {
        this.pressedSprite = pressedSprite;
        return this;
    }

    public String getPressedSpritePath() {
        return pressedSpritePath;
    }

    public UIButton setPressedSpritePath(String path) {
        this.pressedSpritePath = path;
        this.pressedSprite = loadSprite(path);
        return this;
    }

    private Sprite loadSprite(String path) {
        if (path == null || path.isBlank()) return null;
        try {
            Path realPath = AssetManager.getAssetPath().resolve(path);
            return SpriteLoader.loadFromFile(realPath);
        } catch (Exception e) {
            System.err.println("[UIButton] Failed to load sprite: " + path);
            e.printStackTrace();
            return null;
        }
    }

    public UIColor getNormalColor() {
        return normalColor;
    }

    public UIButton setNormalColor(UIColor normalColor) {
        this.normalColor = normalColor;
        return this;
    }

    public UIColor getHoverColor() {
        return hoverColor;
    }

    public UIButton setHoverColor(UIColor hoverColor) {
        this.hoverColor = hoverColor;
        return this;
    }

    public UIColor getPressedColor() {
        return pressedColor;
    }

    public UIButton setPressedColor(UIColor pressedColor) {
        this.pressedColor = pressedColor;
        return this;
    }

    public UIButton setOnClick(Runnable onClick) {
        this.onClick = onClick;
        return this;
    }

    public boolean isHovered() {
        return hovered;
    }

    public boolean isPressed() {
        return pressed;
    }

    public Sprite getCurrentSprite() {
        if (pressed && pressedSprite != null) {
            return pressedSprite;
        }
        if (hovered && hoverSprite != null) {
            return hoverSprite;
        }
        return normalSprite;
    }

    public UIColor getCurrentColor() {
        if (pressed) {
            return pressedColor;
        }
        if (hovered) {
            return hoverColor;
        }
        return normalColor;
    }

    public void processInput(float uiMouseX, float uiMouseY) {
        hovered = contains(uiMouseX, uiMouseY);

        boolean mouseDown = InputManager.isMouseButtonDown(GLFW_MOUSE_BUTTON_LEFT);

        if (hovered && mouseDown && !pressed) {
            System.out.println("[UIButton] Pressed");
            pressed = true;
        }

        if (pressed && !mouseDown) {
            System.out.println("[UIButton] Released");

            if (hovered && onClick != null) {
                System.out.println("[UIButton] Click");
                onClick.run();
            } else if (hovered) {
                System.out.println("[UIButton] Click rilevato, ma onClick è null");
            }

            pressed = false;
        }
    }
}