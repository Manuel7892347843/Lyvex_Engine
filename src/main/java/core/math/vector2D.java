package core.math;

public class vector2D {
    public float x;
    public float y;

    public vector2D(float x, float y){
        this.x = x;
        this.y = y;
    }

    public vector2D(vector2D v){
        this.x = v.x;
        this.y = v.y;
    }

    public vector2D add(vector2D v2){
        return new vector2D(x + v2.x, y + v2.y);
    }

    public vector2D sub(vector2D v2){
        return new vector2D(x - v2.x, y - v2.y);
    }

    public vector2D multiply(float a){
        return new vector2D(x * a, y * a);
    }

    public vector2D scale(vector2D v2){
        return new vector2D(x * v2.x, y * v2.y);
    }

    public float dot(vector2D v2){
        return x * v2.x + y * v2.y;
    }

    public vector2D perpendicular() {
        return new vector2D(-y, x);
    }

    public float magnitudeSq() {
        return x * x + y * y;
    }

    public float magnitude() {
        return (float) Math.sqrt(magnitudeSq());
    }

    public vector2D normalize() {
        float len = magnitude();
        if (len == 0) return new vector2D(0, 0);
        return multiply(1.0f / len);
    }

    public float distance(vector2D v) {
        return this.sub(v).magnitude();
    }

    @Override
    public String toString(){
        return "(" + x + ", " + y + ")";
    }
}
