package core.physics;

import core.ProjectSettings;
import core.component.RigidBody2D;
import core.gameobject.GameObject;
import core.lib.math.vector2f;

import java.util.ArrayList;
import java.util.List;

public class PhysicsWorld2D {

    private static PhysicsWorld2D instance;

    private List<Collider2D> colliders = new ArrayList<>();
    private List<Collision> collisionsThisFrame = new ArrayList<>();

    private vector2f gravity = new vector2f(0, -9.81f);
    private int velocityIterations = 6;
    private int positionIterations = 2;

    public static boolean DEBUG_COLLISIONS = false;

    private PhysicsWorld2D() {}

    public static PhysicsWorld2D getInstance() {
        if (instance == null) {
            instance = new PhysicsWorld2D();
        }
        return instance;
    }

    public void registerCollider(Collider2D collider) {
        if (!colliders.contains(collider)) {
            colliders.add(collider);
        }
    }

    public void unregisterCollider(Collider2D collider) {
        colliders.remove(collider);
    }

    public void setGravity(vector2f gravity) {
        this.gravity = gravity;
    }

    public vector2f getGravity() {
        return gravity;
    }

    public void step(float deltaTime) {
        collisionsThisFrame.clear();

        for (Collider2D collider : colliders) {
            collider.forceUpdateBounds();
        }

        List<Collision> detectedCollisions = new ArrayList<>();

        for (int i = 0; i < colliders.size(); i++) {
            for (int j = i + 1; j < colliders.size(); j++) {
                Collider2D a = colliders.get(i);
                Collider2D b = colliders.get(j);

                if (!canCollide(a, b)) continue;

                Collision collision = testCollision(a, b);
                if (collision != null) {
                    detectedCollisions.add(collision);
                    if (DEBUG_COLLISIONS) {
                        System.out.println("[PHYSICS] Collision detected: " + collision);
                    }
                }
            }
        }

        for (Collision collision : detectedCollisions) {
            if (!collision.isTrigger) {
                resolveCollision(collision);
            }
        }

        for (Collider2D collider : colliders) {
            collider.forceUpdateBounds();
        }

        for (Collision collision : detectedCollisions) {
            GameObject goA = collision.gameObjectA;
            GameObject goB = collision.gameObjectB;

            goA.getComponents().forEach(c -> c.onCollision(collision));
            goB.getComponents().forEach(c -> c.onCollision(collision));
        }

        collisionsThisFrame.addAll(detectedCollisions);
    }

    private boolean canCollide(Collider2D a, Collider2D b) {
        if (a == b) return false;
        if (a.getGameObject() == b.getGameObject()) return false;

        boolean canCollide = (a.getCollisionLayer() & b.getCollisionMask()) != 0 &&
                (b.getCollisionLayer() & a.getCollisionMask()) != 0;

        if (DEBUG_COLLISIONS && !canCollide) {
            PhysicsLayerManager manager = ProjectSettings.getPhysicsLayerManager();
            String layerA = getLayerName(manager, a.getCollisionLayer());
            String layerB = getLayerName(manager, b.getCollisionLayer());

            System.out.println("[PHYSICS] Layer mismatch: " + a.getGameObject().getName() +
                    " (" + layerA + ") vs " + b.getGameObject().getName() + " (" + layerB + ")");
        }

        return canCollide;
    }

    private String getLayerName(PhysicsLayerManager manager, int layerMask) {
        for (int i = 0; i < manager.getLayerCount(); i++) {
            if ((1 << manager.getLayer(i).bit) == layerMask) {
                return manager.getLayer(i).name;
            }
        }
        return "Unknown(" + String.format("0x%04X", layerMask) + ")";
    }

    private Collision testCollision(Collider2D a, Collider2D b) {
        // Broad phase AABB
        if (!a.getWorldBounds().intersects(b.getWorldBounds())) {
            return null;
        }

        // Narrow phase
        if (a instanceof BoxCollider2D && b instanceof BoxCollider2D) {
            return testBoxBox((BoxCollider2D) a, (BoxCollider2D) b);
        }
        else if (a instanceof CircleCollider2D && b instanceof CircleCollider2D) {
            return testCircleCircle((CircleCollider2D) a, (CircleCollider2D) b);
        }
        else if (a instanceof BoxCollider2D && b instanceof CircleCollider2D) {
            return testBoxCircle((BoxCollider2D) a, (CircleCollider2D) b);
        }
        else if (a instanceof CircleCollider2D && b instanceof BoxCollider2D) {
            Collision c = testBoxCircle((BoxCollider2D) b, (CircleCollider2D) a);
            if (c != null) c.normal = c.normal.mul(-1);
            return c;
        }

        return null;
    }

    private Collision testBoxBox(BoxCollider2D a, BoxCollider2D b) {
        vector2f centerA = a.getCenter();
        vector2f centerB = b.getCenter();

        vector2f halfSizeA = a.getSize();
        vector2f halfSizeB = b.getSize();

        float dx = Math.abs(centerB.x - centerA.x);
        float dy = Math.abs(centerB.y - centerA.y);

        float overlapX = (halfSizeA.x + halfSizeB.x) - dx;
        float overlapY = (halfSizeA.y + halfSizeB.y) - dy;

        if (overlapX <= 0 || overlapY <= 0) return null;

        vector2f normal;
        float penetration;

        if (overlapX < overlapY) {
            normal = new vector2f(centerB.x > centerA.x ? 1 : -1, 0);
            penetration = overlapX;
        } else {
            normal = new vector2f(0, centerB.y > centerA.y ? 1 : -1);
            penetration = overlapY;
        }

        return new Collision(a.getGameObject(), b.getGameObject(),
                centerA.add(centerB).mul(0.5f), normal, penetration,
                a.isTrigger() || b.isTrigger());
    }

    private Collision testCircleCircle(CircleCollider2D a, CircleCollider2D b) {
        vector2f centerA = a.getCenter();
        vector2f centerB = b.getCenter();
        float rA = a.getWorldRadius();
        float rB = b.getWorldRadius();

        vector2f diff = centerB.sub(centerA);
        float distSq = diff.x * diff.x + diff.y * diff.y;
        float radiusSum = rA + rB;

        if (distSq > radiusSum * radiusSum) return null;

        float dist = (float) Math.sqrt(distSq);
        vector2f normal = dist > 0 ? diff.mul(1.0f / dist) : new vector2f(1, 0);
        float penetration = radiusSum - dist;

        return new Collision(a.getGameObject(), b.getGameObject(),
                centerA.add(normal.mul(rA)), normal, penetration,
                a.isTrigger() || b.isTrigger());
    }

    private Collision testBoxCircle(BoxCollider2D box, CircleCollider2D circle) {
        vector2f boxCenter = box.getCenter();
        vector2f boxHalfSize = box.getSize();

        vector2f circleCenter = circle.getCenter();
        float radius = circle.getWorldRadius();

        float closestX = Math.max(boxCenter.x - boxHalfSize.x,
                Math.min(circleCenter.x, boxCenter.x + boxHalfSize.x));
        float closestY = Math.max(boxCenter.y - boxHalfSize.y,
                Math.min(circleCenter.y, boxCenter.y + boxHalfSize.y));

        vector2f closest = new vector2f(closestX, closestY);
        vector2f diff = circleCenter.sub(closest);
        float distSq = diff.x * diff.x + diff.y * diff.y;

        if (distSq > radius * radius) return null;

        float dist = (float) Math.sqrt(distSq);
        vector2f normal = dist > 0 ? diff.mul(1.0f / dist) : new vector2f(1, 0);
        float penetration = radius - dist;

        return new Collision(box.getGameObject(), circle.getGameObject(),
                closest, normal, penetration,
                box.isTrigger() || circle.isTrigger());
    }

    private void resolveCollision(Collision collision) {
        GameObject goA = collision.gameObjectA;
        GameObject goB = collision.gameObjectB;

        RigidBody2D rbA = goA.getComponent(RigidBody2D.class);
        RigidBody2D rbB = goB.getComponent(RigidBody2D.class);

        float invMassA = getInvMass(rbA);
        float invMassB = getInvMass(rbB);

        float totalInvMass = invMassA + invMassB;
        if (totalInvMass <= 0) {
            if (DEBUG_COLLISIONS) {
                System.out.println("[PHYSICS] Skip resolve: both objects immovable");
            }
            return;
        }

        float penetrationSlop = 0.005f;
        float effectivePenetration = Math.max(0.0f, collision.penetrationDepth - penetrationSlop);

        if (effectivePenetration <= 0) {
            return;
        }

        float restOffset = 0.01f;
        effectivePenetration += restOffset;

        vector2f separation = collision.normal.mul(effectivePenetration / totalInvMass);

        if (DEBUG_COLLISIONS) {
            System.out.println("[PHYSICS] Resolving: " + goA.getName() + " vs " + goB.getName() +
                    " pen=" + effectivePenetration + " normal=" + collision.normal);
        }

        if (invMassA > 0) {
            vector2f moveA = separation.mul(-invMassA);
            goA.getTransform().translate(moveA);
        }
        if (invMassB > 0) {
            vector2f moveB = separation.mul(invMassB);
            goB.getTransform().translate(moveB);
        }

        if (rbA != null && !rbA.isStatic() && !rbA.isKinematic()) {
            vector2f velA = rbA.getVelocity();
            float velAlongNormal = velA.dot(collision.normal);
            if (velAlongNormal < 0) {
                vector2f cancelVel = collision.normal.mul(velAlongNormal);
                rbA.setVelocity(velA.sub(cancelVel));
            }
        }

        if (rbB != null && !rbB.isStatic() && !rbB.isKinematic()) {
            vector2f velB = rbB.getVelocity();
            float velAlongNormal = velB.dot(collision.normal);
            if (velAlongNormal < 0) {
                vector2f cancelVel = collision.normal.mul(velAlongNormal);
                rbB.setVelocity(velB.sub(cancelVel));
            }
        }


        final float GROUND_NORMAL_THRESHOLD = 0.7f;

        if (rbA != null && collision.normal.y < -GROUND_NORMAL_THRESHOLD) {
            rbA.isGrounded = true;
        }

        if (rbB != null && collision.normal.y > GROUND_NORMAL_THRESHOLD) {
            rbB.isGrounded = true;
        }
    }

    private float getInvMass(RigidBody2D rb) {
        if (rb == null) return 0;
        if (rb.isStatic()) return 0;
        return rb.getInverseMass();
    }

    public List<Collision> getCollisionsThisFrame() {
        return new ArrayList<>(collisionsThisFrame);
    }

    public void clearColliders(){
        colliders.clear();
        collisionsThisFrame.clear();
    }

    public RaycastHit2D raycast(vector2f origin, vector2f direction, float maxDistance) {
        return null;
    }
}