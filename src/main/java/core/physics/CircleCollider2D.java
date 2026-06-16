package core.physics;

import core.lib.math.vector2f;

public class CircleCollider2D extends Collider2D {

    public float radius = 0.5f;

    public CircleCollider2D() {}

    public CircleCollider2D(float radius) {
        this.radius = radius;
    }

    public float getRadius() {
        return radius;
    }

    public void setRadius(float radius) {
        this.radius = radius;
        this.dirty = true;
    }

    @Override
    protected void updateWorldBounds() {
        vector2f pos = getPosition();
        float r = radius;

        worldBounds.min.x = pos.x + offset.x - r;
        worldBounds.min.y = pos.y + offset.y - r;
        worldBounds.max.x = pos.x + offset.x + r;
        worldBounds.max.y = pos.y + offset.y + r;
    }

    @Override
    public vector2f getCenter() {
        vector2f pos = getPosition();
        return new vector2f(pos.x + offset.x, pos.y + offset.y);
    }

    public float getWorldRadius() {
        return radius;
    }

    private vector2f getPosition() {
        return new vector2f(this.getGameObject().getTransform().getPosition().x,
                this.getGameObject().getTransform().getPosition().y);
    }
}