package core.component;

public class EditorCamera2D {
    private float x = 0.0f;
    private float y = 0.0f;
    private float zoom = 1.0f;
    private float moveSpeed = 5.0f;
    private float zoomSpeed = 0.1f;
    private float orthoSize = 5.0f;

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

    public void setMoveSpeed(float moveSpeed) {
        this.moveSpeed = moveSpeed;
    }

    public float getZoomSpeed() {
        return zoomSpeed;
    }

    public void setZoomSpeed(float zoomSpeed) {
        this.zoomSpeed = zoomSpeed;
    }

    public float getOrthoSize() {
        return orthoSize;
    }

    public void setOrthoSize(float orthoSize) {
        this.orthoSize = orthoSize;
    }
}