package core.physics;

import core.lib.math.vector2f;

public class BoxCollider2D extends Collider2D {

    public vector2f size = new vector2f(0.5f, 0.5f);

    public BoxCollider2D() {}

    public BoxCollider2D(vector2f size) {
        this.size = size;
    }

    public vector2f getSize() {
        return size;
    }

    public void setSize(vector2f size) {
        this.size = size;
        this.dirty = true;
    }

    @Override
    protected void updateWorldBounds() {
        vector2f pos = getPosition();

        float halfW = size.x;
        float halfH = size.y;

        worldBounds.min.x = pos.x + offset.x - halfW;
        worldBounds.min.y = pos.y + offset.y - halfH;
        worldBounds.max.x = pos.x + offset.x + halfW;
        worldBounds.max.y = pos.y + offset.y + halfH;
    }

    @Override
    public vector2f getCenter() {
        vector2f pos = getPosition();
        return new vector2f(pos.x + offset.x, pos.y + offset.y);
    }

    public vector2f[] getVertices() {
        vector2f center = getCenter();
        float halfW = size.x;
        float halfH = size.y;

        return new vector2f[] {
                new vector2f(center.x - halfW, center.y - halfH), // bottom-left
                new vector2f(center.x + halfW, center.y - halfH), // bottom-right
                new vector2f(center.x + halfW, center.y + halfH), // top-right
                new vector2f(center.x - halfW, center.y + halfH)  // top-left
        };
    }

    private vector2f getPosition() {
        return new vector2f(this.getGameObject().getTransform().getPosition().x,
                this.getGameObject().getTransform().getPosition().y);
    }
}