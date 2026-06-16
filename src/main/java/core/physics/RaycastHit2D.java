package core.physics;

import core.gameobject.GameObject;
import core.lib.math.vector2f;

public class RaycastHit2D {
    public GameObject gameObject;
    public vector2f point;
    public vector2f normal;
    public float distance;
    public Collider2D collider;
}