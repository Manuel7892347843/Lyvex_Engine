package core.component;

import core.Engine;
import core.physics.PhysicsWorld2D;
import core.lib.math.vector2f;

public class RigidBody2D extends Component {
    public float mass = 1.0f;
    private float inverseMass = 1.0f;
    public vector2f velocity = new vector2f(0, 0);
    private vector2f acceleration = new vector2f(0, 0);
    private vector2f forceAccumulator = new vector2f(0, 0);

    public float gravityScale = 1.0f;
    public float linearDamping = 0.01f;
    public boolean useGravity = true;
    public boolean isKinematic = false;
    public boolean isStatic = false;

    // Ground detection — true quando il rigidbody è a terra
    public boolean isGrounded = false;

    // Velocità massima per evitare tunneling
    public float maxVelocity = 20.0f;

    // Velocità minima per considerare un oggetto "a riposo"
    private static final float SLEEP_THRESHOLD = 0.001f;

    public RigidBody2D() {
        updateInverseMass();
    }

    @Override
    public void fixedUpdate() {
        if (isKinematic || isStatic || getGameObject() == null) return;

        Transform t = getGameObject().getTransform();
        if (t == null) return;

        float dt = Engine.FIXED_TIMESTEP;

        // Salva lo stato grounded del frame precedente
        boolean wasGrounded = isGrounded;
        // Reset per il nuovo frame (verrà settato dalle collisioni)
        isGrounded = false;

        // Applica gravity solo se non grounded
        if (useGravity && gravityScale != 0.0f) {
            // Se eri a terra e la velocity Y è zero o verso il basso,
            // non applicare gravity (resta a terra)
            if (!wasGrounded || velocity.y > 0) {
                vector2f worldGravity = PhysicsWorld2D.getInstance().getGravity();
                vector2f gravityForce = worldGravity.mul(gravityScale * mass);
                forceAccumulator = forceAccumulator.add(gravityForce);
            }
        }

        acceleration = forceAccumulator.mul(inverseMass);
        velocity = velocity.add(acceleration.mul(dt));

        // Clamp velocity per evitare tunneling
        float speed = velocity.length();
        if (speed > maxVelocity) {
            velocity = velocity.mul(maxVelocity / speed);
        }

        if (linearDamping > 0) {
            float dampingFactor = Math.max(0.0f, 1.0f - linearDamping * dt);
            velocity = velocity.mul(dampingFactor);
        }

        // Sleep threshold
        if (Math.abs(velocity.x) < SLEEP_THRESHOLD) velocity.x = 0;
        if (Math.abs(velocity.y) < SLEEP_THRESHOLD) velocity.y = 0;

        vector2f deltaPos = velocity.mul(dt);
        t.translate(deltaPos);

        forceAccumulator = new vector2f(0, 0);
    }

    public void addForce(vector2f force) {
        if (isKinematic || isStatic) return;
        forceAccumulator = forceAccumulator.add(force);
    }

    public void addImpulse(vector2f impulse) {
        if (isKinematic || isStatic) return;
        velocity = velocity.add(impulse.mul(inverseMass));
    }

    public void addForceAtPoint(vector2f force, vector2f point) {
        addForce(force);
    }

    public void setVelocity(vector2f v) {
        this.velocity = new vector2f(v);
    }

    public void setVelocity(float x, float y) {
        this.velocity = new vector2f(x, y);
    }

    public void setMass(float mass) {
        this.mass = Math.max(0, mass);
        updateInverseMass();
        this.isKinematic = (mass <= 0);
    }

    private void updateInverseMass() {
        this.inverseMass = (mass > 0) ? 1.0f / mass : 0.0f;
    }

    public float getKineticEnergy() {
        float speedSq = velocity.dot(velocity);
        return 0.5f * mass * speedSq;
    }

    public vector2f getMomentum() {
        return velocity.mul(mass);
    }

    public vector2f getVelocity() {
        return new vector2f(velocity);
    }

    public vector2f getAcceleration() {
        return new vector2f(acceleration);
    }

    public float getMass() {
        return mass;
    }

    public float getInverseMass() {
        return inverseMass;
    }

    public float getGravityScale() {
        return gravityScale;
    }

    public void setGravityScale(float scale) {
        this.gravityScale = scale;
    }

    public float getLinearDamping() {
        return linearDamping;
    }

    public void setLinearDamping(float damping) {
        this.linearDamping = damping;
    }

    public boolean isUsingGravity() {
        return useGravity;
    }

    public void setUseGravity(boolean use) {
        this.useGravity = use;
    }

    public boolean isKinematic() {
        return isKinematic;
    }

    public void setKinematic(boolean kinematic) {
        this.isKinematic = kinematic;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public void setStatic(boolean isStatic) {
        this.isStatic = isStatic;
        if (isStatic) {
            this.velocity = new vector2f(0, 0);
            this.inverseMass = 0;
        }
    }

    public boolean isGrounded() {
        return isGrounded;
    }
}