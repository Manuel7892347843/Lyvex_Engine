package core.lib.math;

public class vector2f {
    public float x;
    public float y;

    public vector2f() {
        this.x = 0;
        this.y = 0;
    }

    public vector2f(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public vector2f(vector2f other) {
        this.x = other.x;
        this.y = other.y;
    }

    public vector2f add(vector2f other) {
        return new vector2f(this.x + other.x, this.y + other.y);
    }

    public vector2f sub(vector2f other) {
        return new vector2f(this.x - other.x, this.y - other.y);
    }

    public vector2f mul(float scalar) {
        return new vector2f(this.x * scalar, this.y * scalar);
    }

    public vector2f div(float scalar) {
        return new vector2f(this.x / scalar, this.y / scalar);
    }

    public float length() {
        return (float) Math.sqrt(x * x + y * y);
    }

    public vector2f normalize() {
        float len = length();
        if (len == 0) return new vector2f(0, 0);
        return new vector2f(x / len, y / len);
    }

    public float dot(vector2f other) {
        return this.x * other.x + this.y * other.y;
    }

    public vector2f perpendicular() {
        return new vector2f(-y, x);
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}