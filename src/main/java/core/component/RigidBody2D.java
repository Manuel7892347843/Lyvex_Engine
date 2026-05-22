package core.component;

import core.Engine;
import core.lib.math.vector2D;

public class RigidBody2D extends Component{
    public float mass = 1.0f;
    private float inverseMass = 1.0f;
    public vector2D velocity = new vector2D(0, 0);
    private vector2D acceleration = new vector2D(0, 0);
    private vector2D forceAccumulator = new vector2D(0, 0);

    public float gravityScale = 1.0f;
    public static final vector2D GRAVITY = new vector2D(0, -980f);
    public float linearDamping = 0.01f;
    public boolean useGravity = true;
    public boolean isKinematic = false;

    public RigidBody2D(){
        updateInverseMass();
    }

    @Override
    public void update() {
        if (isKinematic || getGameObject() == null) return;

        Transform t = getGameObject().getTransform();
        if (t == null) return;

        if (useGravity) {
            vector2D gravityForce = GRAVITY.multiply(gravityScale * mass);
            forceAccumulator = forceAccumulator.add(gravityForce);
        }

        acceleration = forceAccumulator.multiply(inverseMass);

        velocity = velocity.add(acceleration.multiply(Engine.getDeltaTime()));

        if (linearDamping > 0) {
            float dampingFactor = Math.max(0.0f, 1.0f - linearDamping * Engine.getDeltaTime());
            velocity = velocity.multiply(dampingFactor);
        }

        vector2D deltaPos = velocity.multiply(Engine.getDeltaTime());
        t.translate(deltaPos);

        forceAccumulator = new vector2D(0, 0);
    }

    public void addForce(vector2D force){
        if(!isKinematic) return;
        forceAccumulator = forceAccumulator.add(force);
    }

    public void addImpulse(vector2D impulse){
        if(!isKinematic) return;
        velocity = velocity.add(impulse.multiply(inverseMass));
    }

    public void addForceAtPoint(vector2D force, vector2D point){
        addForce(force);
        // todo: calcolo rotazione, calcolo torque = r x F
    }

    public void setVelocity(vector2D v) {
        this.velocity = new vector2D(v);
    }

    public void setVelocity(float x, float y) {
        this.velocity = new vector2D(x, y);
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

    public vector2D getMomentum() {
        return velocity.multiply(mass);
    }

    public vector2D getVelocity() {
        return new vector2D(velocity);
    }
    public vector2D getAcceleration() {
        return new vector2D(acceleration);
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
}