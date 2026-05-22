package core.component.ui.uiElements;

import core.assetmanager.AssetManager;
import core.component.sprite.Sprite;
import core.component.sprite.SpriteLoader;
import core.component.ui.UIElement;
import core.component.ui.color.UIColor;

import java.nio.file.Path;

public class UIImage extends UIElement {
    public Sprite sprite;
    public UIColor tint = UIColor.white();
    public String spriteAssetPath = "";

    public Sprite getSprite() {
        return sprite;
    }

    public UIImage setSprite(Sprite sprite) {
        this.sprite = sprite;
        return this;
    }

    public String getSpriteAssetPath() {
        return spriteAssetPath;
    }

    public UIImage setSpriteAssetPath(String path) {
        this.spriteAssetPath = path;
        if (path != null && !path.isBlank()) {
            try {
                Path realPath = AssetManager.getAssetPath().resolve(path);
                this.sprite = SpriteLoader.loadFromFile(realPath);
            } catch (Exception e) {
                System.err.println("[UIImage] Failed to load sprite: " + path);
                e.printStackTrace();
            }
        } else {
            this.sprite = null;
        }
        return this;
    }

    public UIColor getTint() {
        return tint;
    }

    public UIImage setTint(UIColor tint) {
        this.tint = tint;
        return this;
    }
}