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

    // --- METODI ESISTENTI ---
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

    // --- METODI AGGIUNTI (necessari per il sistema fisica) ---

    /** Moltiplicazione componente per componente */
    public vector2f mul(vector2f other) {
        return new vector2f(this.x * other.x, this.y * other.y);
    }

    /** Negazione */
    public vector2f neg() {
        return new vector2f(-x, -y);
    }

    /** Modifica questo vettore in-place (utile per performance) */
    public vector2f addInPlace(vector2f other) {
        this.x += other.x;
        this.y += other.y;
        return this;
    }

    public vector2f subInPlace(vector2f other) {
        this.x -= other.x;
        this.y -= other.y;
        return this;
    }

    public vector2f mulInPlace(float scalar) {
        this.x *= scalar;
        this.y *= scalar;
        return this;
    }

    /** Distanza da un altro punto */
    public float distance(vector2f other) {
        float dx = this.x - other.x;
        float dy = this.y - other.y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    /** Distanza al quadrato (più veloce, no sqrt) */
    public float distanceSq(vector2f other) {
        float dx = this.x - other.x;
        float dy = this.y - other.y;
        return dx * dx + dy * dy;
    }

    /** Riflessione rispetto a una normale */
    public vector2f reflect(vector2f normal) {
        float dot = this.dot(normal);
        return this.sub(normal.mul(2 * dot));
    }

    /** Proiezione su un altro vettore */
    public vector2f project(vector2f onto) {
        float dot = this.dot(onto);
        float ontoLenSq = onto.dot(onto);
        if (ontoLenSq == 0) return new vector2f(0, 0);
        return onto.mul(dot / ontoLenSq);
    }

    /** Lerp lineare */
    public static vector2f lerp(vector2f a, vector2f b, float t) {
        return a.add(b.sub(a).mul(t));
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        vector2f other = (vector2f) obj;
        return Float.compare(other.x, x) == 0 && Float.compare(other.y, y) == 0;
    }
}