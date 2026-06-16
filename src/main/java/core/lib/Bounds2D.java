package core.lib;

import core.lib.math.vector2f;

public class Bounds2D {

    public vector2f min = new vector2f(Float.MAX_VALUE, Float.MAX_VALUE);
    public vector2f max = new vector2f(-Float.MAX_VALUE, -Float.MAX_VALUE);

    public Bounds2D() {}

    public Bounds2D(vector2f min, vector2f max) {
        this.min = min;
        this.max = max;
    }

    public vector2f getCenter() {
        return new vector2f((min.x + max.x) * 0.5f, (min.y + max.y) * 0.5f);
    }

    public vector2f getSize() {
        return new vector2f(max.x - min.x, max.y - min.y);
    }

    public boolean contains(vector2f point) {
        return point.x >= min.x && point.x <= max.x &&
                point.y >= min.y && point.y <= max.y;
    }

    public boolean intersects(Bounds2D other) {
        return this.min.x <= other.max.x && this.max.x >= other.min.x &&
                this.min.y <= other.max.y && this.max.y >= other.min.y;
    }

    public void expand(vector2f point) {
        if (point.x < min.x) min.x = point.x;
        if (point.y < min.y) min.y = point.y;
        if (point.x > max.x) max.x = point.x;
        if (point.y > max.y) max.y = point.y;
    }
}