package core.component;

import core.math.matrix4f;
import core.math.vector2D;

public class Camera extends Component {
    public boolean primary = true;
    private float orthoSize = 5.0f;
    private float nearPlane = -100.0f;
    private float farPlane = 100.0f;
    private float aspectRatio = 16.0f / 9.0f;

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
        return projectionMatrix.mul(viewMatrix, viewProjectionMatrix);
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
}