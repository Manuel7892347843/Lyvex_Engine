package core.component.ui;

import core.component.Component;

public class UIElement extends Component {
    public float x = 960.0f;
    public float y = 540.0f;
    public float width = 100.0f;
    public float height = 100.0f;
    public float pivotX = 0.5f;
    public float pivotY = 0.5f;
    public boolean visible = true;
    public int order = 0;

    public float getX() {
        return x;
    }

    public UIElement setX(float x) {
        this.x = x;
        return this;
    }

    public float getY() {
        return y;
    }

    public UIElement setY(float y) {
        this.y = y;
        return this;
    }

    public float getWidth() {
        return width;
    }

    public UIElement setWidth(float width) {
        this.width = width;
        return this;
    }

    public float getHeight() {
        return height;
    }

    public UIElement setHeight(float height) {
        this.height = height;
        return this;
    }

    public float getPivotX() {
        return pivotX;
    }

    public UIElement setPivotX(float pivotX) {
        this.pivotX = pivotX;
        return this;
    }

    public float getPivotY() {
        return pivotY;
    }

    public UIElement setPivotY(float pivotY) {
        this.pivotY = pivotY;
        return this;
    }

    public boolean isVisible() {
        return visible;
    }

    public UIElement setVisible(boolean visible) {
        this.visible = visible;
        return this;
    }

    public int getOrder() {
        return order;
    }

    public UIElement setOrder(int order) {
        this.order = order;
        return this;
    }

    public boolean contains(float screenX, float screenY) {
        float left = x - width * pivotX;
        float right = left + width;
        float bottom = y - height * pivotY;
        float top = bottom + height;

        return screenX >= left && screenX <= right && screenY >= bottom && screenY <= top;
    }
}