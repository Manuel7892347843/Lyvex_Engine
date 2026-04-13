package core.component;

public class EditorCamera2D {
    public float x;
    public float y;
    public float zoom = 1.0f;

    private float moveSpeed = 5.0f;
    private float zoomSpeed = 0.1f;

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getZoom() {
        return zoom;
    }

    public void setZoom(float zoom) {
        this.zoom = zoom;
    }

    public float getMoveSpeed() {
        return moveSpeed;
    }

    public float getZoomSpeed() {
        return zoomSpeed;
    }
}
