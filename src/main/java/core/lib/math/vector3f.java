package core.lib.math;

public class vector3f {
    public float x, y, z;

    public vector3f() {
        this(0.0f, 0.0f, 0.0f);
    }

    public vector3f(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public vector3f set(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    public vector3f add(vector3f other) {
        return new vector3f(this.x + other.x, this.y + other.y, this.z + other.z);
    }

    public vector3f sub(vector3f other) {
        return new vector3f(this.x - other.x, this.y - other.y, this.z - other.z);
    }

    public vector3f mul(float scalar) {
        return new vector3f(this.x * scalar, this.y * scalar, this.z * scalar);
    }

    public vector3f negate() {
        return new vector3f(-this.x, -this.y, -this.z);
    }

    public float length() {
        return (float) Math.sqrt(x * x + y * y + z * z);
    }

    public vector3f normalize() {
        float len = length();
        if (len == 0.0f) return new vector3f(0, 0, 0);
        return new vector3f(x / len, y / len, z / len);
    }

    public float dot(vector3f other) {
        return this.x * other.x + this.y * other.y + this.z * other.z;
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ", " + z + ")";
    }
}