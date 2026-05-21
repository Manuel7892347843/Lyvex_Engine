package core.component.ui.uiElements;

import core.component.ui.UIElement;
import core.component.ui.color.UIColor;

public class UIPanel extends UIElement {
    public UIColor color = new UIColor(0.15f, 0.15f, 0.18f, 1.0f);

    public UIColor getColor() {
        return color;
    }

    public UIPanel setColor(UIColor color) {
        this.color = color;
        return this;
    }
}