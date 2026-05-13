package core.component;

import core.math.vector2D;

public class Transform extends Component{
    private vector2D position = new vector2D(0, 0);
    private float rotation = 0.0f;
    private vector2D scale = new vector2D(1.0f, 1.0f);

    public Transform(){}
    public Transform(float x, float y){
        this.position = new vector2D(x, y);
    }

    public void translate(vector2D offset){
        this.position = position.add(offset);
    }

    public void setPosition(vector2D pos){
        this.position = new vector2D(pos);
    }

    public void setPosition(float x, float y) {
        this.position = new vector2D(x, y);
    }

    public vector2D getPosition() {
        return new vector2D(position);
    }

    public float getX() {
        return position.x;
    }

    public float getY() {
        return position.y;
    }

    public float getRotation() {
        return rotation;
    }
    public void setRotation(float rotation) {
        this.rotation = rotation;
    }

    public vector2D getScale() {
        return new vector2D(scale);
    }
    public void setScale(vector2D scale) {
        this.scale = new vector2D(scale);
    }
    public void setScale(float x, float y) {
        this.scale = new vector2D(x, y);
    }

    public vector2D getForward() {
        float rad = (float) Math.toRadians(rotation);
        return new vector2D(
                (float) Math.cos(rad),
                (float) Math.sin(rad)
        );
    }

    public vector2D getRight() {
        return getForward().perpendicular();
    }
}
