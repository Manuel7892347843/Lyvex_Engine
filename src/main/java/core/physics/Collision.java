package core.physics;

import core.gameobject.GameObject;
import core.lib.math.vector2f;

public class Collision {

    public GameObject gameObjectA;

    public GameObject gameObjectB;

    public vector2f contactPoint;

    public vector2f normal;

    public float penetrationDepth;

    public boolean isTrigger;

    public Collision(GameObject a, GameObject b, vector2f contactPoint,
                     vector2f normal, float penetrationDepth, boolean isTrigger) {
        this.gameObjectA = a;
        this.gameObjectB = b;
        this.contactPoint = contactPoint;
        this.normal = normal;
        this.penetrationDepth = penetrationDepth;
        this.isTrigger = isTrigger;
    }

    @Override
    public String toString() {
        return String.format("Collision[%s <-> %s, depth=%.3f]",
                gameObjectA.getName(), gameObjectB.getName(), penetrationDepth);
    }
}