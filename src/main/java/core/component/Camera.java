package core.component;

import core.lib.math.matrix4f;
import core.lib.math.vector2D;

public class Camera extends Component {
    public boolean primary = true;
    public int targetDisplay = 1;
    public float orthoSize = 5.0f;
    public float nearPlane = -100.0f;
    public float farPlane = 100.0f;
    public float aspectRatio = 16.0f / 9.0f;

    private transient matrix4f viewMatrix = new matrix4f();
    private transient matrix4f projectionMatrix = new matrix4f();
    private transient matrix4f viewProjectionMatrix = new matrix4f();

    public matrix4f getViewMatrix() {
        if (getGameObject() == null) {
            return new matrix4f().identity();
        }

        vector2D pos = getGameObject().getTransform().getPosition();
        float rotZ = getGameObject().getTransform().getRotation();

        viewMatrix.identity()
                .translate(-pos.x, -pos.y, 0.0f)  // Z = 0 per 2D
                .rotateZ((float) Math.toRadians(-rotZ));

        return viewMatrix;
    }

    public matrix4f getProjectionMatrix(float viewportWidth, float viewportHeight) {
        aspectRatio = viewportWidth / viewportHeight;

        float halfHeight = orthoSize;
        float halfWidth = halfHeight * aspectRatio;

        projectionMatrix.identity().ortho(
                -halfWidth, halfWidth,
                -halfHeight, halfHeight,
                nearPlane, farPlane
        );

        return projectionMatrix;
    }

    public matrix4f getViewProjectionMatrix(float viewportWidth, float viewportHeight) {
        getProjectionMatrix(viewportWidth, viewportHeight);
        getViewMatrix();
        return viewMatrix.mul(projectionMatrix, viewProjectionMatrix);
    }

    public void setOrthoSize(float size) {
        this.orthoSize = size;
    }

    public float getOrthoSize() {
        return orthoSize;
    }

    public void setAspectRatio(float aspect) {
        this.aspectRatio = aspect;
    }

    public int getTargetDisplay() {
        return targetDisplay;
    }

    public void setTargetDisplay(int targetDisplay) {
        this.targetDisplay = Math.max(1, targetDisplay);
    }
}