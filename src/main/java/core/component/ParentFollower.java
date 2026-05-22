package core.component;

import core.gameobject.GameObject;
import core.lib.math.vector2D;

public class ParentFollower extends Component{
    public vector2D offset = new vector2D(2, 2);
    public boolean inheritRotation = true;
    public boolean inheritScale = false;
    public boolean enabled = true;
    public ParentFollower() {}

    @Override
    public void update() {
        if (!enabled) return;

        GameObject parent = getGameObject().getParent();
        if (parent == null) return;

        Transform parentTransform = parent.getTransform();
        Transform myTransform = getGameObject().getTransform();

        vector2D parentPos = parentTransform.getPosition();
        float parentRot = parentTransform.getRotation();

        float rotOffsetX = offset.x - offset.y;
        float rotOffsetY = offset.x+ offset.y;

        myTransform.setPosition(parentPos.x + rotOffsetX, parentPos.y + rotOffsetY);

        if (inheritRotation) {
            myTransform.setRotation(parentRot);
        }

        if (inheritScale) {
            vector2D parentScale = parentTransform.getScale();
            myTransform.setScale(parentScale.x, parentScale.y);
        }
    }

    public vector2D getOffset() { return new vector2D(offset); }
    public void setOffset(vector2D offset) { this.offset = new vector2D(offset); }
    public void setOffset(float x, float y) { this.offset = new vector2D(x, y); }

    public boolean isInheritRotation() { return inheritRotation; }
    public void setInheritRotation(boolean inheritRotation) { this.inheritRotation = inheritRotation; }

    public boolean isInheritScale() { return inheritScale; }
    public void setInheritScale(boolean inheritScale) { this.inheritScale = inheritScale; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
