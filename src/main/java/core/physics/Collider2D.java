package core.physics;

import core.component.Component;
import core.lib.Bounds2D;
import core.lib.math.vector2f;

public abstract class Collider2D extends Component {

    public vector2f offset = new vector2f(0, 0);

    public boolean isTrigger = false;

    public int collisionLayer = 0x0001; // default layer 0

    public int collisionMask = 0xFFFF; // collide con tutti di default

    protected Bounds2D worldBounds = new Bounds2D();

    protected boolean dirty = true;

    public vector2f getOffset() {
        return offset;
    }

    public void setOffset(vector2f offset) {
        this.offset = offset;
        this.dirty = true;
    }

    public boolean isTrigger() {
        return isTrigger;
    }

    public void setTrigger(boolean trigger) {
        this.isTrigger = trigger;
    }

    public int getCollisionLayer() {
        return collisionLayer;
    }

    public void setCollisionLayer(int layer) {
        this.collisionLayer = layer;
    }

    public int getCollisionMask() {
        return collisionMask;
    }

    public void setCollisionMask(int mask) {
        this.collisionMask = mask;
    }

    public Bounds2D getWorldBounds() {
        if (dirty) {
            updateWorldBounds();
            dirty = false;
        }
        return worldBounds;
    }

    public void forceUpdateBounds() {
        dirty = true;
        getWorldBounds();
    }

    protected abstract void updateWorldBounds();

    public abstract vector2f getCenter();

    @Override
    public void update() {
        dirty = true;
    }

    @Override
    public void onEnable() {
        PhysicsWorld2D.getInstance().registerCollider(this);
    }

    @Override
    public void onDisable() {
        PhysicsWorld2D.getInstance().unregisterCollider(this);
    }
}