package core.component.ui.uiElements;

import core.component.sprite.Sprite;
import core.component.ui.UIElement;
import core.component.ui.color.UIColor;

public class UIImage extends UIElement {
    public Sprite sprite;
    public UIColor tint = UIColor.white();

    public Sprite getSprite() {
        return sprite;
    }

    public UIImage setSprite(Sprite sprite) {
        this.sprite = sprite;
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